/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.refactoring.actions;

import org.jetbrains.annotations.NotNull;
import com.intellij.ide.DataManager;
import com.intellij.lang.Language;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiJavaPackage;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.typeCook.TypeCookHandler;

public class TypeCookAction extends BaseRefactoringAction
{

	@Override
	protected boolean isAvailableInEditorOnly()
	{
		return false;
	}

	@Override
	public boolean isAvailableForLanguage(Language language)
	{
		return language.equals(JavaLanguage.INSTANCE);
	}

	@Override
	public boolean isEnabledOnElements(@NotNull PsiElement[] elements)
	{
		Project project = DataManager.getInstance().getDataContext().getData(CommonDataKeys.PROJECT);

		if(project == null)
		{
			return false;
		}

		for(int i = 0; i < elements.length; i++)
		{
			PsiElement element = elements[i];

			if(!(element instanceof PsiClass || element instanceof PsiJavaFile || element instanceof PsiDirectory || element instanceof PsiJavaPackage))
			{
				return false;
			}
		}

		return true;
	}

	@Override
	public RefactoringActionHandler getHandler(@NotNull DataContext dataContext)
	{
		return getHandler();
	}

	public RefactoringActionHandler getHandler()
	{
		return new TypeCookHandler();
	}
}
