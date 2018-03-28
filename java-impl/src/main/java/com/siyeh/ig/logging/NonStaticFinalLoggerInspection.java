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
package com.siyeh.ig.logging;

import com.intellij.codeInspection.ui.ListTable;
import com.intellij.codeInspection.ui.ListWrappingTableModel;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.psi.*;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import com.siyeh.ig.fixes.MakeFieldStaticFinalFix;
import com.siyeh.ig.ui.UiUtils;
import org.jdom.Element;
import javax.annotation.Nonnull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class NonStaticFinalLoggerInspection extends BaseInspection {

  @SuppressWarnings("PublicField")
  public String loggerClassName = "java.util.logging.Logger" + ',' +
                                  "org.slf4j.Logger" + ',' +
                                  "org.apache.commons.logging.Log" + ',' +
                                  "org.apache.log4j.Logger";

  private final List<String> loggerClassNames = new ArrayList();

  @Override
  @Nonnull
  public String getID() {
    return "NonConstantLogger";
  }

  @Override
  @Nonnull
  public String getDisplayName() {
    return InspectionGadgetsBundle.message("non.constant.logger.display.name");
  }

  @Override
  @Nonnull
  protected String buildErrorString(Object... infos) {
    return InspectionGadgetsBundle.message("non.constant.logger.problem.descriptor");
  }

  @Override
  protected InspectionGadgetsFix buildFix(Object... infos) {
    final PsiField field = (PsiField)infos[0];
    return MakeFieldStaticFinalFix.buildFixUnconditional(field);
  }

  @Override
  public void readSettings(@Nonnull Element element) throws InvalidDataException {
    super.readSettings(element);
    parseString(loggerClassName, loggerClassNames);
  }

  @Override
  public void writeSettings(@Nonnull Element element) throws WriteExternalException {
    loggerClassName = formatString(loggerClassNames);
    super.writeSettings(element);
  }

  @Override
  public JComponent createOptionsPanel() {
    final ListTable table = new ListTable(
      new ListWrappingTableModel(loggerClassNames, InspectionGadgetsBundle.message("logger.class.name")));
    return UiUtils.createAddRemoveTreeClassChooserPanel(table, InspectionGadgetsBundle.message("choose.logger.class"));
  }

  @Override
  public BaseInspectionVisitor buildVisitor() {
    return new NonStaticFinalLoggerVisitor();
  }

  private class NonStaticFinalLoggerVisitor extends BaseInspectionVisitor {

    @Override
    public void visitClass(@Nonnull PsiClass aClass) {
      if (aClass.isInterface() || aClass.isEnum() || aClass.isAnnotationType()) {
        return;
      }
      if (aClass instanceof PsiTypeParameter) {
        return;
      }
      if (aClass.getContainingClass() != null) {
        return;
      }
      final PsiField[] fields = aClass.getFields();
      for (final PsiField field : fields) {
        if (!isLogger(field)) {
          continue;
        }
        if (field.hasModifierProperty(PsiModifier.STATIC) && field.hasModifierProperty(PsiModifier.FINAL)) {
          continue;
        }
        registerFieldError(field, field);
      }
    }

    private boolean isLogger(PsiVariable variable) {
      final PsiType type = variable.getType();
      final String text = type.getCanonicalText();
      return loggerClassNames.contains(text);
    }
  }
}