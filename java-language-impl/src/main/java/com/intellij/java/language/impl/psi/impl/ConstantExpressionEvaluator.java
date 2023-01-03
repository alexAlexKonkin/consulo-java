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
package com.intellij.java.language.impl.psi.impl;

import com.intellij.java.language.psi.PsiConstantEvaluationHelper;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.Language;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToOne;
import consulo.language.psi.PsiElement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Serega.Vasiliev
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ConstantExpressionEvaluator extends LanguageExtension {
  ExtensionPointCacheKey<ConstantExpressionEvaluator, ByLanguageValue<ConstantExpressionEvaluator>> KEY = ExtensionPointCacheKey.create("ConstantExpressionEvaluator", LanguageOneToOne.build());

  @Nullable
  static ConstantExpressionEvaluator forLanguage(@Nonnull Language language) {
    return Application.get().getExtensionPoint(ConstantExpressionEvaluator.class).getOrBuildCache(KEY).get(language);
  }

  Object computeConstantExpression(PsiElement expression, boolean throwExceptionOnOverflow);

  Object computeExpression(PsiElement expression,
                           boolean throwExceptionOnOverflow,
                           @Nullable PsiConstantEvaluationHelper.AuxEvaluator auxEvaluator);
}
