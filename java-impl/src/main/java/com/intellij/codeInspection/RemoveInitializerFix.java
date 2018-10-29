/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.codeInspection;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInsight.daemon.impl.quickfix.RemoveUnusedVariableFix;
import com.intellij.codeInsight.daemon.impl.quickfix.RemoveUnusedVariableUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionStatement;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.util.PsiExpressionTrimRenderer;
import com.intellij.psi.util.PsiUtil;
import consulo.application.AccessRule;

public class RemoveInitializerFix implements LocalQuickFix
{
	private static final Logger LOG = Logger.getInstance("#" + RemoveInitializerFix.class.getName());

	@Override
	@Nonnull
	public String getName()
	{
		return InspectionsBundle.message("inspection.unused.assignment.remove.quickfix");
	}

	@Override
	public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor)
	{
		final PsiElement psiInitializer = descriptor.getPsiElement();
		if(!(psiInitializer instanceof PsiExpression))
		{
			return;
		}
		if(!(psiInitializer.getParent() instanceof PsiVariable))
		{
			return;
		}

		final PsiVariable variable = (PsiVariable) psiInitializer.getParent();
		sideEffectAwareRemove(project, psiInitializer, psiInitializer, variable);
	}

	@Override
	public boolean startInWriteAction()
	{
		return false;
	}

	protected void sideEffectAwareRemove(Project project, PsiElement psiInitializer, PsiElement elementToDelete, PsiVariable variable)
	{
		if(!FileModificationService.getInstance().prepareFileForWrite(elementToDelete.getContainingFile()))
		{
			return;
		}

		final PsiElement declaration = variable.getParent();
		final List<PsiElement> sideEffects = new ArrayList<>();
		boolean hasSideEffects = RemoveUnusedVariableUtil.checkSideEffects(psiInitializer, variable, sideEffects);
		int res;
		if(hasSideEffects)
		{
			hasSideEffects = PsiUtil.isStatement(psiInitializer);
			res = RemoveUnusedVariableFix.showSideEffectsWarning(sideEffects, variable, FileEditorManager.getInstance(project).getSelectedTextEditor
					(), hasSideEffects, sideEffects.get(0).getText(), variable.getTypeElement().getText() +
					" " +
					variable.getName() +
					";<br>" +
					PsiExpressionTrimRenderer.render((PsiExpression) psiInitializer));
		}
		else
		{
			res = RemoveUnusedVariableUtil.DELETE_ALL;
		}

		AccessRule.writeAsync(() -> {
			if(res == RemoveUnusedVariableUtil.DELETE_ALL)
			{
				elementToDelete.delete();
			}
			else if(res == RemoveUnusedVariableUtil.MAKE_STATEMENT)
			{
				final PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
				final PsiStatement statementFromText = factory.createStatementFromText(psiInitializer.getText() + ";", null);
				final PsiElement parent = elementToDelete.getParent();
				if(parent instanceof PsiExpressionStatement)
				{
					parent.replace(statementFromText);
				}
				else
				{
					declaration.getParent().addAfter(statementFromText, declaration);
					elementToDelete.delete();
				}
			}
		});
	}

	@Override
	@Nonnull
	public String getFamilyName()
	{
		return getName();
	}
}
