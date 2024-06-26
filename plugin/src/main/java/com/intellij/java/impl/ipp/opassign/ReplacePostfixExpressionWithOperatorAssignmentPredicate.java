/*
 * Copyright 2009-2012 Bas Leijdekkers
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
package com.intellij.java.impl.ipp.opassign;

import com.intellij.java.impl.ipp.base.PsiElementPredicate;
import consulo.language.psi.PsiElement;
import com.intellij.java.language.psi.PsiPostfixExpression;
import com.intellij.java.language.psi.JavaTokenType;
import consulo.language.ast.IElementType;

class ReplacePostfixExpressionWithOperatorAssignmentPredicate implements PsiElementPredicate {

  public boolean satisfiedBy(PsiElement element) {
    if (!(element instanceof PsiPostfixExpression)) {
      return false;
    }
    final PsiPostfixExpression postfixExpression = (PsiPostfixExpression)element;
    final IElementType tokenType = postfixExpression.getOperationTokenType();
    return !(!JavaTokenType.PLUSPLUS.equals(tokenType) && !JavaTokenType.MINUSMINUS.equals(tokenType));
  }
}