/*
 * Copyright 2003-2008 Dave Griffith, Bas Leijdekkers
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

import jakarta.annotation.Nonnull;

import com.intellij.java.language.psi.PsiAnonymousClass;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiEnumConstantInitializer;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import com.intellij.java.impl.ig.fixes.MoveAnonymousToInnerClassFix;
import consulo.annotation.component.ExtensionImpl;

@ExtensionImpl
public class AnonymousInnerClassInspection extends BaseInspection {

  @jakarta.annotation.Nonnull
  public String getDisplayName() {
    return InspectionGadgetsBundle.message(
      "anonymous.inner.class.display.name");
  }

  @jakarta.annotation.Nonnull
  protected String buildErrorString(Object... infos) {
    return InspectionGadgetsBundle.message(
      "anonymous.inner.class.problem.descriptor");
  }

  protected InspectionGadgetsFix buildFix(Object... infos) {
    return new MoveAnonymousToInnerClassFix();
  }

  protected boolean buildQuickFixesOnlyForOnTheFlyErrors() {
    return true;
  }

  public BaseInspectionVisitor buildVisitor() {
    return new AnonymousInnerClassVisitor();
  }

  private static class AnonymousInnerClassVisitor
    extends BaseInspectionVisitor {

    @Override
    public void visitClass(@Nonnull PsiClass aClass) {
      //no call to super here, to avoid double counting
    }

    @Override
    public void visitAnonymousClass(@Nonnull PsiAnonymousClass aClass) {
      super.visitAnonymousClass(aClass);
      if (aClass instanceof PsiEnumConstantInitializer) {
        return;
      }
      registerClassError(aClass);
    }
  }
}