/*
 * Copyright 2011 Bas Leijdekkers
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
package com.intellij.java.impl.ipp.braces;

import javax.annotation.Nonnull;

import com.intellij.java.language.psi.PsiArrayInitializerExpression;
import consulo.language.psi.PsiElement;
import com.intellij.java.language.psi.PsiNewExpression;
import com.intellij.java.impl.ipp.base.PsiElementPredicate;

class ArrayCreationExpressionPredicate implements PsiElementPredicate {

  public boolean satisfiedBy(@Nonnull PsiElement element) {
    if (!(element instanceof PsiArrayInitializerExpression)) {
      return false;
    }
    final PsiArrayInitializerExpression arrayInitializerExpression =
      (PsiArrayInitializerExpression)element;
    if (arrayInitializerExpression.getType() == null) {
      return false;
    }
    final PsiElement parent = element.getParent();
    return !(parent instanceof PsiNewExpression);
  }
}