// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection.dataFlow.inliner;

import com.intellij.codeInspection.dataFlow.CFGBuilder;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import com.siyeh.ig.callMatcher.CallMatcher;
import javax.annotation.Nonnull;

import static com.intellij.psi.CommonClassNames.*;

public class BoxingInliner implements CallInliner
{
	private static final CallMatcher BOXING_CALL = CallMatcher.anyOf(
			CallMatcher.staticCall(JAVA_LANG_INTEGER, "valueOf").parameterTypes("int"),
			CallMatcher.staticCall(JAVA_LANG_LONG, "valueOf").parameterTypes("long"),
			CallMatcher.staticCall(JAVA_LANG_SHORT, "valueOf").parameterTypes("short"),
			CallMatcher.staticCall(JAVA_LANG_BYTE, "valueOf").parameterTypes("byte"),
			CallMatcher.staticCall(JAVA_LANG_CHARACTER, "valueOf").parameterTypes("char"),
			CallMatcher.staticCall(JAVA_LANG_BOOLEAN, "valueOf").parameterTypes("boolean"),
			CallMatcher.staticCall(JAVA_LANG_FLOAT, "valueOf").parameterTypes("float"),
			CallMatcher.staticCall(JAVA_LANG_DOUBLE, "valueOf").parameterTypes("double")
	);

	@Override
	public boolean tryInlineCall(@Nonnull CFGBuilder builder, @Nonnull PsiMethodCallExpression call)
	{
		if(BOXING_CALL.test(call))
		{
			PsiExpression arg = call.getArgumentList().getExpressions()[0];
			PsiType type = PsiPrimitiveType.getUnboxedType(call.getType());
			builder.pushExpression(arg)
					.boxUnbox(arg, arg.getType(), type)
					.boxUnbox(call, type, call.getType());
			return true;
		}
		return false;
	}
}