/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.psi.impl.file.impl;

import com.intellij.ProjectTopics;
import com.intellij.ide.highlighter.JavaClassFileType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.impl.scopes.ModuleWithDependenciesScope;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWithId;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiImplUtil;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.java.stubs.index.JavaAutoModuleNameIndex;
import com.intellij.psi.impl.java.stubs.index.JavaFullClassNameIndex;
import com.intellij.psi.impl.java.stubs.index.JavaModuleNameIndex;
import com.intellij.psi.impl.java.stubs.index.JavaSourceModuleNameIndex;
import com.intellij.psi.impl.light.LightJavaModule;
import com.intellij.psi.search.DelegatingGlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.containers.ContainerUtil;
import consulo.disposer.Disposable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Stream;

/**
 * @author dmitry lomov
 */
@Singleton
public class JavaFileManagerImpl implements JavaFileManager, Disposable
{
	private static final Logger LOG = Logger.getInstance("#com.intellij.psi.impl.file.impl.JavaFileManagerImpl");

	private final PsiManagerEx myManager;
	private volatile Set<String> myNontrivialPackagePrefixes;
	private boolean myDisposed;

	@Inject
	public JavaFileManagerImpl(Project project)
	{
		myManager = PsiManagerEx.getInstanceEx(project);
		project.getMessageBus().connect().subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootAdapter()
		{
			@Override
			public void rootsChanged(final ModuleRootEvent event)
			{
				myNontrivialPackagePrefixes = null;
			}
		});
	}

	@Override
	public void dispose()
	{
		myDisposed = true;
	}

	@Nonnull
	@Override
	public PsiClass[] findClasses(@Nonnull String qName, @Nonnull final GlobalSearchScope scope)
	{
		List<Pair<PsiClass, VirtualFile>> result = doFindClasses(qName, scope);

		int count = result.size();
		if(count == 0)
		{
			return PsiClass.EMPTY_ARRAY;
		}
		if(count == 1)
		{
			return new PsiClass[]{result.get(0).getFirst()};
		}

		ContainerUtil.quickSort(result, (o1, o2) -> scope.compare(o2.getSecond(), o1.getSecond()));

		return result.stream().map(p -> p.getFirst()).toArray(PsiClass[]::new);
	}

	@Nonnull
	private List<Pair<PsiClass, VirtualFile>> doFindClasses(@Nonnull String qName, @Nonnull final GlobalSearchScope scope)
	{
		final Collection<PsiClass> classes = JavaFullClassNameIndex.getInstance().get(qName.hashCode(), myManager.getProject(), scope);
		if(classes.isEmpty())
		{
			return Collections.emptyList();
		}
		List<Pair<PsiClass, VirtualFile>> result = new ArrayList<>(classes.size());
		for(PsiClass aClass : classes)
		{
			final String qualifiedName = aClass.getQualifiedName();
			if(qualifiedName == null || !qualifiedName.equals(qName))
			{
				continue;
			}

			PsiFile file = aClass.getContainingFile();
			if(file == null)
			{
				throw new AssertionError("No file for class: " + aClass + " of " + aClass.getClass());
			}
			final boolean valid = file.isValid();
			VirtualFile vFile = file.getVirtualFile();
			if(!valid)
			{
				LOG.error("Invalid file " +
						file + "; virtualFile:" + vFile +
						(vFile != null && !vFile.isValid() ? " (invalid)" : "") +
						"; id=" + (vFile == null ? 0 : ((VirtualFileWithId) vFile).getId()), new PsiInvalidElementAccessException(aClass));
				continue;
			}
			if(!hasAcceptablePackage(vFile))
			{
				continue;
			}

			result.add(Pair.create(aClass, vFile));
		}

		return result;
	}

	@Override
	@Nullable
	public PsiClass findClass(@Nonnull String qName, @Nonnull GlobalSearchScope scope)
	{
		LOG.assertTrue(!myDisposed);
		VirtualFile bestFile = null;
		PsiClass bestClass = null;
		List<Pair<PsiClass, VirtualFile>> result = doFindClasses(qName, scope);

		//noinspection ForLoopReplaceableByForEach
		for(int i = 0; i < result.size(); i++)
		{
			Pair<PsiClass, VirtualFile> pair = result.get(i);
			VirtualFile vFile = pair.getSecond();
			if(bestFile == null || scope.compare(vFile, bestFile) > 0)
			{
				bestFile = vFile;
				bestClass = pair.getFirst();
			}
		}

		return bestClass;
	}

	private boolean hasAcceptablePackage(@Nonnull VirtualFile vFile)
	{
		if(vFile.getFileType() == JavaClassFileType.INSTANCE)
		{
			// See IDEADEV-5626
			final VirtualFile root = ProjectRootManager.getInstance(myManager.getProject()).getFileIndex().getClassRootForFile(vFile);
			VirtualFile parent = vFile.getParent();
			final PsiNameHelper nameHelper = PsiNameHelper.getInstance(myManager.getProject());
			while(parent != null && !Comparing.equal(parent, root))
			{
				if(!nameHelper.isIdentifier(parent.getName()))
				{
					return false;
				}
				parent = parent.getParent();
			}
		}

		return true;
	}

	@Nonnull
	@Override
	public Collection<String> getNonTrivialPackagePrefixes()
	{
		Set<String> names = myNontrivialPackagePrefixes;
		if(names == null)
		{
			names = new HashSet<>();
			final ProjectRootManager rootManager = ProjectRootManager.getInstance(myManager.getProject());
			final VirtualFile[] sourceRoots = rootManager.getContentSourceRoots();
			final ProjectFileIndex fileIndex = rootManager.getFileIndex();
			for(final VirtualFile sourceRoot : sourceRoots)
			{
				if(sourceRoot.isDirectory())
				{
					final String packageName = fileIndex.getPackageNameByDirectory(sourceRoot);
					if(packageName != null && !packageName.isEmpty())
					{
						names.add(packageName);
					}
				}
			}
			myNontrivialPackagePrefixes = names;
		}
		return names;
	}

	@Nonnull
	@Override
	public Collection<PsiJavaModule> findModules(@Nonnull String moduleName, @Nonnull GlobalSearchScope scope)
	{
		GlobalSearchScope excludingScope = new LibSrcExcludingScope(scope);

		List<PsiJavaModule> results = new ArrayList<>(JavaModuleNameIndex.getInstance().get(moduleName, myManager.getProject(), excludingScope));

		for(VirtualFile manifest : JavaSourceModuleNameIndex.getFilesByKey(moduleName, excludingScope))
		{
			ContainerUtil.addIfNotNull(results, LightJavaModule.findModule(myManager, manifest.getParent().getParent()));
		}

		for(VirtualFile root : JavaAutoModuleNameIndex.getFilesByKey(moduleName, excludingScope))
		{
			ContainerUtil.addIfNotNull(results, LightJavaModule.findModule(myManager, root));
		}

		return upgradeModules(sortModules(results, scope), moduleName, scope);
	}

	private static Collection<PsiJavaModule> sortModules(Collection<PsiJavaModule> modules, GlobalSearchScope scope)
	{
		if(modules.size() > 1)
		{
			List<PsiJavaModule> list = new ArrayList<>(modules);
			list.sort((m1, m2) -> scope.compare(PsiImplUtil.getModuleVirtualFile(m2), PsiImplUtil.getModuleVirtualFile(m1)));
			modules = list;
		}
		return modules;
	}

	private static Collection<PsiJavaModule> upgradeModules(Collection<PsiJavaModule> modules, String moduleName, GlobalSearchScope scope)
	{
		if(modules.size() > 1 && PsiJavaModule.UPGRADEABLE.contains(moduleName) && scope instanceof ModuleWithDependenciesScope)
		{
			Module module = ((ModuleWithDependenciesScope) scope).getModule();
			boolean isModular = Stream.of(ModuleRootManager.getInstance(module).getSourceRoots(true))
					.filter(scope::contains)
					.anyMatch(root -> root.findChild(PsiJavaModule.MODULE_INFO_FILE) != null);
			if(isModular)
			{
				List<PsiJavaModule> list = new ArrayList<>(modules);

				ModuleFileIndex index = ModuleRootManager.getInstance(module).getFileIndex();
				for(ListIterator<PsiJavaModule> i = list.listIterator(); i.hasNext(); )
				{
					PsiJavaModule candidate = i.next();
					if(index.getOrderEntryForFile(PsiImplUtil.getModuleVirtualFile(candidate)) instanceof ModuleExtensionWithSdkOrderEntry)
					{
						if(i.previousIndex() > 0)
						{
							i.remove();  // not at the top -> is upgraded
						}
						else
						{
							list = Collections.singletonList(candidate);  // shadows subsequent modules
							break;
						}
					}
				}

				if(list.size() != modules.size())
				{
					modules = list;
				}
			}
		}

		return modules;
	}

	private static class LibSrcExcludingScope extends DelegatingGlobalSearchScope
	{
		private final ProjectFileIndex myIndex;

		LibSrcExcludingScope(@Nonnull GlobalSearchScope baseScope)
		{
			super(baseScope);
			myIndex = ProjectFileIndex.getInstance(Objects.requireNonNull(baseScope.getProject()));
		}

		@Override
		public boolean contains(@Nonnull VirtualFile file)
		{
			return super.contains(file) && (!myIndex.isInLibrarySource(file) || myIndex.isInLibraryClasses(file));
		}
	}
}