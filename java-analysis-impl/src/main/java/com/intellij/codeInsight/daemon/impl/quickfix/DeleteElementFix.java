/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.codeInsight.daemon.impl.quickfix;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.Nls;

import javax.annotation.Nullable;
import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ObjectUtil;
import com.siyeh.ig.psiutils.CommentTracker;
import consulo.java.JavaQuickFixBundle;

public class DeleteElementFix extends LocalQuickFixAndIntentionActionOnPsiElement
{
	private final String myText;

	public DeleteElementFix(@Nonnull PsiElement element)
	{
		super(element);
		myText = null;
	}

	public DeleteElementFix(@Nonnull PsiElement element, @Nonnull @Nls String text)
	{
		super(element);
		myText = text;
	}

	@Nls
	@Nonnull
	@Override
	public String getText()
	{
		return ObjectUtil.notNull(myText, getFamilyName());
	}

	@Nls
	@Nonnull
	@Override
	public String getFamilyName()
	{
		return JavaQuickFixBundle.message("delete.element.fix.text");
	}

	@Override
	public void invoke(@Nonnull Project project, @Nonnull PsiFile file, @Nullable Editor editor, @Nonnull PsiElement startElement, @Nonnull PsiElement endElement)
	{
		if(FileModificationService.getInstance().preparePsiElementForWrite(file))
		{
			WriteAction.run(() -> new CommentTracker().deleteAndRestoreComments(startElement));
		}
	}

	@Override
	public boolean startInWriteAction()
	{
		return false;
	}
}