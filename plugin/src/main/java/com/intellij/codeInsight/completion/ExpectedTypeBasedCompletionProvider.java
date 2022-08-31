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
package com.intellij.codeInsight.completion;

import java.util.Collection;

import javax.annotation.Nonnull;

import com.intellij.codeInsight.ExpectedTypeInfo;
import com.intellij.psi.PsiElement;
import com.intellij.java.language.psi.PsiLiteralExpression;
import com.intellij.util.ProcessingContext;
import com.intellij.util.containers.ContainerUtil;
import consulo.codeInsight.completion.CompletionProvider;

/**
 * @author peter
*/
public abstract class ExpectedTypeBasedCompletionProvider implements CompletionProvider
{

  @Override
  public void addCompletions(@Nonnull final CompletionParameters params, final ProcessingContext matchingContext, @Nonnull final CompletionResultSet result) {
    final PsiElement position = params.getPosition();
    if (position.getParent() instanceof PsiLiteralExpression) return;

    addCompletions(params, result, ContainerUtil.newHashSet(JavaSmartCompletionContributor.getExpectedTypes(params)));
  }

  protected abstract void addCompletions(CompletionParameters params, CompletionResultSet result, Collection<ExpectedTypeInfo> infos);
}
