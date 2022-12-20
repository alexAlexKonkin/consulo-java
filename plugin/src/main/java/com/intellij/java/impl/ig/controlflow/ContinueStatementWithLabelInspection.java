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
package com.intellij.java.impl.ig.controlflow;

import javax.annotation.Nonnull;

import com.intellij.java.language.psi.PsiContinueStatement;
import com.intellij.java.language.psi.PsiIdentifier;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import consulo.annotation.component.ExtensionImpl;

@ExtensionImpl
public class ContinueStatementWithLabelInspection extends BaseInspection {

  @Nonnull
  public String getDisplayName() {
    return InspectionGadgetsBundle.message(
      "continue.statement.with.label.display.name");
  }

  @Nonnull
  protected String buildErrorString(Object... infos) {
    return InspectionGadgetsBundle.message(
      "continue.statement.with.label.problem.descriptor");
  }

  public BaseInspectionVisitor buildVisitor() {
    return new ContinueStatementWithLabelVisitor();
  }

  private static class ContinueStatementWithLabelVisitor
    extends BaseInspectionVisitor {

    @Override
    public void visitContinueStatement(
      @Nonnull PsiContinueStatement statement) {
      super.visitContinueStatement(statement);
      final PsiIdentifier label = statement.getLabelIdentifier();
      if (label == null) {
        return;
      }
      final String labelText = label.getText();
      if (labelText == null) {
        return;
      }
      if (labelText.length() == 0) {
        return;
      }
      registerStatementError(statement);
    }
  }
}