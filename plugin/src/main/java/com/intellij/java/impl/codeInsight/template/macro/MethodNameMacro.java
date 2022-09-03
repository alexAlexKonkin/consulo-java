/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.java.impl.codeInsight.template.macro;

import javax.annotation.Nonnull;

import consulo.language.editor.CodeInsightBundle;
import com.intellij.codeInsight.template.*;
import com.intellij.java.impl.codeInsight.template.JavaCodeContextType;
import consulo.language.LangBundle;
import com.intellij.java.language.psi.PsiClassInitializer;
import consulo.language.psi.PsiElement;
import com.intellij.java.language.psi.PsiMethod;
import com.intellij.java.language.psi.PsiModifier;

public class MethodNameMacro extends Macro {

  @Override
  public String getName() {
    return "methodName";
  }

  @Override
  public String getPresentableName() {
    return CodeInsightBundle.message("macro.methodname");
  }

  @Override
  @Nonnull
  public String getDefaultValue() {
    return "a";
  }

  @Override
  public Result calculateResult(@Nonnull Expression[] params, final ExpressionContext context) {
    PsiElement place = context.getPsiElementAtStartOffset();
    while(place != null){
      if (place instanceof PsiMethod){
        return new TextResult(((PsiMethod)place).getName());
      } else if (place instanceof PsiClassInitializer) {
        return ((PsiClassInitializer) place).hasModifierProperty(PsiModifier.STATIC) ?
               new TextResult(LangBundle.message("java.terms.static.initializer")) :
               new TextResult(LangBundle.message("java.terms.instance.initializer"));
      }
      place = place.getParent();
    }
    return null;
  }

  @Override
  public boolean isAcceptableInContext(TemplateContextType context) {
    return context instanceof JavaCodeContextType;
  }

}
