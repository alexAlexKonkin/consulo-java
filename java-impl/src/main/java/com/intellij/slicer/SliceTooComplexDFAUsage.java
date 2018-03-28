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
package com.intellij.slicer;

import java.awt.Font;

import javax.swing.Icon;

import javax.annotation.Nonnull;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.ui.JBColor;
import com.intellij.usages.TextChunk;
import com.intellij.usages.UsagePresentation;
import com.intellij.util.Processor;

/**
 * User: cdr
 */
public class SliceTooComplexDFAUsage extends SliceUsage
{
	public SliceTooComplexDFAUsage(@Nonnull PsiElement element, @Nonnull SliceUsage parent, @Nonnull PsiSubstitutor substitutor)
	{
		super(element, parent, substitutor, 0, "");
	}

	@Override
	public void processChildren(@Nonnull Processor<SliceUsage> processor)
	{
		// no children
	}

	@Nonnull
	@Override
	public UsagePresentation getPresentation()
	{
		final UsagePresentation presentation = super.getPresentation();
		return new UsagePresentation()
		{
			@Override
			@Nonnull
			public TextChunk[] getText()
			{
				return new TextChunk[]{
						new TextChunk(new TextAttributes(JBColor.RED, null, null, EffectType.WAVE_UNDERSCORE, Font.PLAIN), getTooltipText())
				};
			}

			@Override
			@Nonnull
			public String getPlainText()
			{
				return presentation.getPlainText();
			}

			@Override
			public Icon getIcon()
			{
				return presentation.getIcon();
			}

			@Override
			public String getTooltipText()
			{
				return "Too complex to analyze, analysis stopped here";
			}
		};
	}
}
