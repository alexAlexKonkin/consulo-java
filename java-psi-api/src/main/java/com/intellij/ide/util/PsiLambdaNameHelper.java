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
package com.intellij.ide.util;

import gnu.trove.THashMap;

import java.util.Map;

import javax.annotation.Nonnull;

import com.intellij.openapi.util.Key;
import com.intellij.psi.JavaRecursiveElementWalkingVisitor;
import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiLambdaExpression;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.ParameterizedCachedValue;
import com.intellij.psi.util.ParameterizedCachedValueProvider;
import com.intellij.psi.util.PsiTreeUtil;

public class PsiLambdaNameHelper
{
	private static final Key<ParameterizedCachedValue<Map<PsiLambdaExpression, String>, PsiClass>> LAMBDA_NAME = Key.create("ANONYMOUS_CLASS_NAME");

	@javax.annotation.Nullable
	public static String getVMName(@Nonnull PsiLambdaExpression lambdaExpression)
	{
		final PsiClass upper = PsiTreeUtil.getParentOfType(lambdaExpression, PsiClass.class);
		if(upper == null)
		{
			return null;
		}
		ParameterizedCachedValue<Map<PsiLambdaExpression, String>, PsiClass> value = upper.getUserData(LAMBDA_NAME);
		if(value == null)
		{
			value = CachedValuesManager.getManager(upper.getProject()).createParameterizedCachedValue(new ParameterizedCachedValueProvider<Map<PsiLambdaExpression, String>, PsiClass>()
			{
				@Override
				public CachedValueProvider.Result<Map<PsiLambdaExpression, String>> compute(final PsiClass upper)
				{
					final Map<PsiLambdaExpression, String> map = new THashMap<PsiLambdaExpression, String>();
					upper.accept(new JavaRecursiveElementWalkingVisitor()
					{
						int index;

						@Override
						public void visitLambdaExpression(PsiLambdaExpression expression)
						{
							map.put(expression, "$" + index++);
							super.visitLambdaExpression(expression);
						}

						@Override
						public void visitClass(PsiClass aClass)
						{
							if(aClass == upper)
							{
								super.visitClass(aClass);
							}
						}
					});
					return CachedValueProvider.Result.create(map, upper);
				}
			}, false);
			upper.putUserData(LAMBDA_NAME, value);
		}
		return "lambda" + getLambdaPrefix(lambdaExpression) + value.getValue(upper).get(lambdaExpression);
	}

	public static String getLambdaPrefix(@Nonnull PsiLambdaExpression lambdaExpression)
	{
		PsiMember member = PsiTreeUtil.getParentOfType(lambdaExpression, PsiMethod.class, PsiClass.class, PsiField.class);
		final String methodPrefix;
		if(member instanceof PsiMethod)
		{
			methodPrefix = member.getContainingClass() instanceof PsiAnonymousClass ? "" : "$" + member.getName();
		}
		else if(member instanceof PsiField && member.getContainingClass() instanceof PsiAnonymousClass)
		{
			methodPrefix = "";
		}
		else
		{
			//inside class initializer everywhere or field in a named class
			methodPrefix = "$new";
		}
		return methodPrefix;
	}
}
