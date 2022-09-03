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
package com.intellij.java.debugger.impl.actions;

import consulo.language.psi.PsiElement;
import com.intellij.java.language.psi.PsiMethod;
import com.intellij.java.language.psi.PsiSubstitutor;
import com.intellij.java.language.psi.util.PsiFormatUtil;
import consulo.ide.impl.psi.util.PsiFormatUtilBase;
import com.intellij.util.Range;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Eugene Zhuravlev
 * Date: 10/25/13
 */
public class MethodSmartStepTarget extends SmartStepTarget
{
	private final PsiMethod myMethod;

	public MethodSmartStepTarget(
			@Nonnull PsiMethod method,
			@Nullable String label,
			@Nullable PsiElement highlightElement,
			boolean needBreakpointRequest,
			Range<Integer> lines)
	{
		super(label, highlightElement, needBreakpointRequest, lines);
		myMethod = method;
	}

	@Nullable
	@Override
	public Image getIcon()
	{
		return IconDescriptorUpdaters.getIcon(myMethod, 0);
	}

	@Nonnull
	@Override
	public String getPresentation()
	{
		String label = getLabel();
		String formatted = PsiFormatUtil.formatMethod(
				myMethod,
				PsiSubstitutor.EMPTY,
				PsiFormatUtilBase.SHOW_NAME | PsiFormatUtilBase.SHOW_PARAMETERS,
				PsiFormatUtilBase.SHOW_TYPE,
				999
		);
		return label != null ? label + formatted : formatted;
	}

	@Nonnull
	public PsiMethod getMethod()
	{
		return myMethod;
	}

	public boolean equals(Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(o == null || getClass() != o.getClass())
		{
			return false;
		}

		final MethodSmartStepTarget that = (MethodSmartStepTarget) o;

		if(!myMethod.equals(that.myMethod))
		{
			return false;
		}

		return true;
	}

	public int hashCode()
	{
		return myMethod.hashCode();
	}
}
