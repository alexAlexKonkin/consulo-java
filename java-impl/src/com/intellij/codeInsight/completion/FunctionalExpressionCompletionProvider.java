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
package com.intellij.codeInsight.completion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.codeInsight.ExpectedTypeInfo;
import com.intellij.codeInsight.generation.GenerateMembersUtil;
import com.intellij.codeInsight.lookup.AutoCompletionPolicy;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.impl.source.resolve.JavaResolveUtil;
import com.intellij.psi.impl.source.resolve.graphInference.FunctionalInterfaceParameterizationUtil;
import com.intellij.psi.impl.source.tree.java.MethodReferenceResolver;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.util.TypeConversionUtil;
import com.intellij.util.Consumer;
import com.intellij.util.ObjectUtils;
import com.intellij.util.ProcessingContext;
import com.intellij.util.containers.JBIterable;
import consulo.annotations.RequiredReadAction;
import consulo.codeInsight.completion.CompletionProvider;

/**
 * User: anna
 */
public class FunctionalExpressionCompletionProvider implements CompletionProvider
{

	private static final InsertHandler<LookupElement> CONSTRUCTOR_REF_INSERT_HANDLER = (context, item) ->
	{
		int start = context.getStartOffset();
		PsiClass psiClass = PsiUtil.resolveClassInType((PsiType) item.getObject());
		if(psiClass != null)
		{
			String insertedName = StringUtil.trimEnd(item.getLookupString(), "::new");
			while(insertedName.endsWith("[]"))
			{
				insertedName = insertedName.substring(0, insertedName.length() - 2);
			}
			JavaCompletionUtil.insertClassReference(psiClass, context.getFile(), start, start + insertedName.length());
		}
	};

	private static boolean isLambdaContext(@NotNull PsiElement element)
	{
		final PsiElement rulezzRef = element.getParent();
		return rulezzRef != null && rulezzRef instanceof PsiReferenceExpression && ((PsiReferenceExpression) rulezzRef).getQualifier() == null && LambdaUtil.isValidLambdaContext(rulezzRef.getParent
				());
	}

	@RequiredReadAction
	@Override
	public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet result)
	{
		addFunctionalVariants(parameters, true, true, result.getPrefixMatcher(), result);
	}

	static void addFunctionalVariants(@NotNull CompletionParameters parameters, boolean smart, boolean addInheritors, PrefixMatcher matcher, Consumer<LookupElement> result)
	{
		if(!PsiUtil.isLanguageLevel8OrHigher(parameters.getOriginalFile()) || !isLambdaContext(parameters.getPosition()))
		{
			return;
		}

		ExpectedTypeInfo[] expectedTypes = JavaSmartCompletionContributor.getExpectedTypes(parameters);
		for(ExpectedTypeInfo expectedType : expectedTypes)
		{
			final PsiType defaultType = expectedType.getDefaultType();
			if(LambdaUtil.isFunctionalType(defaultType))
			{
				final PsiType functionalInterfaceType = FunctionalInterfaceParameterizationUtil.getGroundTargetType(defaultType);
				final PsiMethod functionalInterfaceMethod = LambdaUtil.getFunctionalInterfaceMethod(functionalInterfaceType);
				if(functionalInterfaceMethod != null)
				{
					PsiParameter[] params = PsiParameter.EMPTY_ARRAY;
					final PsiElement originalPosition = parameters.getPosition();
					final PsiSubstitutor substitutor = LambdaUtil.getSubstitutor(functionalInterfaceMethod, PsiUtil.resolveGenericsClassInType(functionalInterfaceType));
					if(!functionalInterfaceMethod.hasTypeParameters())
					{
						params = functionalInterfaceMethod.getParameterList().getParameters();
						final Project project = functionalInterfaceMethod.getProject();
						final JVMElementFactory jvmElementFactory = JVMElementFactories.getFactory(originalPosition.getLanguage(), project);
						final JavaCodeStyleManager javaCodeStyleManager = JavaCodeStyleManager.getInstance(project);
						if(jvmElementFactory != null)
						{
							params = GenerateMembersUtil.overriddenParameters(params, jvmElementFactory, javaCodeStyleManager, substitutor, originalPosition);
						}

						String paramsString = params.length == 1 ? getParamName(params[0], originalPosition) : "(" + StringUtil.join(params, parameter -> getParamName(parameter, originalPosition),
								",") + ")";

						final CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);
						PsiLambdaExpression lambdaExpression = (PsiLambdaExpression) JavaPsiFacade.getElementFactory(project).createExpressionFromText(paramsString + " -> {}", null);
						lambdaExpression = (PsiLambdaExpression) codeStyleManager.reformat(lambdaExpression);
						paramsString = lambdaExpression.getParameterList().getText();
						final LookupElementBuilder builder = LookupElementBuilder.create(functionalInterfaceMethod, paramsString + " -> ").withPresentableText(paramsString + " -> {}").withTypeText
								(functionalInterfaceType.getPresentableText()).withIcon(AllIcons.Nodes.Function);
						LookupElement lambdaElement = builder.withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE);
						result.consume(smart ? lambdaElement : PrioritizedLookupElement.withPriority(lambdaElement, 1));
					}

					addMethodReferenceVariants(smart, addInheritors, parameters, matcher, functionalInterfaceType, functionalInterfaceMethod, params, originalPosition, substitutor, element -> result
							.consume(smart ? JavaSmartCompletionContributor.decorate(element, Arrays.asList(expectedTypes)) : element));
				}
			}
		}
	}

	private static void addMethodReferenceVariants(boolean smart,
			boolean addInheritors,
			CompletionParameters parameters,
			PrefixMatcher matcher,
			PsiType functionalInterfaceType,
			PsiMethod functionalInterfaceMethod,
			PsiParameter[] params,
			PsiElement originalPosition,
			PsiSubstitutor substitutor,
			Consumer<LookupElement> result)
	{
		final PsiType expectedReturnType = substitutor.substitute(functionalInterfaceMethod.getReturnType());
		if(expectedReturnType == null)
		{
			return;
		}

		if(params.length > 0)
		{
			for(LookupElement element : collectVariantsByReceiver(!smart, functionalInterfaceType, params, originalPosition, substitutor, expectedReturnType))
			{
				result.consume(element);
			}
		}
		for(LookupElement element : collectThisVariants(functionalInterfaceType, params, originalPosition, substitutor, expectedReturnType))
		{
			result.consume(element);
		}

		for(LookupElement element : collectStaticVariants(functionalInterfaceType, params, originalPosition, substitutor, expectedReturnType))
		{
			result.consume(element);
		}

		Consumer<PsiType> consumer = eachReturnType ->
		{
			PsiClass psiClass = PsiUtil.resolveClassInType(eachReturnType);
			if(psiClass == null || !MethodReferenceResolver.canBeConstructed(psiClass))
			{
				return;
			}

			if(eachReturnType.getArrayDimensions() == 0)
			{
				PsiMethod[] constructors = psiClass.getConstructors();
				for(PsiMethod psiMethod : constructors)
				{
					if(areParameterTypesAppropriate(psiMethod, params, substitutor, 0))
					{
						result.consume(createConstructorReferenceLookup(functionalInterfaceType, eachReturnType));
					}
				}
				if(constructors.length == 0 && params.length == 0)
				{
					result.consume(createConstructorReferenceLookup(functionalInterfaceType, eachReturnType));
				}
			}
			else if(params.length == 1 && PsiType.INT.equals(params[0].getType()))
			{
				result.consume(createConstructorReferenceLookup(functionalInterfaceType, eachReturnType));
			}
		};
		if(addInheritors && expectedReturnType instanceof PsiClassType)
		{
			JavaInheritorsGetter.processInheritors(parameters, Collections.singletonList((PsiClassType) expectedReturnType), matcher, consumer);
		}
		else
		{
			consumer.consume(expectedReturnType);
		}
	}

	private static LookupElement createConstructorReferenceLookup(@NotNull PsiType functionalInterfaceType, @NotNull PsiType constructedType)
	{
		constructedType = TypeConversionUtil.erasure(constructedType);
		return LookupElementBuilder.create(constructedType, constructedType.getPresentableText() + "::new").withTypeText(functionalInterfaceType.getPresentableText()).withIcon(AllIcons.Nodes
				.MethodReference).withInsertHandler(CONSTRUCTOR_REF_INSERT_HANDLER).withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE);
	}

	@NotNull
	private static LookupElement createMethodRefOnThis(PsiType functionalInterfaceType, PsiMethod psiMethod, @Nullable PsiClass outerClass)
	{
		String fullString = (outerClass == null ? "" : outerClass.getName() + ".") + "this::" + psiMethod.getName();
		return LookupElementBuilder.create(psiMethod, fullString).withLookupString(psiMethod.getName()).withPresentableText(fullString).withTypeText(functionalInterfaceType.getPresentableText())
				.withIcon(AllIcons.Nodes.MethodReference).withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE);
	}

	@NotNull
	private static LookupElement createMethodRefOnClass(PsiType functionalInterfaceType, PsiMethod psiMethod, PsiClass qualifierClass)
	{
		String presentableText = qualifierClass.getName() + "::" + psiMethod.getName();
		return LookupElementBuilder.create(psiMethod).withLookupString(presentableText).withPresentableText(presentableText).withInsertHandler((context, item) ->
		{
			context.getDocument().insertString(context.getStartOffset(), "::");
			JavaCompletionUtil.insertClassReference(qualifierClass, context.getFile(), context.getStartOffset());
		}).withTypeText(functionalInterfaceType.getPresentableText()).withIcon(AllIcons.Nodes.MethodReference).withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE);
	}

	private static List<LookupElement> collectThisVariants(PsiType functionalInterfaceType, PsiParameter[] params, PsiElement originalPosition, PsiSubstitutor substitutor, PsiType expectedReturnType)
	{
		List<LookupElement> result = new ArrayList<>();

		Iterable<PsiClass> instanceClasses = JBIterable.generate(originalPosition, PsiElement::getParent).filter(PsiMember.class).takeWhile(m -> !m.hasModifierProperty(PsiModifier.STATIC)).filter
				(PsiClass.class);

		boolean first = true;
		for(PsiClass psiClass : instanceClasses)
		{
			if(!first && psiClass.getName() == null)
			{
				continue;
			}

			for(PsiMethod psiMethod : psiClass.getMethods())
			{
				if(!psiMethod.hasModifierProperty(PsiModifier.STATIC) && hasAppropriateReturnType(expectedReturnType, psiMethod) && areParameterTypesAppropriate(psiMethod, params, substitutor, 0))
				{
					result.add(createMethodRefOnThis(functionalInterfaceType, psiMethod, first ? null : psiClass));
				}
			}
			first = false;
		}
		return result;
	}

	private static List<LookupElement> collectStaticVariants(PsiType functionalInterfaceType,
			PsiParameter[] params,
			PsiElement originalPosition,
			PsiSubstitutor substitutor,
			PsiType expectedReturnType)
	{
		List<LookupElement> result = new ArrayList<>();
		for(PsiClass psiClass : JBIterable.generate(PsiTreeUtil.getParentOfType(originalPosition, PsiClass.class), PsiClass::getContainingClass))
		{
			for(PsiMethod psiMethod : psiClass.getMethods())
			{
				if(psiMethod.hasModifierProperty(PsiModifier.STATIC) && hasAppropriateReturnType(expectedReturnType, psiMethod) && areParameterTypesAppropriate(psiMethod, params, substitutor, 0))
				{
					result.add(createMethodRefOnClass(functionalInterfaceType, psiMethod, psiClass));
				}
			}
		}
		return result;
	}

	private static List<LookupElement> collectVariantsByReceiver(boolean prioritize,
			PsiType functionalInterfaceType,
			PsiParameter[] params,
			PsiElement originalPosition,
			PsiSubstitutor substitutor,
			PsiType expectedReturnType)
	{
		List<LookupElement> result = new ArrayList<>();
		final PsiType functionalInterfaceParamType = substitutor.substitute(params[0].getType());
		final PsiClass paramClass = PsiUtil.resolveClassInClassTypeOnly(functionalInterfaceParamType);
		if(paramClass != null && !paramClass.hasTypeParameters())
		{
			final Set<String> visited = new HashSet<>();
			for(PsiMethod psiMethod : paramClass.getAllMethods())
			{
				PsiClass containingClass = psiMethod.getContainingClass();
				PsiClass qualifierClass = containingClass != null ? containingClass : paramClass;
				if(visited.add(psiMethod.getName()) && !psiMethod.hasModifierProperty(PsiModifier.STATIC) && hasAppropriateReturnType(expectedReturnType, psiMethod) && areParameterTypesAppropriate
						(psiMethod, params, substitutor, 1) && JavaResolveUtil.isAccessible(psiMethod, null, psiMethod.getModifierList(), originalPosition, null, null))
				{
					LookupElement methodRefLookupElement = createMethodRefOnClass(functionalInterfaceType, psiMethod, qualifierClass);
					if(prioritize && containingClass == paramClass)
					{
						methodRefLookupElement = PrioritizedLookupElement.withExplicitProximity(methodRefLookupElement, 1);
					}
					result.add(methodRefLookupElement);
				}
			}
		}
		return result;
	}

	private static boolean hasAppropriateReturnType(PsiType expectedReturnType, PsiMethod psiMethod)
	{
		PsiType returnType = psiMethod.getReturnType();
		return returnType != null && TypeConversionUtil.isAssignable(expectedReturnType, returnType);
	}

	private static boolean areParameterTypesAppropriate(PsiMethod psiMethod, PsiParameter[] params, PsiSubstitutor substitutor, int offset)
	{
		final PsiParameterList parameterList = psiMethod.getParameterList();
		if(parameterList.getParametersCount() == params.length - offset)
		{
			final PsiParameter[] referenceMethodParams = parameterList.getParameters();
			for(int i = 0; i < params.length - offset; i++)
			{
				if(!Comparing.equal(referenceMethodParams[i].getType(), substitutor.substitute(params[i + offset].getType())))
				{
					return false;
				}
			}
			return true;
		}
		return false;
	}

	private static String getParamName(PsiParameter param, PsiElement originalPosition)
	{
		return JavaCodeStyleManager.getInstance(originalPosition.getProject()).suggestUniqueVariableName(ObjectUtils.assertNotNull(param.getName()), originalPosition, false);
	}
}
