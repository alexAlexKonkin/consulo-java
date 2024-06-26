/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.java.impl.refactoring.typeMigration;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import com.intellij.java.language.psi.PsiExpression;
import com.intellij.java.language.psi.PsiType;
import com.intellij.java.impl.refactoring.typeMigration.usageInfo.TypeMigrationUsageInfo;
import consulo.language.util.IncorrectOperationException;

public class TypeConversionDescriptorBase
{
	private TypeMigrationUsageInfo myRoot;

	public TypeConversionDescriptorBase()
	{
	}

	public TypeMigrationUsageInfo getRoot()
	{
		return myRoot;
	}

	public void setRoot(final TypeMigrationUsageInfo root)
	{
		myRoot = root;
	}

	/**
	 * @return converted expression type or null if not known
	 */
	@Nullable
	public PsiType conversionType()
	{
		return null;
	}

	public PsiExpression replace(PsiExpression expression, @Nonnull TypeEvaluator evaluator) throws IncorrectOperationException
	{
		return expression;
	}

	@Override
	public String toString()
	{
		return "$";
	}
}