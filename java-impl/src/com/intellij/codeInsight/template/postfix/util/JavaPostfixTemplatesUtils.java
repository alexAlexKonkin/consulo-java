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
package com.intellij.codeInsight.template.postfix.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import consulo.java.module.util.JavaClassNames;
import com.intellij.codeInsight.CodeInsightServicesUtil;
import com.intellij.codeInsight.CodeInsightUtilCore;
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplatePsiInfo;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTreeUtil;

public abstract class JavaPostfixTemplatesUtils {
	private JavaPostfixTemplatesUtils() {
	}

	public static PostfixTemplatePsiInfo JAVA_PSI_INFO = new PostfixTemplatePsiInfo() {

		@NotNull
		@Override
		public PsiElement createStatement(@NotNull PsiElement context,
				@NotNull String prefix,
				@NotNull String suffix) {
			return JavaPostfixTemplatesUtils.createStatement(context, prefix, suffix);
		}

		@NotNull
		@Override
		public PsiElement createExpression(@NotNull PsiElement context,
				@NotNull String prefix,
				@NotNull String suffix) {
			return JavaPostfixTemplatesUtils.createExpression(context, prefix, suffix);
		}

		@Nullable
		@Override
		public PsiExpression getTopmostExpression(@NotNull PsiElement element) {
			return JavaPostfixTemplatesUtils.getTopmostExpression(element);
		}

		@NotNull
		@Override
		public PsiExpression getNegatedExpression(@NotNull PsiElement element) {
			return CodeInsightServicesUtil.invertCondition((PsiExpression)element);
		}
	};

	public static Condition<PsiElement> IS_BOOLEAN = new Condition<PsiElement>() {
		@Override
		public boolean value(PsiElement element) {
			return element instanceof PsiExpression && isBoolean(((PsiExpression)element).getType());
		}
	};

	public static Condition<PsiElement> IS_THROWABLE = new Condition<PsiElement>() {
		@Override
		public boolean value(PsiElement element) {
			return element instanceof PsiExpression && isThrowable((((PsiExpression)element).getType()));
		}
	};

	public static Condition<PsiElement> IS_NON_VOID = new Condition<PsiElement>() {
		@Override
		public boolean value(PsiElement element) {
			return element instanceof PsiExpression && isNonVoid((((PsiExpression)element).getType()));
		}
	};

	public static Condition<PsiElement> IS_NOT_PRIMITIVE = new Condition<PsiElement>() {
		@Override
		public boolean value(PsiElement element) {
			return element instanceof PsiExpression && isNotPrimitiveTypeExpression(((PsiExpression)element));
		}
	};

	public static PsiElement createStatement(@NotNull PsiElement context,
			@NotNull String prefix,
			@NotNull String suffix) {
		PsiExpression expr = getTopmostExpression(context);
		PsiElement parent = expr != null ? expr.getParent() : null;
		assert parent instanceof PsiStatement;
		PsiElementFactory factory = JavaPsiFacade.getInstance(context.getProject()).getElementFactory();
		return factory.createStatementFromText(prefix + expr.getText() + suffix, expr);
	}

	public static PsiElement createExpression(@NotNull PsiElement context,
			@NotNull String prefix,
			@NotNull String suffix) {
		PsiExpression expr = getTopmostExpression(context);
		PsiElement parent = expr != null ? expr.getParent() : null;
		assert parent instanceof PsiStatement;
		PsiElementFactory factory = JavaPsiFacade.getInstance(context.getProject()).getElementFactory();
		return factory.createExpressionFromText(prefix + expr.getText() + suffix, expr);
	}

	@Contract("null -> false")
	public static boolean isNotPrimitiveTypeExpression(@Nullable PsiExpression expression) {
		return expression != null && !(expression.getType() instanceof PsiPrimitiveType);
	}

	@Contract("null -> false")
	public static boolean isIterable(@Nullable PsiType type) {
		return type != null && InheritanceUtil.isInheritor(type, JavaClassNames.JAVA_LANG_ITERABLE);
	}

	@Contract("null -> false")
	public static boolean isThrowable(@Nullable PsiType type) {
		return type != null && InheritanceUtil.isInheritor(type, JavaClassNames.JAVA_LANG_THROWABLE);
	}

	@Contract("null -> false")
	public static boolean isArray(@Nullable PsiType type) {
		return type != null && type instanceof PsiArrayType;
	}

	@Contract("null -> false")
	public static boolean isBoolean(@Nullable PsiType type) {
		return type != null && (PsiType.BOOLEAN.equals(type) || PsiType.BOOLEAN.equals(PsiPrimitiveType.getUnboxedType(type)));
	}

	@Contract("null -> false")
	public static boolean isNonVoid(@Nullable PsiType type) {
		return type != null && !PsiType.VOID.equals(type);
	}

	@Contract("null -> false")
	public static boolean isNumber(@Nullable PsiType type) {
		if (type == null) {
			return false;
		}
		if (PsiType.INT.equals(type) || PsiType.BYTE.equals(type) || PsiType.LONG.equals(type)) {
			return true;
		}

		PsiPrimitiveType unboxedType = PsiPrimitiveType.getUnboxedType(type);
		return PsiType.INT.equals(unboxedType) || PsiType.BYTE.equals(unboxedType) || PsiType.LONG.equals(unboxedType);
	}

	@Nullable
	public static PsiExpression getTopmostExpression(PsiElement context) {
		PsiExpressionStatement statement = PsiTreeUtil.getNonStrictParentOfType(context, PsiExpressionStatement.class);
		return statement != null ? PsiTreeUtil.getChildOfType(statement, PsiExpression.class) : null;
	}

	public static void formatPsiCodeBlock(PsiElement newStatement, Editor editor) {
		CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(newStatement.getProject());
		PsiElement statement = newStatement.replace(codeStyleManager.reformat(newStatement));

		PsiCodeBlock type = PsiTreeUtil.getChildOfType(statement, PsiCodeBlock.class);
		assert type != null;
		PsiCodeBlock block = CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(type);
		TextRange range = block.getStatements()[0].getTextRange();
		editor.getDocument().deleteString(range.getStartOffset(), range.getEndOffset());

		editor.getCaretModel().moveToOffset(range.getStartOffset());
	}
}

