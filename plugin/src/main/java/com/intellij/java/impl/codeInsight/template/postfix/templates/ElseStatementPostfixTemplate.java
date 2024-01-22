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
package com.intellij.java.impl.codeInsight.template.postfix.templates;

import consulo.language.editor.postfixTemplate.ElseExpressionPostfixTemplateBase;
import com.intellij.java.impl.codeInsight.generation.surroundWith.JavaWithIfExpressionSurrounder;
import consulo.language.editor.surroundWith.Surrounder;

import jakarta.annotation.Nonnull;

import static com.intellij.java.impl.codeInsight.template.postfix.util.JavaPostfixTemplatesUtils.IS_BOOLEAN;
import static com.intellij.java.impl.codeInsight.template.postfix.util.JavaPostfixTemplatesUtils.JAVA_PSI_INFO;

public class ElseStatementPostfixTemplate extends ElseExpressionPostfixTemplateBase {
  public ElseStatementPostfixTemplate() {
    super(JAVA_PSI_INFO, IS_BOOLEAN);
  }

  @Nonnull
  @Override
  protected Surrounder getSurrounder() {
    return new JavaWithIfExpressionSurrounder();
  }
}
