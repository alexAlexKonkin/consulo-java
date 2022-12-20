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
package com.intellij.java.impl.ig.security;

import com.intellij.java.language.psi.PsiNewExpression;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.psiutils.TypeUtils;
import consulo.annotation.component.ExtensionImpl;

import javax.annotation.Nonnull;

@ExtensionImpl
public class ClassLoaderInstantiationInspection extends BaseInspection {

  @Nonnull
  public String getDisplayName() {
    return InspectionGadgetsBundle.message(
      "class.loader.instantiation.display.name");
  }

  @Nonnull
  protected String buildErrorString(Object... infos) {
    return InspectionGadgetsBundle.message(
      "class.loader.instantiation.problem.descriptor");
  }

  public BaseInspectionVisitor buildVisitor() {
    return new ClassLoaderInstantiationVisitor();
  }

  private static class ClassLoaderInstantiationVisitor
    extends BaseInspectionVisitor {

    @Override
    public void visitNewExpression(@Nonnull PsiNewExpression expression) {
      super.visitNewExpression(expression);
      if (!TypeUtils.expressionHasTypeOrSubtype(expression,
                                                "java.lang.ClassLoader")) {
        return;
      }
      registerNewExpressionError(expression);
    }
  }
}