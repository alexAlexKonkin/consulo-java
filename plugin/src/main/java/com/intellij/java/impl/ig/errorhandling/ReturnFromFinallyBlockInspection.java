/*
 * Copyright 2003-2012 Dave Griffith, Bas Leijdekkers
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
package com.intellij.java.impl.ig.errorhandling;

import javax.annotation.Nonnull;

import com.intellij.java.language.psi.PsiReturnStatement;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.psiutils.ControlFlowUtils;
import consulo.annotation.component.ExtensionImpl;

@ExtensionImpl
public class ReturnFromFinallyBlockInspection extends BaseInspection {

  @Nonnull
  public String getID() {
    return "ReturnInsideFinallyBlock";
  }

  @Nonnull
  public String getDisplayName() {
    return InspectionGadgetsBundle.message("return.from.finally.block.display.name");
  }

  @Nonnull
  protected String buildErrorString(Object... infos) {
    return InspectionGadgetsBundle.message("return.from.finally.block.problem.descriptor");
  }

  public boolean isEnabledByDefault() {
    return true;
  }

  public BaseInspectionVisitor buildVisitor() {
    return new ReturnFromFinallyBlockVisitor();
  }

  private static class ReturnFromFinallyBlockVisitor extends BaseInspectionVisitor {

    @Override
    public void visitReturnStatement(@Nonnull PsiReturnStatement statement) {
      super.visitReturnStatement(statement);
      if (!ControlFlowUtils.isInFinallyBlock(statement)) {
        return;
      }
      registerStatementError(statement);
    }
  }
}