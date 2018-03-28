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
package com.intellij.codeInsight.daemon.impl.quickfix;

import java.util.List;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.Nls;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.LowPriorityAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionStatement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiIfStatement;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.IncorrectOperationException;
import com.siyeh.ig.psiutils.BlockUtils;
import com.siyeh.ig.psiutils.SideEffectChecker;
import com.siyeh.ig.psiutils.StatementExtractor;
import consulo.java.JavaQuickFixBundle;

public class DeleteSideEffectsAwareFix implements IntentionAction, LowPriorityAction
{
	private final SmartPsiElementPointer<PsiExpressionStatement> myPointer;
	private final String myMessage;

	public DeleteSideEffectsAwareFix(PsiExpressionStatement statement)
	{
		myPointer = SmartPointerManager.getInstance(statement.getProject()).createSmartPsiElementPointer(statement);
		PsiExpression expression = statement.getExpression();
		List<PsiExpression> sideEffects = SideEffectChecker.extractSideEffectExpressions(expression);
		if(sideEffects.isEmpty())
		{
			myMessage = JavaQuickFixBundle.message("delete.element.fix.text");
		}
		else if(sideEffects.size() == 1 && sideEffects.get(0) == PsiUtil.skipParenthesizedExprDown(expression))
		{
			// "Remove unnecessary parentheses" action is already present which will do the same
			myMessage = "";
		}
		else
		{
			PsiStatement[] statements = StatementExtractor.generateStatements(sideEffects, expression);
			if(statements.length == 1)
			{
				if(statements[0] instanceof PsiIfStatement)
				{
					myMessage = JavaQuickFixBundle.message("extract.side.effects.convert.to.if");
				}
				else
				{
					myMessage = JavaQuickFixBundle.message("extract.side.effects.single");
				}
			}
			else
			{
				myMessage = JavaQuickFixBundle.message("extract.side.effects.multiple");
			}
		}
	}

	@Nls
	@Nonnull
	@Override
	public String getText()
	{
		return myMessage;
	}

	@Nls
	@Nonnull
	@Override
	public String getFamilyName()
	{
		return JavaQuickFixBundle.message("extract.side.effects.family.name");
	}

	@Override
	public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file)
	{
		return !myMessage.isEmpty();
	}

	@Override
	public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException
	{
		PsiExpressionStatement statement = myPointer.getElement();
		if(statement == null)
		{
			return;
		}
		PsiExpression expression = statement.getExpression();
		List<PsiExpression> sideEffects = SideEffectChecker.extractSideEffectExpressions(expression);
		PsiStatement[] statements = StatementExtractor.generateStatements(sideEffects, expression);
		if(statements.length > 0)
		{
			BlockUtils.addBefore(statement, statements);
		}
		statement.delete();
	}

	@Override
	public boolean startInWriteAction()
	{
		return true;
	}
}
