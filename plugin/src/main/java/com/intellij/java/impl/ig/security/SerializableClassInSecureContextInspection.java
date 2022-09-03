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
package com.intellij.java.impl.ig.security;

import consulo.language.editor.inspection.ui.SingleCheckboxOptionsPanel;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiMethod;
import com.intellij.java.language.psi.PsiTypeParameter;
import com.intellij.java.language.psi.util.InheritanceUtil;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.psiutils.ControlFlowUtils;
import com.intellij.java.impl.ig.psiutils.SerializationUtils;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import javax.swing.*;

public class SerializableClassInSecureContextInspection extends BaseInspection {

  @SuppressWarnings("PublicField")
  public boolean ignoreThrowable = false;

  @Override
  @Nonnull
  public String getDisplayName() {
    return InspectionGadgetsBundle.message("serializable.class.in.secure.context.display.name");
  }

  @Override
  @Nonnull
  protected String buildErrorString(Object... infos) {
    return InspectionGadgetsBundle.message("serializable.class.in.secure.context.problem.descriptor");
  }

  @Nullable
  @Override
  public JComponent createOptionsPanel() {
    return new SingleCheckboxOptionsPanel(
      InspectionGadgetsBundle.message("ignore.classes.extending.throwable.option"), this, "ignoreThrowable");
  }

  @Override
  public BaseInspectionVisitor buildVisitor() {
    return new SerializableClassInSecureContextVisitor();
  }

  private class SerializableClassInSecureContextVisitor extends BaseInspectionVisitor {

    @Override
    public void visitClass(@Nonnull PsiClass aClass) {
      if (aClass.isInterface() || aClass.isAnnotationType() || aClass.isEnum()) {
        return;
      }
      if (aClass instanceof PsiTypeParameter || !SerializationUtils.isSerializable(aClass)) {
        return;
      }
      final PsiMethod[] methods = aClass.findMethodsByName("writeObject", true);
      for (final PsiMethod method : methods) {
        if (!SerializationUtils.isWriteObject(method)) {
          continue;
        }
        if (ControlFlowUtils.methodAlwaysThrowsException((PsiMethod)method.getNavigationElement())) {
          return;
        }
        else {
          break;
        }
      }
      if (ignoreThrowable && InheritanceUtil.isInheritor(aClass, false, "java.lang.Throwable")) {
        return;
      }
      registerClassError(aClass);
    }
  }

  @Override
  public String getAlternativeID() {
    return "serial";
  }
}