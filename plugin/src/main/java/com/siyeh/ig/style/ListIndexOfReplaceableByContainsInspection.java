/*
 * Copyright 2006-2011 Bas Leijdekkers
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
package com.siyeh.ig.style;

import javax.annotation.Nonnull;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.java.language.psi.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.IncorrectOperationException;
import com.siyeh.HardcodedMethodConstants;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import com.siyeh.ig.psiutils.ComparisonUtils;
import com.siyeh.ig.psiutils.ExpressionUtils;
import consulo.java.language.module.util.JavaClassNames;
import org.jetbrains.annotations.NonNls;

public class ListIndexOfReplaceableByContainsInspection
  extends BaseInspection {

  @Override
  @Nonnull
  public String getDisplayName() {
    return InspectionGadgetsBundle.message(
      "indexof.replaceable.by.contains.display.name");
  }

  @Override
  @Nonnull
  public String buildErrorString(Object... infos) {
    final PsiBinaryExpression expression = (PsiBinaryExpression)infos[0];
    final PsiExpression lhs = expression.getLOperand();
    final String text;
    if (lhs instanceof PsiMethodCallExpression) {
      final PsiMethodCallExpression callExpression =
        (PsiMethodCallExpression)lhs;
      text = createContainsExpressionText(callExpression, false,
                                          expression.getOperationTokenType());
    }
    else {
      final PsiMethodCallExpression callExpression =
        (PsiMethodCallExpression)expression.getROperand();
      assert callExpression != null;
      text = createContainsExpressionText(callExpression, true,
                                          expression.getOperationTokenType());
    }
    return InspectionGadgetsBundle.message(
      "expression.can.be.replaced.problem.descriptor", text);
  }

  @Override
  @javax.annotation.Nullable
  protected InspectionGadgetsFix buildFix(Object... infos) {
    return new IndexOfReplaceableByContainsFix();
  }

  private static class IndexOfReplaceableByContainsFix
    extends InspectionGadgetsFix {

    @Override
    protected void doFix(Project project, ProblemDescriptor descriptor)
      throws IncorrectOperationException {
      final PsiBinaryExpression expression =
        (PsiBinaryExpression)descriptor.getPsiElement();
      final PsiExpression lhs = expression.getLOperand();
      final PsiExpression rhs = expression.getROperand();
      final String newExpressionText;
      if (lhs instanceof PsiMethodCallExpression) {
        final PsiMethodCallExpression callExpression =
          (PsiMethodCallExpression)lhs;
        newExpressionText =
          createContainsExpressionText(callExpression, false,
                                       expression.getOperationTokenType());
      }
      else {
        final PsiMethodCallExpression callExpression =
          (PsiMethodCallExpression)rhs;
        assert callExpression != null;
        newExpressionText =
          createContainsExpressionText(callExpression, true,
                                       expression.getOperationTokenType());
      }
      replaceExpression(expression, newExpressionText);
    }

    @Nonnull
    public String getName() {
      return InspectionGadgetsBundle.message(
        "replace.indexof.with.contains.quickfix");
    }
  }

  static String createContainsExpressionText(
    @Nonnull PsiMethodCallExpression call,
    boolean flipped, IElementType tokenType) {
    final PsiReferenceExpression methodExpression =
      call.getMethodExpression();
    final PsiExpression qualifierExpression =
      methodExpression.getQualifierExpression();
    final String qualifierText;
    if (qualifierExpression == null) {
      qualifierText = "";
    }
    else {
      qualifierText = qualifierExpression.getText();
    }
    final PsiExpressionList argumentList = call.getArgumentList();
    final PsiExpression expression = argumentList.getExpressions()[0];
    @NonNls final String newExpressionText =
      qualifierText + ".contains(" + expression.getText() + ')';
    if (tokenType.equals(JavaTokenType.EQEQ)) {
      return '!' + newExpressionText;
    }
    else if (!flipped && (tokenType.equals(JavaTokenType.LT) ||
                          tokenType.equals(JavaTokenType.LE))) {
      return '!' + newExpressionText;
    }
    else if (flipped && (tokenType.equals(JavaTokenType.GT) ||
                         tokenType.equals(JavaTokenType.GE))) {
      return '!' + newExpressionText;
    }
    return newExpressionText;
  }

  @Override
  public BaseInspectionVisitor buildVisitor() {
    return new IndexOfReplaceableByContainsVisitor();
  }

  private static class IndexOfReplaceableByContainsVisitor
    extends BaseInspectionVisitor {

    @Override
    public void visitBinaryExpression(
      PsiBinaryExpression expression) {
      super.visitBinaryExpression(expression);
      final PsiExpression rhs = expression.getROperand();
      if (rhs == null) {
        return;
      }
      if (!ComparisonUtils.isComparison(expression)) {
        return;
      }
      final PsiExpression lhs = expression.getLOperand();
      if (lhs instanceof PsiMethodCallExpression) {
        if (canBeReplacedByContains(lhs, rhs, false,
                                    expression.getOperationTokenType())) {
          registerError(expression, expression);
        }
      }
      else if (rhs instanceof PsiMethodCallExpression) {
        if (canBeReplacedByContains(rhs, lhs, true,
                                    expression.getOperationTokenType())) {
          registerError(expression, expression);
        }
      }
    }

    private static boolean canBeReplacedByContains(
      PsiExpression lhs,
      PsiExpression rhs, boolean flipped, IElementType tokenType) {
      final PsiMethodCallExpression callExpression =
        (PsiMethodCallExpression)lhs;
      if (!isIndexOfCall(callExpression)) {
        return false;
      }
      final Object object =
        ExpressionUtils.computeConstantExpression(rhs);
      if (!(object instanceof Integer)) {
        return false;
      }
      final Integer integer = (Integer)object;
      final int constant = integer.intValue();
      if (flipped) {
        if (constant == -1 && (JavaTokenType.NE.equals(tokenType) ||
                               JavaTokenType.LT.equals(tokenType) ||
                               JavaTokenType.EQEQ.equals(tokenType) ||
                               JavaTokenType.GE.equals(tokenType))) {
          return true;
        }
        else if (constant == 0 &&
                 (JavaTokenType.LE.equals(tokenType) ||
                  JavaTokenType.GT.equals(tokenType))) {
          return true;
        }
      }
      else {
        if (constant == -1 && (JavaTokenType.NE.equals(tokenType) ||
                               JavaTokenType.GT.equals(tokenType) ||
                               JavaTokenType.EQEQ.equals(tokenType) ||
                               JavaTokenType.LE.equals(tokenType))) {
          return true;
        }
        else if (constant == 0 &&
                 (JavaTokenType.GE.equals(tokenType) ||
                  JavaTokenType.LT.equals(tokenType))) {
          return true;
        }
      }
      return false;
    }

    private static boolean isIndexOfCall(
      @Nonnull PsiMethodCallExpression expression) {
      final PsiReferenceExpression methodExpression =
        expression.getMethodExpression();
      final String methodName = methodExpression.getReferenceName();
      if (!HardcodedMethodConstants.INDEX_OF.equals(methodName)) {
        return false;
      }
      final PsiExpressionList argumentList = expression.getArgumentList();
      final PsiExpression[] arguments = argumentList.getExpressions();
      if (arguments.length != 1) {
        return false;
      }
      final PsiExpression qualifier =
        methodExpression.getQualifierExpression();
      if (qualifier == null) {
        return false;
      }
      final PsiType qualifierType = qualifier.getType();
      if (qualifierType == null) {
        return false;
      }
      final Project project = expression.getProject();
      final GlobalSearchScope projectScope =
        GlobalSearchScope.allScope(project);
      final JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
      final PsiClass javaUtilListClass =
        psiFacade.findClass(JavaClassNames.JAVA_UTIL_LIST,
                            projectScope);
      if (javaUtilListClass == null) {
        return false;
      }
      final PsiElementFactory factory = psiFacade.getElementFactory();
      final PsiClassType javaUtilListType =
        factory.createType(javaUtilListClass);
      return javaUtilListType.isAssignableFrom(qualifierType);
    }
  }
}