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
package com.intellij.java.impl.ig.abstraction;

import com.intellij.java.impl.codeInspection.util.SpecialAnnotationsUtil;
import com.intellij.java.impl.ig.fixes.AddToIgnoreIfAnnotatedByListQuickFix;
import com.intellij.java.impl.ig.psiutils.LibraryUtil;
import com.intellij.java.language.codeInsight.AnnotationUtil;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiMethod;
import com.intellij.java.language.psi.PsiModifier;
import com.intellij.java.language.psi.PsiModifierListOwner;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import com.siyeh.ig.psiutils.TestUtils;
import com.siyeh.ig.ui.ExternalizableStringSet;
import consulo.annotation.component.ExtensionImpl;
import consulo.java.language.module.util.JavaClassNames;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;

@ExtensionImpl
public class PublicMethodNotExposedInInterfaceInspection
  extends BaseInspection {

  @SuppressWarnings({"PublicField"})
  public boolean onlyWarnIfContainingClassImplementsAnInterface = false;

  @SuppressWarnings({"PublicField"})
  public final ExternalizableStringSet ignorableAnnotations =
    new ExternalizableStringSet();

  @Override
  @Nonnull
  public String getDisplayName() {
    return InspectionGadgetsBundle.message(
      "public.method.not.in.interface.display.name");
  }

  @Override
  @Nonnull
  protected String buildErrorString(Object... infos) {
    return InspectionGadgetsBundle.message(
      "public.method.not.in.interface.problem.descriptor");
  }

  @jakarta.annotation.Nonnull
  @Override
  protected InspectionGadgetsFix[] buildFixes(Object... infos) {
    return AddToIgnoreIfAnnotatedByListQuickFix.build((PsiModifierListOwner)infos[0], ignorableAnnotations);
  }

  @Override
  public JComponent createOptionsPanel() {
    final JPanel panel = new JPanel(new GridBagLayout());
    final JPanel annotationsListControl = SpecialAnnotationsUtil.createSpecialAnnotationsListControl(
        ignorableAnnotations, InspectionGadgetsBundle.message("ignore.if.annotated.by"));
    final GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = 0;
    constraints.weighty = 1.0;
    constraints.weightx = 1.0;
    constraints.anchor = GridBagConstraints.CENTER;
    constraints.fill = GridBagConstraints.BOTH;
    panel.add(annotationsListControl, constraints);
    final consulo.deadCodeNotWorking.impl.CheckBox checkBox = new consulo.deadCodeNotWorking.impl.CheckBox(InspectionGadgetsBundle.message(
      "public.method.not.in.interface.option"), this, "onlyWarnIfContainingClassImplementsAnInterface");
    constraints.gridy = 1;
    constraints.weighty = 0.0;
    constraints.anchor = GridBagConstraints.WEST;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    panel.add(checkBox, constraints);
    return panel;
  }

  @Override
  public BaseInspectionVisitor buildVisitor() {
    return new PublicMethodNotExposedInInterfaceVisitor();
  }

  private class PublicMethodNotExposedInInterfaceVisitor
    extends BaseInspectionVisitor {

    @Override
    public void visitMethod(@Nonnull PsiMethod method) {
      super.visitMethod(method);
      if (method.isConstructor()) {
        return;
      }
      if (method.getNameIdentifier() == null) {
        return;
      }
      if (!method.hasModifierProperty(PsiModifier.PUBLIC)) {
        return;
      }
      if (method.hasModifierProperty(PsiModifier.STATIC)) {
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
      if (!containingClass.hasModifierProperty(PsiModifier.PUBLIC)) {
        return;
      }
      if (AnnotationUtil.isAnnotated(method, ignorableAnnotations)) {
        return;
      }
      if (onlyWarnIfContainingClassImplementsAnInterface) {
        final PsiClass[] superClasses = containingClass.getSupers();
        boolean implementsInterface = false;
        for (PsiClass superClass : superClasses) {
          if (superClass.isInterface() &&
              !LibraryUtil.classIsInLibrary(superClass)) {
            implementsInterface = true;
            break;
          }
        }
        if (!implementsInterface) {
          return;
        }
      }
      if (exposedInInterface(method)) {
        return;
      }
      if (TestUtils.isJUnitTestMethod(method)) {
        return;
      }
      registerMethodError(method, method);
    }

    private boolean exposedInInterface(PsiMethod method) {
      final PsiMethod[] superMethods = method.findSuperMethods();
      for (final PsiMethod superMethod : superMethods) {
        final PsiClass superClass = superMethod.getContainingClass();
        if (superClass == null) {
          continue;
        }
        if (superClass.isInterface()) {
          return true;
        }
        final String superclassName = superClass.getQualifiedName();
        if (JavaClassNames.JAVA_LANG_OBJECT.equals(superclassName)) {
          return true;
        }
        if (exposedInInterface(superMethod)) {
          return true;
        }
      }
      return false;
    }
  }
}