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

import jakarta.annotation.Nonnull;

import com.intellij.java.language.psi.PsiMethodCallExpression;
import com.intellij.java.language.psi.PsiType;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.psiutils.ControlFlowUtils;
import com.siyeh.ig.psiutils.MethodCallUtils;
import consulo.annotation.component.ExtensionImpl;

@ExtensionImpl
public class BusyWaitInspection extends BaseInspection {

  @Nonnull
  public String getDisplayName() {
    return InspectionGadgetsBundle.message("busy.wait.display.name");
  }

  @Nonnull
  protected String buildErrorString(Object... infos) {
    return InspectionGadgetsBundle.message("busy.wait.problem.descriptor");
  }

  public BaseInspectionVisitor buildVisitor() {
    return new BusyWaitVisitor();
  }

  private static class BusyWaitVisitor extends BaseInspectionVisitor {

    @Override
    public void visitMethodCallExpression(
      @jakarta.annotation.Nonnull PsiMethodCallExpression expression) {
      super.visitMethodCallExpression(expression);
      if (!MethodCallUtils.isCallToMethod(expression, "java.lang.Thread",
                                          PsiType.VOID, "sleep", PsiType.LONG) &&
          !MethodCallUtils.isCallToMethod(expression,
                                          "java.lang.Thread", PsiType.VOID, "sleep",
                                          PsiType.LONG, PsiType.INT)) {
        return;
      }
      if (!ControlFlowUtils.isInLoop(expression)) {
        return;
      }
      registerMethodCallError(expression);
    }
  }
}