/*
 * Copyright 2003-2007 Dave Griffith, Bas Leijdekkers
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
package com.intellij.java.impl.ig.threading;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.inspection.ProblemDescriptor;
import com.intellij.java.language.psi.PsiExpression;
import com.intellij.java.language.psi.PsiExpressionList;
import com.intellij.java.language.psi.PsiMethodCallExpression;
import com.intellij.java.language.psi.PsiReferenceExpression;
import consulo.project.Project;
import consulo.language.psi.*;
import consulo.language.util.IncorrectOperationException;
import com.siyeh.HardcodedMethodConstants;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import jakarta.annotation.Nonnull;

@ExtensionImpl
public class ObjectNotifyInspection extends BaseInspection {

  @jakarta.annotation.Nonnull
  public String getID() {
    return "CallToNotifyInsteadOfNotifyAll";
  }

  @jakarta.annotation.Nonnull
  public String getDisplayName() {
    return InspectionGadgetsBundle.message("object.notify.display.name");
  }

  @jakarta.annotation.Nonnull
  protected String buildErrorString(Object... infos) {
    return InspectionGadgetsBundle.message(
      "object.notify.problem.descriptor");
  }

  public BaseInspectionVisitor buildVisitor() {
    return new ObjectNotifyVisitor();
  }

  public InspectionGadgetsFix buildFix(Object... infos) {
    return new ObjectNotifyFix();
  }

  private static class ObjectNotifyFix extends InspectionGadgetsFix {

    @Nonnull
    public String getName() {
      return InspectionGadgetsBundle.message(
        "object.notify.replace.quickfix");
    }

    public void doFix(Project project, ProblemDescriptor descriptor)
      throws IncorrectOperationException {
      final PsiElement methodNameElement = descriptor.getPsiElement();
      final PsiReferenceExpression methodExpression =
        (PsiReferenceExpression)methodNameElement.getParent();
      assert methodExpression != null;
      final PsiExpression qualifier =
        methodExpression.getQualifierExpression();
      if (qualifier == null) {
        replaceExpression(methodExpression,
                          HardcodedMethodConstants.NOTIFY_ALL);
      }
      else {
        final String qualifierText = qualifier.getText();
        replaceExpression(methodExpression,
                          qualifierText + '.' +
                          HardcodedMethodConstants.NOTIFY_ALL);
      }
    }
  }

  private static class ObjectNotifyVisitor extends BaseInspectionVisitor {

    @Override
    public void visitMethodCallExpression(
      @jakarta.annotation.Nonnull PsiMethodCallExpression expression) {
      super.visitMethodCallExpression(expression);
      final PsiReferenceExpression methodExpression =
        expression.getMethodExpression();
      final String methodName = methodExpression.getReferenceName();

      if (!HardcodedMethodConstants.NOTIFY.equals(methodName)) {
        return;
      }
      final PsiExpressionList argumentList = expression.getArgumentList();
      if (argumentList.getExpressions().length != 0) {
        return;
      }
      registerMethodCallError(expression);
    }
  }
}