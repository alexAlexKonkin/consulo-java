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
package com.intellij.psi.impl.source.resolve;

import org.jetbrains.annotations.Nullable;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.ConstraintType;
import com.intellij.psi.PsiCallExpression;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameter;

/**
 * @author yole
 */
public class DefaultParameterTypeInferencePolicy extends ParameterTypeInferencePolicy
{
	public static final DefaultParameterTypeInferencePolicy INSTANCE = new DefaultParameterTypeInferencePolicy();

	@Nullable
	@Override
	public Pair<PsiType, ConstraintType> inferTypeConstraintFromCallContext(PsiExpression innerMethodCall, PsiExpressionList parent, PsiCallExpression contextCall, PsiTypeParameter typeParameter)
	{
		return null;
	}

	@Override
	public PsiType getDefaultExpectedType(PsiCallExpression methodCall)
	{
		return PsiType.getJavaLangObject(methodCall.getManager(), methodCall.getResolveScope());
	}

	@Override
	public Pair<PsiType, ConstraintType> getInferredTypeWithNoConstraint(PsiManager manager, PsiType superType)
	{
		return Pair.create(superType, ConstraintType.SUBTYPE);
	}

	@Override
	public PsiType adjustInferredType(PsiManager manager, PsiType guess, ConstraintType constraintType)
	{
		return guess;
	}
}