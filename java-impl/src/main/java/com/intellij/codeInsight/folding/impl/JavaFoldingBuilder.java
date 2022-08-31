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
package com.intellij.codeInsight.folding.impl;

import java.awt.Font;

import javax.annotation.Nonnull;

import com.intellij.codeInsight.ExpectedTypeInfo;
import com.intellij.codeInsight.ExpectedTypesProvider;
import com.intellij.java.language.JavaLanguage;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.project.Project;
import com.intellij.java.language.psi.PsiAnonymousClass;
import com.intellij.java.language.psi.PsiAssignmentExpression;
import com.intellij.psi.PsiElement;
import com.intellij.java.language.psi.PsiNewExpression;
import com.intellij.java.language.psi.PsiReferenceExpression;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.util.FontUtil;

public class JavaFoldingBuilder extends JavaFoldingBuilderBase
{
	@Override
	protected boolean isBelowRightMargin(@Nonnull Project project, int lineLength)
	{
		final CodeStyleSettings settings = CodeStyleSettingsManager.getSettings(project);
		return lineLength <= settings.getRightMargin(JavaLanguage.INSTANCE);
	}

	@Override
	protected boolean shouldShowExplicitLambdaType(@Nonnull PsiAnonymousClass anonymousClass, @Nonnull PsiNewExpression expression)
	{
		PsiElement parent = expression.getParent();
		if(parent instanceof PsiReferenceExpression || parent instanceof PsiAssignmentExpression)
		{
			return true;
		}

		ExpectedTypeInfo[] types = ExpectedTypesProvider.getExpectedTypes(expression, false);
		return types.length != 1 || !types[0].getType().equals(anonymousClass.getBaseClassType());
	}

	@Override
	@Nonnull
	protected String rightArrow()
	{
		return getRightArrow();
	}

	@Nonnull
	public static String getRightArrow()
	{
		Font font = EditorColorsManager.getInstance().getGlobalScheme().getFont(EditorFontType.PLAIN);
		return FontUtil.rightArrow(font);
	}
}

