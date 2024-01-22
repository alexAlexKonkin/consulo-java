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
package com.intellij.java.impl.ig.classlayout;

import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiDeclarationStatement;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import com.intellij.java.impl.ig.fixes.MoveClassFix;
import consulo.annotation.component.ExtensionImpl;
import jakarta.annotation.Nonnull;

@ExtensionImpl
public class LimitedScopeInnerClassInspection extends BaseInspection {

  @Nonnull
  public String getDisplayName() {
    return InspectionGadgetsBundle.message(
      "limited.scope.inner.class.display.name");
  }

  @Nonnull
  protected String buildErrorString(Object... infos) {
    return InspectionGadgetsBundle.message(
      "limited.scope.inner.class.problem.descriptor");
  }

  protected InspectionGadgetsFix buildFix(Object... infos) {
    return new MoveClassFix();
  }

  protected boolean buildQuickFixesOnlyForOnTheFlyErrors() {
    return true;
  }

  public BaseInspectionVisitor buildVisitor() {
    return new LimitedScopeInnerClassVisitor();
  }

  private static class LimitedScopeInnerClassVisitor
    extends BaseInspectionVisitor {

    @Override
    public void visitClass(@jakarta.annotation.Nonnull PsiClass aClass) {
      if (aClass.getParent() instanceof PsiDeclarationStatement) {
        registerClassError(aClass);
      }
    }
  }
}