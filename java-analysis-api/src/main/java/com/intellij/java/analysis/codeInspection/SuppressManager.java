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

/*
 * User: anna
 * Date: 24-Dec-2007
 */
package com.intellij.java.analysis.codeInspection;

import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInspection.InspectionSuppressor;
import com.intellij.codeInspection.SuppressIntentionAction;
import com.intellij.codeInspection.SuppressQuickFix;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.java.language.psi.PsiAnnotation;
import com.intellij.java.language.psi.PsiCodeBlock;
import com.intellij.java.language.psi.PsiField;
import com.intellij.java.language.psi.PsiLiteralExpression;
import com.intellij.psi.util.PsiTreeUtil;

import javax.annotation.Nonnull;

public abstract class SuppressManager implements BatchSuppressManager, InspectionSuppressor
{
	public static SuppressManager getInstance()
	{
		return ServiceManager.getService(SuppressManager.class);
	}

	public static boolean isSuppressedInspectionName(PsiLiteralExpression expression)
	{
		PsiAnnotation annotation = PsiTreeUtil.getParentOfType(expression, PsiAnnotation.class, true, PsiCodeBlock.class, PsiField.class);
		return annotation != null && SUPPRESS_INSPECTIONS_ANNOTATION_NAME.equals(annotation.getQualifiedName());
	}

	@Nonnull
	@Override
	public SuppressQuickFix[] createBatchSuppressActions(@Nonnull HighlightDisplayKey key)
	{
		return BatchSuppressManager.SERVICE.getInstance().createBatchSuppressActions(key);
	}

	@Nonnull
	public abstract SuppressIntentionAction[] createSuppressActions(@Nonnull HighlightDisplayKey key);
}