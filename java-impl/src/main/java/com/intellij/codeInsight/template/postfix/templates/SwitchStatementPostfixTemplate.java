/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.codeInsight.template.postfix.templates;

import static com.intellij.codeInsight.template.postfix.util.JavaPostfixTemplatesUtils.JAVA_PSI_INFO;

import javax.annotation.Nonnull;
import com.intellij.codeInsight.template.postfix.util.JavaPostfixTemplatesUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Condition;
import com.intellij.pom.java.LanguageLevel;
import consulo.java.module.util.JavaClassNames;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiType;

public class SwitchStatementPostfixTemplate extends JavaStatementWrapPostfixTemplate {
  private static final Condition<PsiElement> SWITCH_TYPE = new Condition<PsiElement>() {
    @Override
    public boolean value(PsiElement expression) {
      if (!(expression instanceof PsiExpression)) {
        return false;
      }

      PsiType type = ((PsiExpression)expression).getType();

      if (type == null) return false;
      if (PsiType.INT.isAssignableFrom(type)) return true;

      if (type instanceof PsiClassType) {
        PsiClass psiClass = ((PsiClassType)type).resolve();
        if (psiClass != null && psiClass.isEnum()) return true;
      }

      if (type.equalsToText(JavaClassNames.JAVA_LANG_STRING)) {
        if (ApplicationManager.getApplication().isUnitTestMode()) return true; // todo: mock jdk 6 and 7
        PsiFile containingFile = expression.getContainingFile();
        if (containingFile instanceof PsiJavaFile) {
          LanguageLevel level = ((PsiJavaFile)containingFile).getLanguageLevel();
          if (level.isAtLeast(LanguageLevel.JDK_1_7)) return true;
        }
      }

      return false;
    }
  };

  public SwitchStatementPostfixTemplate() {
    super("switch", "switch (expr)", JAVA_PSI_INFO, SWITCH_TYPE);
  }

  @Override
  protected void afterExpand(@Nonnull PsiElement newStatement, @Nonnull Editor editor) {
    JavaPostfixTemplatesUtils.formatPsiCodeBlock(newStatement, editor);
  }

  @Nonnull
  @Override
  protected String getHead() {
    return "switch (";
  }

  @Nonnull
  @Override
  protected String getTail() {
    return ") {\nst;\n}";
  }
}
