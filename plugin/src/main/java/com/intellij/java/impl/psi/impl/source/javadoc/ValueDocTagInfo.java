/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.java.impl.psi.impl.source.javadoc;

import com.intellij.java.language.impl.codeInsight.daemon.JavaErrorBundle;
import consulo.language.psi.PsiElement;
import com.intellij.java.language.psi.PsiField;
import com.intellij.java.language.psi.PsiModifier;
import consulo.language.psi.PsiReference;
import com.intellij.java.language.impl.psi.impl.JavaConstantExpressionEvaluator;
import com.intellij.java.language.psi.javadoc.JavadocTagInfo;
import com.intellij.java.language.psi.javadoc.PsiDocTagValue;
import com.intellij.java.language.psi.util.PsiUtil;

/**
 * @author yole
 */
public class ValueDocTagInfo implements JavadocTagInfo {
  @Override
  public String getName() {
    return "value";
  }

  @Override
  public boolean isInline() {
    return true;
  }

  @Override
  public boolean isValidInContext(PsiElement element) {
    return true;
  }

  @Override
  public String checkTagValue(PsiDocTagValue value) {
    boolean hasReference = (value != null && value.getFirstChild() != null);
    if (hasReference) {
      if (!PsiUtil.isLanguageLevel5OrHigher(value)) {
        return JavaErrorBundle.message("javadoc.value.tag.jdk15.required");
      }
    }

    if (value != null) {
      PsiReference reference = value.getReference();
      if (reference != null) {
        PsiElement target = reference.resolve();
        if (target != null) {
          if (!(target instanceof PsiField)) {
            return JavaErrorBundle.message("javadoc.value.field.required");
          }
          PsiField field = (PsiField) target;
          if (!field.hasModifierProperty(PsiModifier.STATIC)) {
            return JavaErrorBundle.message("javadoc.value.static.field.required");
          }
          if (field.getInitializer() == null ||
              JavaConstantExpressionEvaluator.computeConstantExpression(field.getInitializer(), false) == null) {
            return JavaErrorBundle.message("javadoc.value.field.with.initializer.required");
          }
        }
      }
    }

    return null;
  }

  @Override
  public PsiReference getReference(PsiDocTagValue value) {
    return null;
  }
}
