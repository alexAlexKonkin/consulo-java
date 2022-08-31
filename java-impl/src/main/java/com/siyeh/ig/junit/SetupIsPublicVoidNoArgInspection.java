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
package com.siyeh.ig.junit;

import javax.annotation.Nonnull;

import com.intellij.java.language.psi.*;
import com.intellij.java.language.psi.util.InheritanceUtil;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import org.jetbrains.annotations.NonNls;

public class SetupIsPublicVoidNoArgInspection extends BaseInspection {

  @Override
  @Nonnull
  public String getID() {
    return "SetUpWithIncorrectSignature";
  }

  @Override
  @Nonnull
  public String getDisplayName() {
    return InspectionGadgetsBundle.message(
      "setup.is.public.void.no.arg.display.name");
  }

  @Override
  @Nonnull
  protected String buildErrorString(Object... infos) {
    return InspectionGadgetsBundle.message(
      "setup.is.public.void.no.arg.problem.descriptor");
  }

  @Override
  public BaseInspectionVisitor buildVisitor() {
    return new SetupIsPublicVoidNoArgVisitor();
  }

  private static class SetupIsPublicVoidNoArgVisitor
    extends BaseInspectionVisitor {

    @Override
    public void visitMethod(@Nonnull PsiMethod method) {
      //note: no call to super;
      @NonNls final String methodName = method.getName();
      if (!"setUp".equals(methodName)) {
        return;
      }
      final PsiType returnType = method.getReturnType();
      if (returnType == null) {
        return;
      }
      final PsiClass targetClass = method.getContainingClass();
      if (targetClass == null) {
        return;
      }
      if (!InheritanceUtil.isInheritor(targetClass,
                                       "junit.framework.TestCase")) {
        return;
      }
      final PsiParameterList parameterList = method.getParameterList();
      if (parameterList.getParametersCount() != 0 ||
          !returnType.equals(PsiType.VOID) ||
          !method.hasModifierProperty(PsiModifier.PUBLIC) &&
          !method.hasModifierProperty(PsiModifier.PROTECTED)) {
        registerMethodError(method);
      }
    }
  }
}