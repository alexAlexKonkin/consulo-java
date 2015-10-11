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
package org.mustbe.consulo.java.psi.impl;

import org.mustbe.consulo.java.module.extension.JavaModuleExtension;
import org.consulo.module.extension.ModuleExtension;
import org.consulo.psi.PsiPackage;
import org.consulo.psi.PsiPackageManager;
import org.consulo.psi.PsiPackageSupportProvider;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiNameHelper;
import com.intellij.psi.impl.file.PsiPackageImpl;

/**
 * @author VISTALL
 * @since 8:43/20.05.13
 */
public class JavaPsiPackageSupportProvider implements PsiPackageSupportProvider
{
	@Override
	public boolean isSupported(@NotNull ModuleExtension moduleExtension)
	{
		return moduleExtension instanceof JavaModuleExtension;
	}

	@Override
	public boolean isValidPackageName(@NotNull Module module, @NotNull String packageName)
	{
		return PsiNameHelper.getInstance(module.getProject()).isQualifiedName(packageName);
	}

	@NotNull
	@Override
	public PsiPackage createPackage(
			@NotNull PsiManager psiManager,
			@NotNull PsiPackageManager packageManager,
			@NotNull Class<? extends ModuleExtension> extensionClass,
			@NotNull String packageName)
	{
		return new PsiPackageImpl(psiManager, packageManager, extensionClass, packageName);
	}
}
