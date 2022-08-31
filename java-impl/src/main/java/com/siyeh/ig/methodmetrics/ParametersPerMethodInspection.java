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
package com.siyeh.ig.methodmetrics;

import javax.annotation.Nonnull;

import com.intellij.java.language.psi.PsiMethod;
import com.intellij.java.language.psi.PsiParameterList;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.psiutils.LibraryUtil;

public class ParametersPerMethodInspection extends MethodMetricInspection {

  @Nonnull
  public String getID() {
    return "MethodWithTooManyParameters";
  }

  @Nonnull
  public String getDisplayName() {
    return InspectionGadgetsBundle.message(
      "parameters.per.method.display.name");
  }

  @Nonnull
  public String buildErrorString(Object... infos) {
    final Integer parameterCount = (Integer)infos[0];
    return InspectionGadgetsBundle.message(
      "parameters.per.method.problem.descriptor", parameterCount);
  }

  protected int getDefaultLimit() {
    return 5;
  }

  protected String getConfigurationLabel() {
    return InspectionGadgetsBundle.message("parameter.limit.option");
  }

  public BaseInspectionVisitor buildVisitor() {
    return new ParametersPerMethodVisitor();
  }

  private class ParametersPerMethodVisitor extends BaseInspectionVisitor {

    @Override
    public void visitMethod(@Nonnull PsiMethod method) {
      // note: no call to super
      if (method.getNameIdentifier() == null) {
        return;
      }
      if (method.isConstructor()) {
        return;
      }
      final PsiParameterList parameterList = method.getParameterList();
      final int parametersCount = parameterList.getParametersCount();
      if (parametersCount <= getLimit()) {
        return;
      }
      if (LibraryUtil.isOverrideOfLibraryMethod(method)) {
        return;
      }
      registerMethodError(method, Integer.valueOf(parametersCount));
    }
  }
}