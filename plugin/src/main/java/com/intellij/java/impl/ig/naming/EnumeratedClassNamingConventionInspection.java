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
package com.intellij.java.impl.ig.naming;

import javax.annotation.Nonnull;

import com.intellij.java.language.psi.PsiClass;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import com.intellij.java.impl.ig.fixes.RenameFix;
import consulo.annotation.component.ExtensionImpl;

@ExtensionImpl
public class EnumeratedClassNamingConventionInspection
  extends ConventionInspection {

  private static final int DEFAULT_MIN_LENGTH = 8;
  private static final int DEFAULT_MAX_LENGTH = 64;

  @Nonnull
  public String getDisplayName() {
    return InspectionGadgetsBundle.message(
      "enumerated.class.naming.convention.display.name");
  }

  protected InspectionGadgetsFix buildFix(Object... infos) {
    return new RenameFix();
  }

  protected boolean buildQuickFixesOnlyForOnTheFlyErrors() {
    return true;
  }

  @Nonnull
  public String buildErrorString(Object... infos) {
    final String className = (String)infos[0];
    if (className.length() < getMinLength()) {
      return InspectionGadgetsBundle.message(
        "enumerated.class.naming.convention.problem.descriptor.short");
    }
    else if (className.length() > getMaxLength()) {
      return InspectionGadgetsBundle.message(
        "enumerated.class.naming.convention.problem.descriptor.long");
    }
    return InspectionGadgetsBundle.message(
      "enumerated.class.naming.convention.problem.descriptor.regex.mismatch",
      getRegex());
  }

  protected String getDefaultRegex() {
    return "[A-Z][A-Za-z\\d]*";
  }

  protected int getDefaultMinLength() {
    return DEFAULT_MIN_LENGTH;
  }

  protected int getDefaultMaxLength() {
    return DEFAULT_MAX_LENGTH;
  }

  public BaseInspectionVisitor buildVisitor() {
    return new NamingConventionsVisitor();
  }

  private class NamingConventionsVisitor extends BaseInspectionVisitor {

    @Override
    public void visitClass(@Nonnull PsiClass aClass) {
      if (!aClass.isEnum()) {
        return;
      }
      final String name = aClass.getName();
      if (name == null) {
        return;
      }
      if (isValid(name)) {
        return;
      }
      registerClassError(aClass, name);
    }
  }
}