/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.testIntegration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.lang.Language;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ExternalLibraryDescriptor;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JVMElementFactory;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.IncorrectOperationException;

public abstract class JavaTestFramework implements TestFramework
{
	@Override
	public boolean isLibraryAttached(@NotNull Module module)
	{
		GlobalSearchScope scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module);
		PsiClass c = JavaPsiFacade.getInstance(module.getProject()).findClass(getMarkerClassFQName(), scope);
		return c != null;
	}

	@Nullable
	@Override
	public String getLibraryPath()
	{
		ExternalLibraryDescriptor descriptor = getFrameworkLibraryDescriptor();
		if(descriptor != null)
		{
			return descriptor.getLibraryClassesRoots().get(0);
		}
		return null;
	}

	public ExternalLibraryDescriptor getFrameworkLibraryDescriptor()
	{
		return null;
	}

	protected abstract String getMarkerClassFQName();

	@Override
	public boolean isTestClass(@NotNull PsiElement clazz)
	{
		return clazz instanceof PsiClass && isTestClass((PsiClass) clazz, false);
	}

	@Override
	public boolean isPotentialTestClass(@NotNull PsiElement clazz)
	{
		return clazz instanceof PsiClass && isTestClass((PsiClass) clazz, true);
	}

	protected abstract boolean isTestClass(PsiClass clazz, boolean canBePotential);

	protected boolean isUnderTestSources(PsiClass clazz)
	{
		PsiFile psiFile = clazz.getContainingFile();
		VirtualFile vFile = psiFile.getVirtualFile();
		if(vFile == null)
		{
			return false;
		}
		return ProjectRootManager.getInstance(clazz.getProject()).getFileIndex().isInTestSourceContent(vFile);
	}

	@Override
	@Nullable
	public PsiElement findSetUpMethod(@NotNull PsiElement clazz)
	{
		return clazz instanceof PsiClass ? findSetUpMethod((PsiClass) clazz) : null;
	}

	@Nullable
	protected abstract PsiMethod findSetUpMethod(@NotNull PsiClass clazz);

	@Override
	@Nullable
	public PsiElement findTearDownMethod(@NotNull PsiElement clazz)
	{
		return clazz instanceof PsiClass ? findTearDownMethod((PsiClass) clazz) : null;
	}

	@Nullable
	protected abstract PsiMethod findTearDownMethod(@NotNull PsiClass clazz);

	@Override
	public PsiElement findOrCreateSetUpMethod(@NotNull PsiElement clazz) throws IncorrectOperationException
	{
		return clazz instanceof PsiClass ? findOrCreateSetUpMethod((PsiClass) clazz) : null;
	}

	@Override
	public boolean isIgnoredMethod(PsiElement element)
	{
		return false;
	}

	@Override
	@NotNull
	public Language getLanguage()
	{
		return JavaLanguage.INSTANCE;
	}

	@Nullable
	protected abstract PsiMethod findOrCreateSetUpMethod(PsiClass clazz) throws IncorrectOperationException;

	public boolean isParameterized(PsiClass clazz)
	{
		return false;
	}

	@Nullable
	public PsiMethod findParametersMethod(PsiClass clazz)
	{
		return null;
	}

	@Nullable
	public FileTemplateDescriptor getParametersMethodFileTemplateDescriptor()
	{
		return null;
	}

	public abstract char getMnemonic();

	public PsiMethod createSetUpPatternMethod(JVMElementFactory factory)
	{
		final FileTemplate template = FileTemplateManager.getDefaultInstance().getCodeTemplate(getSetUpMethodFileTemplateDescriptor().getFileName());
		final String templateText = StringUtil.replace(StringUtil.replace(template.getText(), "${BODY}\n", ""), "${NAME}", "setUp");
		return factory.createMethodFromText(templateText, null);
	}

	public FileTemplateDescriptor getTestClassFileTemplateDescriptor()
	{
		return null;
	}

	public void setupLibrary(Module module)
	{
	}

	public boolean isSingleConfig()
	{
		return false;
	}

	public boolean isTestMethod(PsiMethod method, PsiClass myClass)
	{
		return isTestMethod(method);
	}
}
