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
package com.intellij.java.impl.ig.methodmetrics;

import jakarta.annotation.Nonnull;

import com.intellij.java.language.psi.PsiMethod;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.BaseInspectionVisitor;
import consulo.annotation.component.ExtensionImpl;

@ExtensionImpl
public class NestingDepthInspection extends MethodMetricInspection {

  @Nonnull
  public String getID() {
    return "OverlyNestedMethod";
  }

  @Nonnull
  public String getDisplayName() {
    return InspectionGadgetsBundle.message("nesting.depth.display.name");
  }

  protected int getDefaultLimit() {
    return 5;
  }

  protected String getConfigurationLabel() {
    return InspectionGadgetsBundle.message("nesting.depth.limit.option");
  }

  @Nonnull
  public String buildErrorString(Object... infos) {
    final Integer nestingDepth = (Integer)infos[0];
    return InspectionGadgetsBundle.message(
      "nesting.depth.problem.descriptor", nestingDepth);
  }

  public BaseInspectionVisitor buildVisitor() {
    return new NestingDepthMethodVisitor();
  }

  private class NestingDepthMethodVisitor extends BaseInspectionVisitor {

    @Override
    public void visitMethod(@Nonnull PsiMethod method) {
      // note: no call to super
      if (method.getNameIdentifier() == null) {
        return;
      }
      final NestingDepthVisitor visitor = new NestingDepthVisitor();
      method.accept(visitor);
      final int count = visitor.getMaximumDepth();
      if (count <= getLimit()) {
        return;
      }
      registerMethodError(method, Integer.valueOf(count));
    }
  }
}
