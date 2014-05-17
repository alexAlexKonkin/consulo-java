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
package com.intellij.codeInsight.template.postfix.templates;

import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiParenthesizedExpression;

public class ParenthesizedExpressionPostfixTemplate extends ExpressionPostfixTemplateWithChooser {
  public ParenthesizedExpressionPostfixTemplate() {
    super("par", "(expression)");
  }

  @Override
  protected void doIt(@NotNull Editor editor, @NotNull PsiExpression expression) {
    PsiElementFactory factory = JavaPsiFacade.getInstance(expression.getProject()).getElementFactory();
    PsiParenthesizedExpression parenthesizedExpression = (PsiParenthesizedExpression)factory.createExpressionFromText("(expr)", expression.getParent());
    PsiExpression operand = parenthesizedExpression.getExpression();
    assert operand != null;
    operand.replace(expression);
    expression.replace(parenthesizedExpression);
  }
}