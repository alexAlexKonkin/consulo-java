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
package com.intellij.java.impl.ig.encapsulation;

import com.intellij.java.impl.codeInspection.util.SpecialAnnotationsUtil;
import com.intellij.java.impl.ig.fixes.AddToIgnoreIfAnnotatedByListQuickFix;
import com.intellij.java.impl.ig.fixes.EncapsulateVariableFix;
import com.intellij.java.language.codeInsight.AnnotationUtil;
import com.intellij.java.language.psi.*;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import com.siyeh.ig.psiutils.ClassUtils;
import com.siyeh.ig.ui.ExternalizableStringSet;
import consulo.annotation.component.ExtensionImpl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@ExtensionImpl
public class PublicFieldInspection extends BaseInspection {

  @SuppressWarnings({"PublicField"})
  public boolean ignoreEnums = false;

  @SuppressWarnings({"PublicField"})
  public final ExternalizableStringSet ignorableAnnotations = new ExternalizableStringSet();

  @Override
  @Nonnull
  public String getDisplayName() {
    return InspectionGadgetsBundle.message("public.field.display.name");
  }

  @Override
  @Nonnull
  public String buildErrorString(Object... infos) {
    return InspectionGadgetsBundle.message(
      "public.field.problem.descriptor");
  }

  @Override
  @Nullable
  public JComponent createOptionsPanel() {
    final JPanel panel = new JPanel(new BorderLayout());
    final JPanel annotationsListControl = SpecialAnnotationsUtil.createSpecialAnnotationsListControl(
      ignorableAnnotations, InspectionGadgetsBundle.message("ignore.if.annotated.by"));
    panel.add(annotationsListControl, BorderLayout.CENTER);
    final consulo.language.editor.inspection.ui.CheckBox checkBox = new consulo.language.editor.inspection.ui.CheckBox(InspectionGadgetsBundle.message(
      "public.field.ignore.enum.type.fields.option"), this, "ignoreEnums");
    panel.add(checkBox, BorderLayout.SOUTH);
    return panel;
  }

  @Nonnull
  @Override
  protected InspectionGadgetsFix[] buildFixes(Object... infos) {
    final List<InspectionGadgetsFix> fixes = new ArrayList();
    final PsiField field = (PsiField)infos[0];
    fixes.add(new EncapsulateVariableFix(field.getName()));
    AddToIgnoreIfAnnotatedByListQuickFix.build(field, ignorableAnnotations, fixes);
    return fixes.toArray(new InspectionGadgetsFix[fixes.size()]);
  }

  @Override
  protected boolean buildQuickFixesOnlyForOnTheFlyErrors() {
    return true;
  }

  @Override
  public BaseInspectionVisitor buildVisitor() {
    return new PublicFieldVisitor();
  }

  private class PublicFieldVisitor extends BaseInspectionVisitor {

    @Override
    public void visitField(@Nonnull PsiField field) {
      if (!field.hasModifierProperty(PsiModifier.PUBLIC)) {
        return;
      }
      if (AnnotationUtil.isAnnotated(field, ignorableAnnotations)) {
        return;
      }
      if (field.hasModifierProperty(PsiModifier.FINAL)) {
        if (field.hasModifierProperty(PsiModifier.STATIC)) {
          return;
        }
        final PsiType type = field.getType();
        if (ClassUtils.isImmutable(type)) {
          return;
        }
        if (ignoreEnums) {
          if (type instanceof PsiClassType) {
            final PsiClassType classType = (PsiClassType)type;
            final PsiClass aClass = classType.resolve();
            if (aClass != null && aClass.isEnum()) {
              return;
            }
          }
        }
      }
      registerFieldError(field, field);
    }
  }
}