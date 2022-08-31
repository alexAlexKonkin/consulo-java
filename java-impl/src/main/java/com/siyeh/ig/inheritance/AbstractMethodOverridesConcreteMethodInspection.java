/*
 * Copyright 2003-2010 Dave Griffith, Bas Leijdekkers
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
package com.siyeh.ig.inheritance;

import javax.annotation.Nonnull;

import consulo.java.language.module.util.JavaClassNames;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiMethod;
import com.intellij.java.language.psi.PsiModifier;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;

public class AbstractMethodOverridesConcreteMethodInspection
  extends BaseInspection {

  @Override
  @Nonnull
  public String getDisplayName() {
    return InspectionGadgetsBundle.message(
      "abstract.method.overrides.concrete.method.display.name");
  }

  @Override
  @Nonnull
  protected String buildErrorString(Object... infos) {
    return InspectionGadgetsBundle.message(
      "abstract.method.overrides.concrete.method.problem.descriptor");
  }

  @Override
  public BaseInspectionVisitor buildVisitor() {
    return new AbstractMethodOverridesConcreteMethodVisitor();
  }

  private static class AbstractMethodOverridesConcreteMethodVisitor
    extends BaseInspectionVisitor {

    @Override
    public void visitMethod(@Nonnull PsiMethod method) {
      //no call to super, so we don't drill into anonymous classes
      if (method.isConstructor()) {
        return;
      }
      final PsiClass containingClass = method.getContainingClass();
      if (containingClass == null) {
        return;
      }
      if (containingClass.isInterface() ||
          containingClass.isAnnotationType()) {
        return;
      }
      if (!method.hasModifierProperty(PsiModifier.ABSTRACT)) {
        return;
      }
      final PsiMethod[] superMethods = method.findSuperMethods();
      for (final PsiMethod superMethod : superMethods) {
        final PsiClass superClass = superMethod.getContainingClass();
        if (superClass == null) {
          continue;
        }
        final String superClassName = superClass.getQualifiedName();
        if (!superClass.isInterface() &&
            !JavaClassNames.JAVA_LANG_OBJECT.equals(superClassName) &&
            !superMethod.hasModifierProperty(PsiModifier.ABSTRACT)) {
          registerMethodError(method);
          return;
        }
      }
    }
  }
}
