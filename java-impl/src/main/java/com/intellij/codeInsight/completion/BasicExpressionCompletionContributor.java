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
package com.intellij.codeInsight.completion;

import com.intellij.java.analysis.codeInsight.guess.GuessManager;
import com.intellij.codeInsight.lookup.ExpressionLookupItem;
import com.intellij.codeInsight.lookup.KeywordLookupItem;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.VariableLookupItem;
import com.intellij.codeInsight.template.SmartCompletionContextType;
import com.intellij.codeInsight.template.impl.TemplateImpl;
import com.intellij.codeInsight.template.impl.TemplateSettings;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.filters.getters.ClassLiteralGetter;
import com.intellij.psi.filters.getters.ThisGetter;
import com.intellij.psi.scope.BaseScopeProcessor;
import com.intellij.psi.scope.util.PsiScopesUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Consumer;
import com.intellij.util.containers.MultiMap;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;

/**
 * @author peter
 */
public class BasicExpressionCompletionContributor
{

	private static void addKeyword(final Consumer<LookupElement> result, final PsiElement element, final String s)
	{
		result.consume(createKeywordLookupItem(element, s));
	}

	public static LookupElement createKeywordLookupItem(final PsiElement element, final String s)
	{
		return new KeywordLookupItem(JavaPsiFacade.getElementFactory(element.getProject()).createKeyword(s, element), element);
	}

	public static void fillCompletionVariants(JavaSmartCompletionParameters parameters, final Consumer<LookupElement> result, PrefixMatcher matcher)
	{
		final PsiElement element = parameters.getPosition();
		if(JavaKeywordCompletion.isAfterTypeDot(element))
		{
			addKeyword(result, element, PsiKeyword.CLASS);
			addKeyword(result, element, PsiKeyword.THIS);

		}

		if(!JavaKeywordCompletion.AFTER_DOT.accepts(element))
		{
			if(parameters.getParameters().getInvocationCount() <= 1)
			{
				new CollectionsUtilityMethodsProvider(parameters.getPosition(), parameters.getExpectedType(), parameters.getDefaultType(), result).addCompletions(StringUtil.isNotEmpty(matcher
						.getPrefix()));
			}
			ClassLiteralGetter.addCompletions(parameters, result, matcher);

			final PsiElement position = parameters.getPosition();
			final PsiType expectedType = parameters.getExpectedType();

			for(final TemplateImpl template : TemplateSettings.getInstance().getTemplates())
			{
				if(!template.isDeactivated() && template.getTemplateContext().isEnabled(new SmartCompletionContextType()))
				{
					result.consume(new SmartCompletionTemplateItem(template, position));
				}
			}

			addKeyword(result, position, PsiKeyword.TRUE);
			addKeyword(result, position, PsiKeyword.FALSE);

			final PsiElement parent = position.getParent();
			if(parent != null && !(parent.getParent() instanceof PsiSwitchLabelStatement))
			{
				for(final PsiExpression expression : ThisGetter.getThisExpressionVariants(position))
				{
					result.consume(new ExpressionLookupItem(expression));
				}
			}

			processDataflowExpressionTypes(parameters, expectedType, matcher, result);
		}
	}

	static void processDataflowExpressionTypes(JavaSmartCompletionParameters parameters, @Nullable PsiType expectedType, final PrefixMatcher matcher, Consumer<? super LookupElement> consumer)
	{
		final PsiExpression context = PsiTreeUtil.getParentOfType(parameters.getPosition(), PsiExpression.class);
		if(context == null)
		{
			return;
		}

		MultiMap<PsiExpression, PsiType> map = GuessManager.getInstance(context.getProject()).getControlFlowExpressionTypes(context, parameters.getParameters().getInvocationCount() > 1);
		if(map.isEmpty())
		{
			return;
		}

		PsiScopesUtil.treeWalkUp(new BaseScopeProcessor()
		{
			@Override
			public boolean execute(@Nonnull PsiElement element, @Nonnull ResolveState state)
			{
				if(element instanceof PsiLocalVariable)
				{
					if(!matcher.prefixMatches(((PsiLocalVariable) element).getName()))
					{
						return true;
					}

					final PsiExpression expression = ((PsiLocalVariable) element).getInitializer();
					if(expression instanceof PsiTypeCastExpression)
					{
						PsiTypeCastExpression typeCastExpression = (PsiTypeCastExpression) expression;
						final PsiExpression operand = typeCastExpression.getOperand();
						if(operand != null)
						{
							if(map.get(operand).contains(typeCastExpression.getType()))
							{
								map.remove(operand);
							}
						}
					}
				}
				return true;
			}
		}, context, context.getContainingFile());

		for(PsiExpression expression : map.keySet())
		{
			for(PsiType castType : map.get(expression))
			{
				PsiType baseType = expression.getType();
				if(expectedType == null || (expectedType.isAssignableFrom(castType) && (baseType == null || !expectedType.isAssignableFrom(baseType))))
				{
					consumer.consume(CastingLookupElementDecorator.createCastingElement(expressionToLookupElement(expression), castType));
				}
			}
		}
	}

	@Nonnull
	private static LookupElement expressionToLookupElement(@Nonnull PsiExpression expression)
	{
		if(expression instanceof PsiReferenceExpression)
		{
			final PsiReferenceExpression refExpr = (PsiReferenceExpression) expression;
			if(!refExpr.isQualified())
			{
				final PsiElement target = refExpr.resolve();
				if(target instanceof PsiVariable)
				{
					final VariableLookupItem item = new VariableLookupItem((PsiVariable) target);
					item.setSubstitutor(PsiSubstitutor.EMPTY);
					return item;
				}
			}
		}
		if(expression instanceof PsiMethodCallExpression)
		{
			final PsiMethodCallExpression call = (PsiMethodCallExpression) expression;
			if(!call.getMethodExpression().isQualified())
			{
				final PsiMethod method = call.resolveMethod();
				if(method != null)
				{
					return new JavaMethodCallElement(method);
				}
			}
		}

		return new ExpressionLookupItem(expression);
	}

}
