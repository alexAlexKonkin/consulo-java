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
package com.intellij.psi.impl.source.resolve.reference.impl;

import static com.intellij.psi.impl.source.resolve.reference.impl.JavaReflectionReferenceUtil.*;

import gnu.trove.THashSet;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;

import org.jetbrains.annotations.Contract;
import javax.annotation.Nonnull;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.completion.JavaLookupElementBuilder;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.util.MethodSignatureBackedByPsiMethod;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;

/**
 * @author Konstantin Bulenkov
 */
public class JavaLangClassMemberReference extends PsiReferenceBase<PsiLiteralExpression> implements InsertHandler<LookupElement>
{
	private final PsiExpression myContext;

	public JavaLangClassMemberReference(@Nonnull PsiLiteralExpression literal, @Nonnull PsiExpression context)
	{
		super(literal);
		myContext = context;
	}

	@Override
	public PsiElement bindToElement(@Nonnull PsiElement element) throws IncorrectOperationException
	{
		return element;
	}

	@Override
	public PsiElement resolve()
	{
		final Object value = myElement.getValue();
		if(value instanceof String)
		{
			final String name = (String) value;
			final String type = getMemberType(myElement);

			if(type != null)
			{
				final PsiClass psiClass = getPsiClass();
				if(psiClass != null)
				{
					switch(type)
					{

						case GET_FIELD:
						{
							return psiClass.findFieldByName(name, true);
						}

						case GET_DECLARED_FIELD:
						{
							final PsiField field = psiClass.findFieldByName(name, false);
							return isPotentiallyAccessible(field, psiClass) ? field : null;
						}

						case GET_METHOD:
						{
							final PsiMethod[] methods = psiClass.findMethodsByName(name, true);
							final PsiMethod publicMethod = ContainerUtil.find(methods, method -> isRegularMethod(method) && isPublic(method));
							if(publicMethod != null)
							{
								return publicMethod;
							}
							return ContainerUtil.find(methods, method -> isRegularMethod(method));
						}

						case GET_DECLARED_METHOD:
						{
							final PsiMethod[] methods = psiClass.findMethodsByName(name, false);
							return ContainerUtil.find(methods, method -> isRegularMethod(method) && isPotentiallyAccessible(method, psiClass));
						}
					}
				}
			}
		}
		return null;
	}

	@javax.annotation.Nullable
	private PsiClass getPsiClass()
	{
		return getReflectiveClass(myContext);
	}

	@Nonnull
	@Override
	public Object[] getVariants()
	{
		final String type = getMemberType(myElement);
		if(type != null)
		{
			final PsiClass psiClass = getPsiClass();
			if(psiClass != null)
			{
				switch(type)
				{

					case GET_DECLARED_FIELD:
						return Arrays.stream(psiClass.getFields()).filter(field -> field.getName() != null).sorted(Comparator.comparing(PsiField::getName)).map(field -> lookupField(field)).toArray();

					case GET_FIELD:
					{
						final Set<String> uniqueNames = new THashSet<>();
						return Arrays.stream(psiClass.getAllFields()).filter(field -> isPotentiallyAccessible(field, psiClass) && field.getName() != null && uniqueNames.add(field.getName())).sorted
								(Comparator.comparingInt((PsiField field) -> isPublic(field) ? 0 : 1).thenComparing(PsiField::getName)).map(field -> withPriority(lookupField(field), isPublic(field))
						).toArray();
					}

					case GET_DECLARED_METHOD:
						return Arrays.stream(psiClass.getMethods()).filter(method -> isRegularMethod(method)).sorted(Comparator.comparing(PsiMethod::getName)).map(method -> lookupMethod(method))
								.toArray();

					case GET_METHOD:
					{
						return psiClass.getVisibleSignatures().stream().map(MethodSignatureBackedByPsiMethod::getMethod).filter(method -> isRegularMethod(method) && isPotentiallyAccessible(method,
								psiClass)).sorted(Comparator.comparingInt((PsiMethod method) -> getMethodSortOrder(method)).thenComparing(PsiMethod::getName)).map(method -> withPriority(lookupMethod
								(method), -getMethodSortOrder(method))).toArray();
					}
				}
			}
		}
		return EMPTY_ARRAY;
	}


	/**
	 * Non-public members of superclass/superinterface can't be obtained via reflection, they need to be filtered out.
	 */
	@Contract("null, _ -> false")
	private static boolean isPotentiallyAccessible(PsiMember member, PsiClass psiClass)
	{
		return member != null && (member.getContainingClass() == psiClass || isPublic(member));
	}

	@Nonnull
	private LookupElement lookupMethod(PsiMethod method)
	{
		return JavaLookupElementBuilder.forMethod(method, PsiSubstitutor.EMPTY).withInsertHandler(this);
	}

	@Override
	public void handleInsert(InsertionContext context, LookupElement item)
	{
		final Object object = item.getObject();
		if(object instanceof PsiMethod)
		{
			final String text = getParameterTypesText((PsiMethod) object);
			if(text != null)
			{
				replaceText(context, text.isEmpty() ? "" : ", " + text);
			}
		}
	}
}
