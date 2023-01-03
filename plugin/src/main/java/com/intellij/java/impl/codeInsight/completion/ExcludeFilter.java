/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.java.impl.codeInsight.completion;

import com.intellij.java.language.psi.PsiVariable;
import consulo.language.psi.PsiElement;
import consulo.language.psi.filter.ElementFilter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author peter
 */
class ExcludeFilter implements ElementFilter
{
	private final PsiElement myExcluded;

	public ExcludeFilter(@Nonnull PsiVariable excluded)
	{
		myExcluded = excluded;
	}

	@Override
	public boolean isAcceptable(Object element, @Nullable PsiElement context)
	{
		return element != myExcluded;
	}

	@Override
	public boolean isClassAcceptable(Class hintClass)
	{
		return true;
	}
}