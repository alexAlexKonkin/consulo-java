/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

/**
 * @author cdr
 */
package com.intellij.codeInsight.daemon.impl.analysis;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.application.options.colors.ScopeAttributesUtil;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.ide.highlighter.JavaHighlightingColors;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.colors.TextAttributesScheme;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.packageDependencies.DependencyValidationManager;
import com.intellij.packageDependencies.DependencyValidationManagerImpl;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.ElementType;
import com.intellij.psi.impl.source.tree.TreeUtil;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import com.intellij.psi.search.scope.packageSet.NamedScopesHolder;
import com.intellij.psi.search.scope.packageSet.PackageSet;
import com.intellij.psi.util.PsiTreeUtil;

public class HighlightNamesUtil
{
	private static final Logger LOG = Logger.getInstance(HighlightNamesUtil.class);

	@Nullable
	public static HighlightInfo highlightMethodName(@NotNull PsiMethod method, final PsiElement elementToHighlight, final boolean isDeclaration, @NotNull TextAttributesScheme colorsScheme)
	{
		return highlightMethodName(method, elementToHighlight, elementToHighlight.getTextRange(), colorsScheme, isDeclaration);
	}

	@Nullable
	public static HighlightInfo highlightMethodName(@NotNull PsiMember methodOrClass,
			final PsiElement elementToHighlight,
			TextRange range,
			@NotNull TextAttributesScheme colorsScheme,
			final boolean isDeclaration)
	{
		boolean isInherited = false;

		if(!isDeclaration)
		{
			if(isCalledOnThis(elementToHighlight))
			{
				final PsiClass containingClass = methodOrClass instanceof PsiMethod ? methodOrClass.getContainingClass() : null;
				PsiClass enclosingClass = containingClass == null ? null : PsiTreeUtil.getParentOfType(elementToHighlight, PsiClass.class);
				while(enclosingClass != null)
				{
					isInherited = enclosingClass.isInheritor(containingClass, true);
					if(isInherited)
					{
						break;
					}
					enclosingClass = PsiTreeUtil.getParentOfType(enclosingClass, PsiClass.class, true);
				}
			}
		}

		LOG.assertTrue(methodOrClass instanceof PsiMethod || !isDeclaration);
		TextAttributesKey attributesKey = methodOrClass instanceof PsiMethod ? getMethodNameHighlightKey((PsiMethod) methodOrClass, isDeclaration,
				isInherited).getAttributesKey() : CodeInsightColors.CONSTRUCTOR_CALL_ATTRIBUTES;
		if(attributesKey != null)
		{
			TextAttributes attributes = mergeWithScopeAttributes(methodOrClass, attributesKey, colorsScheme);
			HighlightInfo.Builder builder = HighlightInfo.newHighlightInfo(HighlightInfoType.INFORMATION).range(range);
			if(attributes != null)
			{
				builder.textAttributes(attributes);
			}
			builder = builder.needsUpdateOnTyping(false);
			builder = builder.severity(HighlightInfoType.SYMBOL_TYPE_SEVERITY);
			return builder.createUnconditionally();
		}
		return null;
	}

	private static boolean isCalledOnThis(PsiElement elementToHighlight)
	{
		PsiMethodCallExpression methodCallExpression = PsiTreeUtil.getParentOfType(elementToHighlight, PsiMethodCallExpression.class);
		if(methodCallExpression != null)
		{
			PsiElement qualifier = methodCallExpression.getMethodExpression().getQualifier();
			if(qualifier == null || qualifier instanceof PsiThisExpression)
			{
				return true;
			}
		}
		return false;
	}

	private static TextAttributes mergeWithScopeAttributes(final PsiElement element, @NotNull TextAttributesKey attributesKey, @NotNull TextAttributesScheme colorsScheme)
	{
		TextAttributes regularAttributes = colorsScheme.getAttributes(attributesKey);
		if(element == null)
		{
			return regularAttributes;
		}
		TextAttributes scopeAttributes = getScopeAttributes(element, colorsScheme);
		return TextAttributes.merge(scopeAttributes, regularAttributes);
	}

	@Nullable
	public static HighlightInfo highlightClassName(PsiClass aClass, PsiElement elementToHighlight, @NotNull TextAttributesScheme colorsScheme)
	{
		TextAttributesKey type = getClassNameHighlightKey(aClass, elementToHighlight);
		if(elementToHighlight != null)
		{
			TextAttributes attributes = mergeWithScopeAttributes(aClass, type, colorsScheme);
			TextRange range = elementToHighlight.getTextRange();
			if(elementToHighlight instanceof PsiJavaCodeReferenceElement)
			{
				final PsiJavaCodeReferenceElement referenceElement = (PsiJavaCodeReferenceElement) elementToHighlight;
				PsiReferenceParameterList parameterList = referenceElement.getParameterList();
				if(parameterList != null)
				{
					final TextRange paramListRange = parameterList.getTextRange();
					if(paramListRange.getEndOffset() > paramListRange.getStartOffset())
					{
						range = new TextRange(range.getStartOffset(), paramListRange.getStartOffset());
					}
				}
			}

			// This will highlight @ sign in annotation as well.
			final PsiElement parent = elementToHighlight.getParent();
			if(parent instanceof PsiAnnotation)
			{
				final PsiAnnotation psiAnnotation = (PsiAnnotation) parent;
				range = new TextRange(psiAnnotation.getTextRange().getStartOffset(), range.getEndOffset());
			}

			HighlightInfo.Builder builder = HighlightInfo.newHighlightInfo(HighlightInfoType.INFORMATION).range(range);
			builder = builder.needsUpdateOnTyping(false);
			builder = builder.severity(HighlightInfoType.SYMBOL_TYPE_SEVERITY);
			if(attributes != null)
			{
				builder.textAttributes(attributes);
			}
			return builder.createUnconditionally();
		}
		return null;
	}

	@Nullable
	public static HighlightInfo highlightVariableName(final PsiVariable variable, final PsiElement elementToHighlight, @NotNull TextAttributesScheme colorsScheme)
	{
		TextAttributesKey highlightKey = getVariableNameHighlightKey(variable);
		if(highlightKey != null)
		{
			if(variable instanceof PsiField)
			{
				TextAttributes attributes = mergeWithScopeAttributes(variable, highlightKey, colorsScheme);
				HighlightInfo.Builder builder = HighlightInfo.newHighlightInfo(HighlightInfoType.INFORMATION).range(elementToHighlight.getTextRange());
				if(attributes != null)
				{
					builder.textAttributes(attributes);
				}
				builder = builder.needsUpdateOnTyping(false);
				builder = builder.severity(HighlightInfoType.SYMBOL_TYPE_SEVERITY);
				return builder.createUnconditionally();
			}
			return HighlightInfo.newHighlightInfo(HighlightInfoType.INFORMATION).range(elementToHighlight).create();
		}
		return null;
	}

	@Nullable
	public static HighlightInfo highlightClassNameInQualifier(final PsiJavaCodeReferenceElement element, @NotNull TextAttributesScheme colorsScheme)
	{
		PsiExpression qualifierExpression = null;
		if(element instanceof PsiReferenceExpression)
		{
			qualifierExpression = ((PsiReferenceExpression) element).getQualifierExpression();
		}
		if(qualifierExpression instanceof PsiJavaCodeReferenceElement)
		{
			PsiElement resolved = ((PsiJavaCodeReferenceElement) qualifierExpression).resolve();
			if(resolved instanceof PsiClass)
			{
				return highlightClassName((PsiClass) resolved, qualifierExpression, colorsScheme);
			}
		}
		return null;
	}

	//TODO [VISTALL] migrate to text attribute keys
	@NotNull
	private static HighlightInfoType getMethodNameHighlightKey(@NotNull PsiMethod method, boolean isDeclaration, boolean isInheritedMethod)
	{
		if(method.isConstructor())
		{
			return isDeclaration ? HighlightInfoType.CONSTRUCTOR_DECLARATION : HighlightInfoType.CONSTRUCTOR_CALL;
		}
		if(isDeclaration)
		{
			return HighlightInfoType.METHOD_DECLARATION;
		}
		if(method.hasModifierProperty(PsiModifier.STATIC))
		{
			return HighlightInfoType.STATIC_METHOD;
		}
		if(isInheritedMethod)
		{
			return HighlightInfoType.INHERITED_METHOD;
		}
		if(method.hasModifierProperty(PsiModifier.ABSTRACT))
		{
			return HighlightInfoType.ABSTRACT_METHOD;
		}
		return HighlightInfoType.METHOD_CALL;
	}

	@Nullable
	private static TextAttributesKey getVariableNameHighlightKey(PsiVariable var)
	{
		if(var instanceof PsiLocalVariable || var instanceof PsiParameter && ((PsiParameter) var).getDeclarationScope() instanceof PsiForeachStatement)
		{
			return JavaHighlightingColors.LOCAL_VARIABLE;
		}
		if(var instanceof PsiField)
		{
			return var.hasModifierProperty(PsiModifier.STATIC) ? var.hasModifierProperty(PsiModifier.FINAL) ? JavaHighlightingColors.STATIC_FINAL_FIELD : JavaHighlightingColors.STATIC_FIELD :
					JavaHighlightingColors.INSTANCE_FIELD;
		}
		if(var instanceof PsiParameter)
		{
			return JavaHighlightingColors.PARAMETER;
		}
		return null;
	}

	@Nullable
	public static HighlightInfo highlightReassignedVariable(PsiVariable variable, PsiElement elementToHighlight)
	{
		if(variable instanceof PsiLocalVariable)
		{
			return HighlightInfo.newHighlightInfo(HighlightInfoType.INFORMATION).textAttributes(JavaHighlightingColors.REASSIGNED_LOCAL_VARIABLE).range(elementToHighlight).create();
		}
		if(variable instanceof PsiParameter)
		{
			return HighlightInfo.newHighlightInfo(HighlightInfoType.INFORMATION).textAttributes(JavaHighlightingColors.REASSIGNED_PARAMETER).range(elementToHighlight).create();
		}
		return null;
	}

	@NotNull
	private static TextAttributesKey getClassNameHighlightKey(@Nullable PsiClass aClass, @Nullable PsiElement element)
	{
		if(element instanceof PsiJavaCodeReferenceElement && element.getParent() instanceof PsiAnonymousClass)
		{
			return JavaHighlightingColors.ANONYMOUS_CLASS_NAME;
		}
		if(aClass != null)
		{
			if(aClass.isAnnotationType())
			{
				return JavaHighlightingColors.ANNOTATION_NAME;
			}
			if(aClass.isInterface())
			{
				return JavaHighlightingColors.INTERFACE_NAME;
			}
			if(aClass.isEnum())
			{
				return JavaHighlightingColors.ENUM_NAME;
			}
			if(aClass instanceof PsiTypeParameter)
			{
				return JavaHighlightingColors.TYPE_PARAMETER_NAME;
			}
			final PsiModifierList modList = aClass.getModifierList();
			if(modList != null && modList.hasModifierProperty(PsiModifier.ABSTRACT))
			{
				return JavaHighlightingColors.ABSTRACT_CLASS_NAME;
			}
		}
		// use class by default
		return JavaHighlightingColors.CLASS_NAME;
	}

	private static TextAttributes getScopeAttributes(@NotNull PsiElement element, @NotNull TextAttributesScheme colorsScheme)
	{
		PsiFile file = element.getContainingFile();
		if(file == null)
		{
			return null;
		}
		TextAttributes result = null;
		DependencyValidationManagerImpl validationManager = (DependencyValidationManagerImpl) DependencyValidationManager.getInstance(file.getProject());
		List<Pair<NamedScope, NamedScopesHolder>> scopes = validationManager.getScopeBasedHighlightingCachedScopes();
		for(Pair<NamedScope, NamedScopesHolder> scope : scopes)
		{
			NamedScope namedScope = scope.getFirst();
			NamedScopesHolder scopesHolder = scope.getSecond();
			PackageSet packageSet = namedScope.getValue();
			if(packageSet != null && packageSet.contains(file, scopesHolder))
			{
				TextAttributesKey scopeKey = ScopeAttributesUtil.getScopeTextAttributeKey(namedScope.getName());
				TextAttributes attributes = colorsScheme.getAttributes(scopeKey);
				if(attributes == null || attributes.isEmpty())
				{
					continue;
				}
				result = TextAttributes.merge(attributes, result);
			}
		}
		return result;
	}

	public static TextRange getMethodDeclarationTextRange(@NotNull PsiMethod method)
	{
		if(method instanceof SyntheticElement)
		{
			return TextRange.EMPTY_RANGE;
		}
		int start = stripAnnotationsFromModifierList(method.getModifierList());
		final TextRange throwsRange = method.getThrowsList().getTextRange();
		LOG.assertTrue(throwsRange != null, method);
		int end = throwsRange.getEndOffset();
		return new TextRange(start, end);
	}

	public static TextRange getFieldDeclarationTextRange(@NotNull PsiField field)
	{
		int start = stripAnnotationsFromModifierList(field.getModifierList());
		int end = field.getNameIdentifier().getTextRange().getEndOffset();
		return new TextRange(start, end);
	}

	public static TextRange getClassDeclarationTextRange(@NotNull PsiClass aClass)
	{
		if(aClass instanceof PsiEnumConstantInitializer)
		{
			return ((PsiEnumConstantInitializer) aClass).getEnumConstant().getNameIdentifier().getTextRange();
		}
		final PsiElement psiElement = aClass instanceof PsiAnonymousClass ? ((PsiAnonymousClass) aClass).getBaseClassReference() : aClass.getModifierList() == null ? aClass.getNameIdentifier() :
				aClass.getModifierList();
		if(psiElement == null)
		{
			return new TextRange(aClass.getTextRange().getStartOffset(), aClass.getTextRange().getStartOffset());
		}
		int start = stripAnnotationsFromModifierList(psiElement);
		PsiElement endElement = aClass instanceof PsiAnonymousClass ? ((PsiAnonymousClass) aClass).getBaseClassReference() : aClass.getImplementsList();
		if(endElement == null)
		{
			endElement = aClass.getNameIdentifier();
		}
		TextRange endTextRange = endElement == null ? null : endElement.getTextRange();
		int end = endTextRange == null ? start : endTextRange.getEndOffset();
		return new TextRange(start, end);
	}

	private static int stripAnnotationsFromModifierList(@NotNull PsiElement element)
	{
		TextRange textRange = element.getTextRange();
		if(textRange == null)
		{
			return 0;
		}
		PsiAnnotation lastAnnotation = null;
		for(PsiElement child : element.getChildren())
		{
			if(child instanceof PsiAnnotation)
			{
				lastAnnotation = (PsiAnnotation) child;
			}
		}
		if(lastAnnotation == null)
		{
			return textRange.getStartOffset();
		}
		ASTNode node = lastAnnotation.getNode();
		if(node != null)
		{
			do
			{
				node = TreeUtil.nextLeaf(node);
			}
			while(node != null && ElementType.JAVA_COMMENT_OR_WHITESPACE_BIT_SET.contains(node.getElementType()));
		}
		if(node != null)
		{
			return node.getTextRange().getStartOffset();
		}
		return textRange.getStartOffset();
	}
}
