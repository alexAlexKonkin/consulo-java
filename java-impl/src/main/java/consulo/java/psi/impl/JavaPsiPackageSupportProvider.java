/*
 * Copyright 2013 Consulo.org
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
package consulo.java.psi.impl;

import javax.annotation.Nonnull;

import consulo.java.language.module.extension.JavaModuleExtension;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiManager;
import com.intellij.java.language.psi.PsiNameHelper;
import com.intellij.psi.impl.file.PsiPackageImpl;
import consulo.module.extension.ModuleExtension;
import consulo.psi.PsiPackage;
import consulo.psi.PsiPackageManager;
import consulo.psi.PsiPackageSupportProvider;

/**
 * @author VISTALL
 * @since 8:43/20.05.13
 */
public class JavaPsiPackageSupportProvider implements PsiPackageSupportProvider
{
	@Override
	public boolean isSupported(@Nonnull ModuleExtension moduleExtension)
	{
		return moduleExtension instanceof JavaModuleExtension;
	}

	@Override
	public boolean isValidPackageName(@Nonnull Module module, @Nonnull String packageName)
	{
		return PsiNameHelper.getInstance(module.getProject()).isQualifiedName(packageName);
	}

	@Nonnull
	@Override
	public PsiPackage createPackage(
			@Nonnull PsiManager psiManager,
			@Nonnull PsiPackageManager packageManager,
			@Nonnull Class<? extends ModuleExtension> extensionClass,
			@Nonnull String packageName)
	{
		return new PsiPackageImpl(psiManager, packageManager, extensionClass, packageName);
	}
}
