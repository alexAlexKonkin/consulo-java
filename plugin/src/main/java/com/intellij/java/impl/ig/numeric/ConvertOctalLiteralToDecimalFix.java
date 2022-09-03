/*
 * Copyright 2010 Bas Leijdekkers
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
package com.intellij.java.impl.ig.numeric;

import javax.annotation.Nonnull;

import consulo.language.editor.inspection.ProblemDescriptor;
import com.intellij.java.language.psi.JavaPsiFacade;
import com.intellij.java.language.psi.PsiElementFactory;
import com.intellij.java.language.psi.PsiExpression;
import com.intellij.java.language.psi.PsiLiteralExpression;
import consulo.project.Project;
import com.intellij.psi.*;
import consulo.language.util.IncorrectOperationException;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.InspectionGadgetsFix;

class ConvertOctalLiteralToDecimalFix
  extends InspectionGadgetsFix {

  @Nonnull
  public String getName() {
    return InspectionGadgetsBundle.message(
      "convert.octal.literal.to.decimal.literal.quickfix");
  }

  @Override
  protected void doFix(Project project, ProblemDescriptor descriptor)
    throws IncorrectOperationException {
    final PsiElement element = descriptor.getPsiElement();
    if (!(element instanceof PsiLiteralExpression)) return;

    final Object value = ((PsiLiteralExpression)element).getValue();
    if (value == null) return;

    final JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
    final PsiElementFactory factory = psiFacade.getElementFactory();
    final PsiExpression decimalNumber =
      factory.createExpressionFromText(value.toString(),
                                       element);
    element.replace(decimalNumber);
  }
}
