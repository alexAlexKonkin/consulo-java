/*
 * Copyright 2006-2007 Dave Griffith, Bas Leijdekkers
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

import com.intellij.java.language.psi.JavaTokenType;
import com.intellij.java.language.psi.PsiJavaToken;
import com.intellij.java.language.psi.PsiPrefixExpression;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.ast.IElementType;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import javax.annotation.Nonnull;

@ExtensionImpl
public class UnaryPlusInspection extends BaseInspection {

  @Nonnull
  public String getDisplayName() {
    return InspectionGadgetsBundle.message("unary.plus.display.name");
  }

  @Nonnull
  protected String buildErrorString(Object... infos) {
    return InspectionGadgetsBundle.message(
      "unary.plus.problem.descriptor");
  }

  public BaseInspectionVisitor buildVisitor() {
    return new UnaryPlusVisitor();
  }

  private static class UnaryPlusVisitor extends BaseInspectionVisitor {

    @Override
    public void visitPrefixExpression(
      PsiPrefixExpression prefixExpression) {
      super.visitPrefixExpression(prefixExpression);
      final PsiJavaToken token = prefixExpression.getOperationSign();
      final IElementType tokenType = token.getTokenType();
      if (!tokenType.equals(JavaTokenType.PLUS)) {
        return;
      }
      registerError(token);
    }
  }
}