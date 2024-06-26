/*
 * Copyright 2007 Bas Leijdekkers
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
package com.intellij.java.impl.ipp.varargs;

import com.intellij.java.impl.ipp.base.Intention;
import com.intellij.java.impl.ipp.base.PsiElementPredicate;
import com.intellij.java.language.psi.*;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.intention.IntentionMetaData;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;

import jakarta.annotation.Nonnull;

@ExtensionImpl
@IntentionMetaData(ignoreId = "java.MakeMethodVarargsIntention", fileExtensions = "java", categories = {"Java", "Declaration"})
public class WrapVarargArgumentsWithExplicitArrayIntention extends Intention {

  @Override
  @Nonnull
  protected PsiElementPredicate getElementPredicate() {
    return new VarargArgumentsPredicate();
  }

  @Override
  protected void processIntention(@Nonnull PsiElement element)
    throws IncorrectOperationException {
    final PsiMethodCallExpression methodCallExpression =
      PsiTreeUtil.getParentOfType(element,
                                  PsiMethodCallExpression.class);
    if (methodCallExpression == null) {
      return;
    }
    final PsiMethod method = methodCallExpression.resolveMethod();
    if (method == null) {
      return;
    }
    final PsiParameterList parameterList = method.getParameterList();
    final int parametersCount = parameterList.getParametersCount();
    final PsiReferenceExpression methodExpression =
      methodCallExpression.getMethodExpression();
    final String methodExpressionText = methodExpression.getText();
    final StringBuilder newExpression =
      new StringBuilder(methodExpressionText);
    final PsiExpressionList argumentList =
      methodCallExpression.getArgumentList();
    final PsiExpression[] arguments = argumentList.getExpressions();
    newExpression.append('(');
    final int varargParameterIndex = parametersCount - 1;
    for (int i = 0; i < varargParameterIndex; i++) {
      newExpression.append(arguments[i].getText());
      newExpression.append(", ");
    }
    final PsiParameter[] parameters = parameterList.getParameters();
    final PsiParameter varargParameter = parameters[varargParameterIndex];
    final PsiArrayType type = (PsiArrayType)varargParameter.getType();
    newExpression.append("new ");
    final PsiType componentType = type.getComponentType();
    final JavaResolveResult resolveResult =
      methodCallExpression.resolveMethodGenerics();
    final PsiSubstitutor substitutor = resolveResult.getSubstitutor();
    final PsiType substitutedType = substitutor.substitute(componentType);
    newExpression.append(substitutedType.getCanonicalText());
    newExpression.append("[]{");
    if (arguments.length > varargParameterIndex) {
      newExpression.append(arguments[varargParameterIndex].getText());
      for (int i = parametersCount; i < arguments.length; i++) {
        newExpression.append(", ");
        newExpression.append(arguments[i].getText());
      }
    }
    newExpression.append("})");
    replaceExpression(newExpression.toString(), methodCallExpression);
  }
}