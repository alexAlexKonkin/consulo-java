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
package com.intellij.java.impl.codeInsight.editorActions.smartEnter;

import consulo.codeEditor.Editor;
import com.intellij.java.language.psi.JavaTokenType;
import consulo.language.psi.PsiElement;
import com.intellij.java.language.psi.PsiNewExpression;
import consulo.language.util.IncorrectOperationException;

public class MissingArrayConstructorBracketFixer implements Fixer
{
	@Override
	public void apply(Editor editor, JavaSmartEnterProcessor processor, PsiElement psiElement) throws IncorrectOperationException
	{
		if(!(psiElement instanceof PsiNewExpression))
		{
			return;
		}
		PsiNewExpression expr = (PsiNewExpression) psiElement;
		int count = 0;
		for(PsiElement element : expr.getChildren())
		{
			if(element.getNode().getElementType() == JavaTokenType.LBRACKET)
			{
				count++;
			}
			else if(element.getNode().getElementType() == JavaTokenType.RBRACKET)
			{
				count--;
			}
		}
		if(count > 0)
		{
			editor.getDocument().insertString(psiElement.getTextRange().getEndOffset(), "]");
		}
	}
}