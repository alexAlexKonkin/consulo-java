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
package com.intellij.java.impl.ig.jdk;

import com.intellij.java.language.psi.PsiAssertStatement;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import consulo.annotation.component.ExtensionImpl;

import jakarta.annotation.Nonnull;

@ExtensionImpl
public class AssertStatementInspection extends BaseInspection {

  @jakarta.annotation.Nonnull
  public String getDisplayName() {
    return InspectionGadgetsBundle.message(
      "assert.statement.display.name");
  }

  @Nonnull
  public String buildErrorString(Object... infos) {
    return InspectionGadgetsBundle.message(
      "statement.problem.descriptor");
  }

  public BaseInspectionVisitor buildVisitor() {
    return new AssertStatementVisitor();
  }

  private static class AssertStatementVisitor
    extends BaseInspectionVisitor {

    @Override
    public void visitAssertStatement(PsiAssertStatement statement) {
      super.visitAssertStatement(statement);
      registerStatementError(statement);
    }
  }
}