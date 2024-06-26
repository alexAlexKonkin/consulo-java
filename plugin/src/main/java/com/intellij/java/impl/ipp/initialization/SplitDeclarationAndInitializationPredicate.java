/*
 * Copyright 2003-2005 Dave Griffith
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
package com.intellij.java.impl.ipp.initialization;

import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiExpression;
import com.intellij.java.language.psi.PsiField;
import consulo.language.psi.*;
import com.intellij.java.impl.ipp.base.PsiElementPredicate;
import com.intellij.java.impl.ipp.psiutils.ErrorUtil;
import jakarta.annotation.Nonnull;

class SplitDeclarationAndInitializationPredicate
  implements PsiElementPredicate {

  public boolean satisfiedBy(@Nonnull PsiElement element) {
    final PsiElement parent = element.getParent();
    if (!(parent instanceof PsiField)) {
      return false;
    }
    if (element instanceof PsiComment &&
        element == parent.getFirstChild()) {
      return false;
    }
    final PsiField field = (PsiField)parent;
    final PsiExpression initializer = field.getInitializer();
    if (initializer == null) {
      return false;
    }
    final PsiClass containingClass = field.getContainingClass();
    if (containingClass == null || containingClass.isInterface()) {
      return false;
    }
    return !ErrorUtil.containsError(field);
  }
}
