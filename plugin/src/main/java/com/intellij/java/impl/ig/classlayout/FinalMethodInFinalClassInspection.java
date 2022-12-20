/*
 * Copyright 2003-2011 Dave Griffith, Bas Leijdekkers
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

import javax.annotation.Nonnull;

import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiMethod;
import com.intellij.java.language.psi.PsiModifier;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import com.intellij.java.impl.ig.fixes.RemoveModifierFix;
import consulo.annotation.component.ExtensionImpl;

@ExtensionImpl
public class FinalMethodInFinalClassInspection extends BaseInspection {

  @Override
  @Nonnull
  public String getDisplayName() {
    return InspectionGadgetsBundle.message(
      "final.method.in.final.class.display.name");
  }

  @Override
  public BaseInspectionVisitor buildVisitor() {
    return new FinalMethodInFinalClassVisitor();
  }

  @Override
  @Nonnull
  protected String buildErrorString(Object... infos) {
    return InspectionGadgetsBundle.message(
      "final.method.in.final.class.problem.descriptor");
  }

  @Override
  public InspectionGadgetsFix buildFix(Object... infos) {
    return new RemoveModifierFix((String)infos[0]);
  }

  private static class FinalMethodInFinalClassVisitor
    extends BaseInspectionVisitor {

    @Override
    public void visitMethod(@Nonnull PsiMethod method) {
      if (!method.hasModifierProperty(PsiModifier.FINAL)) {
        return;
      }
      final PsiClass containingClass = method.getContainingClass();
      if (containingClass == null || containingClass.isEnum()) {
        return;
      }
      if (!containingClass.hasModifierProperty(PsiModifier.FINAL)) {
        return;
      }
      registerModifierError(PsiModifier.FINAL, method, PsiModifier.FINAL);
    }
  }
}