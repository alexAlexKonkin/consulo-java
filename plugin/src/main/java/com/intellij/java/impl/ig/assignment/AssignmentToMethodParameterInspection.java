/*
 * Copyright 2003-2009 Dave Griffith, Bas Leijdekkers
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
package com.intellij.java.impl.ig.assignment;

import com.intellij.java.language.psi.*;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.*;
import consulo.language.ast.IElementType;
import consulo.language.editor.inspection.ui.SingleCheckboxOptionsPanel;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import com.intellij.java.impl.ig.fixes.ExtractParameterAsLocalVariableFix;
import com.siyeh.ig.psiutils.VariableAccessUtils;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;

@ExtensionImpl
public class AssignmentToMethodParameterInspection
  extends BaseInspection {

  @SuppressWarnings({"PublicField"})
  public boolean ignoreTransformationOfOriginalParameter = false;

  @Override
  @Nonnull
  public String getDisplayName() {
    return InspectionGadgetsBundle.message(
      "assignment.to.method.parameter.display.name");
  }

  @Override
  @Nonnull
  public String buildErrorString(Object... infos) {
    return InspectionGadgetsBundle.message(
      "assignment.to.method.parameter.problem.descriptor");
  }

  @Override
  @Nullable
  public JComponent createOptionsPanel() {
    return new SingleCheckboxOptionsPanel(InspectionGadgetsBundle.message(
      "assignment.to.method.parameter.ignore.transformation.option"), this,
                                          "ignoreTransformationOfOriginalParameter");
  }

  @Override
  protected InspectionGadgetsFix buildFix(Object... infos) {
    return new ExtractParameterAsLocalVariableFix();
  }

  @Override
  public BaseInspectionVisitor buildVisitor() {
    return new AssignmentToMethodParameterVisitor();
  }

  private class AssignmentToMethodParameterVisitor
    extends BaseInspectionVisitor {

    @Override
    public void visitAssignmentExpression(
      @Nonnull PsiAssignmentExpression expression) {
      super.visitAssignmentExpression(expression);
      final PsiExpression lhs = expression.getLExpression();
      final PsiParameter parameter = getMethodParameter(lhs);
      if (parameter == null) {
        return;
      }
      if (ignoreTransformationOfOriginalParameter) {
        final PsiExpression rhs = expression.getRExpression();
        if (rhs != null && VariableAccessUtils.variableIsUsed(parameter, rhs)) {
          return;
        }
        final IElementType tokenType =
          expression.getOperationTokenType();
        if (tokenType == JavaTokenType.PLUSEQ ||
            tokenType == JavaTokenType.MINUSEQ ||
            tokenType == JavaTokenType.ASTERISKEQ ||
            tokenType == JavaTokenType.DIVEQ ||
            tokenType == JavaTokenType.ANDEQ ||
            tokenType == JavaTokenType.OREQ ||
            tokenType == JavaTokenType.XOREQ ||
            tokenType == JavaTokenType.PERCEQ ||
            tokenType == JavaTokenType.LTLTEQ ||
            tokenType == JavaTokenType.GTGTEQ ||
            tokenType == JavaTokenType.GTGTGTEQ) {
          return;
        }
      }
      registerError(lhs);
    }

    @Override
    public void visitPrefixExpression(
      @Nonnull PsiPrefixExpression expression) {
      if (ignoreTransformationOfOriginalParameter) {
        return;
      }
      super.visitPrefixExpression(expression);
      final IElementType tokenType = expression.getOperationTokenType();
      if (!tokenType.equals(JavaTokenType.PLUSPLUS) &&
          !tokenType.equals(JavaTokenType.MINUSMINUS)) {
        return;
      }
      final PsiExpression operand = expression.getOperand();
      if (operand == null) {
        return;
      }
      final PsiParameter parameter = getMethodParameter(operand);
      if (parameter == null) {
        return;
      }
      registerError(operand);
    }

    @Override
    public void visitPostfixExpression(
      @Nonnull PsiPostfixExpression expression) {
      if (ignoreTransformationOfOriginalParameter) {
        return;
      }
      super.visitPostfixExpression(expression);
      final IElementType tokenType = expression.getOperationTokenType();
      if (!tokenType.equals(JavaTokenType.PLUSPLUS) &&
          !tokenType.equals(JavaTokenType.MINUSMINUS)) {
        return;
      }
      final PsiExpression operand = expression.getOperand();
      final PsiParameter parameter = getMethodParameter(operand);
      if (parameter == null) {
        return;
      }
      registerError(operand);
    }

    @Nullable
    private PsiParameter getMethodParameter(PsiExpression expression) {
      if (!(expression instanceof PsiReferenceExpression)) {
        return null;
      }
      final PsiReferenceExpression referenceExpression =
        (PsiReferenceExpression)expression;
      final PsiElement variable = referenceExpression.resolve();
      if (!(variable instanceof PsiParameter)) {
        return null;
      }
      final PsiParameter parameter = (PsiParameter)variable;
      final PsiElement declarationScope = parameter.getDeclarationScope();
      if (declarationScope instanceof PsiCatchSection) {
        return null;
      }
      if (declarationScope instanceof PsiForeachStatement) {
        return null;
      }
      return parameter;
    }
  }
}