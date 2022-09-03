/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.java.impl.codeInsight.intention.impl;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.Nls;

import javax.annotation.Nullable;
import consulo.language.editor.inspection.LocalQuickFixAndIntentionActionOnPsiElement;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import com.intellij.java.language.psi.JavaPsiFacade;
import com.intellij.java.language.psi.PsiAssignmentExpression;
import com.intellij.java.language.psi.PsiBinaryExpression;
import consulo.language.psi.PsiElement;
import com.intellij.java.language.psi.PsiExpression;
import consulo.language.psi.PsiFile;
import consulo.language.codeStyle.CodeStyleManager;
import com.siyeh.InspectionGadgetsBundle;

public class ReplaceAssignmentWithComparisonFix extends LocalQuickFixAndIntentionActionOnPsiElement
{
	public ReplaceAssignmentWithComparisonFix(PsiAssignmentExpression expr)
	{
		super(expr);
	}

	@Override
	public void invoke(@Nonnull Project project, @Nonnull PsiFile file, @Nullable Editor editor, @Nonnull PsiElement startElement, @Nonnull PsiElement endElement)
	{
		PsiBinaryExpression comparisonExpr = (PsiBinaryExpression) JavaPsiFacade.getElementFactory(project).createExpressionFromText("a==b", startElement);
		PsiAssignmentExpression assignmentExpression = (PsiAssignmentExpression) startElement;
		comparisonExpr.getLOperand().replace(assignmentExpression.getLExpression());
		PsiExpression rOperand = comparisonExpr.getROperand();
		assert rOperand != null;
		PsiExpression rExpression = assignmentExpression.getRExpression();
		assert rExpression != null;
		rOperand.replace(rExpression);
		CodeStyleManager.getInstance(project).reformat(assignmentExpression.replace(comparisonExpr));
	}

	@Nonnull
	@Override
	public String getText()
	{
		return getFamilyName();
	}

	@Nls
	@Nonnull
	@Override
	public String getFamilyName()
	{
		return InspectionGadgetsBundle.message("assignment.used.as.condition.replace.quickfix");
	}
}
