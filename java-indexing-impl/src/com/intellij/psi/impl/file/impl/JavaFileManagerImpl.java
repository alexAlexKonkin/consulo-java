/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.java.util.JavaClassNames;
import com.intellij.ProjectTopics;
import com.intellij.ide.highlighter.JavaClassFileType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootAdapter;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWithId;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.psi.PsiNameHelper;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.java.stubs.index.JavaFullClassNameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.containers.ConcurrentHashMap;
import com.intellij.util.containers.ContainerUtil;

/**
 * Author: dmitrylomov
 */
@org.consulo.lombok.annotations.Logger
public class JavaFileManagerImpl implements JavaFileManager, Disposable
{
	private final ConcurrentHashMap<GlobalSearchScope, PsiClass> myCachedObjectClassMap = new ConcurrentHashMap<GlobalSearchScope, PsiClass>();
	private final PsiManagerEx myManager;
	private final ProjectRootManager myProjectRootManager;
	private Set<String> myNontrivialPackagePrefixes = null;
	private boolean myDisposed = false;

	public JavaFileManagerImpl(final Project project, final PsiManagerEx manager, final ProjectRootManager projectRootManager)
	{
		myManager = manager;
		myProjectRootManager = projectRootManager;
		project.getMessageBus().connect().subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootAdapter()
		{
			@Override
			public void rootsChanged(final ModuleRootEvent event)
			{
				myNontrivialPackagePrefixes = null;
			}
		});

		myManager.registerRunnableToRunOnChange(new Runnable()
		{
			@Override
			public void run()
			{
				myCachedObjectClassMap.clear();
			}
		});
	}

	@Override
	public void dispose()
	{
		myDisposed = true;
		myCachedObjectClassMap.clear();
	}

	@Override
	public PsiClass[] findClasses(@NotNull String qName, @NotNull final GlobalSearchScope scope)
	{
		final Collection<PsiClass> classes = JavaFullClassNameIndex.getInstance().get(qName.hashCode(), myManager.getProject(), scope);
		if(classes.isEmpty())
		{
			return PsiClass.EMPTY_ARRAY;
		}
		List<PsiClass> result = new ArrayList<PsiClass>(classes.size());
		int count = 0;
		PsiClass aClass = null;
		for(PsiClass found : classes)
		{
			aClass = found;
			final String qualifiedName = aClass.getQualifiedName();
			if(qualifiedName == null || !qualifiedName.equals(qName))
			{
				continue;
			}

			VirtualFile vFile = aClass.getContainingFile().getVirtualFile();
			if(!hasAcceptablePackage(vFile))
			{
				continue;
			}

			result.add(aClass);
			count++;
		}

		if(count == 0)
		{
			return PsiClass.EMPTY_ARRAY;
		}
		if(count == 1)
		{
			return new PsiClass[]{aClass};
		}

		ContainerUtil.quickSort(result, new Comparator<PsiClass>()
		{
			@Override
			public int compare(PsiClass o1, PsiClass o2)
			{
				return scope.compare(o2.getContainingFile().getVirtualFile(), o1.getContainingFile().getVirtualFile());
			}
		});

		return result.toArray(new PsiClass[count]);
	}

	@Override
	@Nullable
	public PsiClass findClass(@NotNull String qName, @NotNull GlobalSearchScope scope)
	{
		LOGGER.assertTrue(!myDisposed);

		if(JavaClassNames.JAVA_LANG_OBJECT.equals(qName))
		{ // optimization
			PsiClass cached = myCachedObjectClassMap.get(scope);
			if(cached == null)
			{
				cached = findClassInIndex(qName, scope);
				if(cached != null)
				{
					cached = myCachedObjectClassMap.cacheOrGet(scope, cached);
				}
			}

			return cached;
		}

		return findClassInIndex(qName, scope);
	}


	@Nullable
	private PsiClass findClassInIndex(String qName, GlobalSearchScope scope)
	{
		VirtualFile bestFile = null;
		PsiClass bestClass = null;
		final Collection<PsiClass> classes = JavaFullClassNameIndex.getInstance().get(qName.hashCode(), myManager.getProject(), scope);

		for(PsiClass aClass : classes)
		{
			PsiFile file = aClass.getContainingFile();
			if(file == null)
			{
				LOGGER.error("aClass=" + aClass + " of class " + aClass.getClass() + "; valid=" + aClass.isValid());
				continue;
			}
			final boolean valid = aClass.isValid();
			VirtualFile vFile = file.getVirtualFile();
			if(!valid)
			{
				LOGGER.error("Invalid class " + aClass + "; " +
						file + (file.isValid() ? "" : " (invalid)") +
						"; virtualFile:" + vFile +
						(vFile != null && !vFile.isValid() ? " (invalid)" : "") +
						"; id=" + (vFile == null ? 0 : ((VirtualFileWithId) vFile).getId()), new PsiInvalidElementAccessException(aClass));
				continue;
			}

			final String qualifiedName = aClass.getQualifiedName();
			if(qualifiedName == null || !qualifiedName.equals(qName))
			{
				continue;
			}


			if(!hasAcceptablePackage(vFile))
			{
				continue;
			}
			if(bestFile == null || scope.compare(vFile, bestFile) > 0)
			{
				bestFile = vFile;
				bestClass = aClass;
			}
		}
		return bestClass;
	}

	private boolean hasAcceptablePackage(final VirtualFile vFile)
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

	@Override
	public Collection<String> getNonTrivialPackagePrefixes()
	{
		if(myNontrivialPackagePrefixes == null)
		{
			Set<String> names = new HashSet<String>();
			final ProjectRootManager rootManager = myProjectRootManager;
			final VirtualFile[] sourceRoots = rootManager.getContentSourceRoots();
			final ProjectFileIndex fileIndex = rootManager.getFileIndex();
			for(final VirtualFile sourceRoot : sourceRoots)
			{
				final String packageName = fileIndex.getPackageNameByDirectory(sourceRoot);
				if(packageName != null && !packageName.isEmpty())
				{
					names.add(packageName);
				}
			}
			myNontrivialPackagePrefixes = names;
		}
		return myNontrivialPackagePrefixes;
	}
}
