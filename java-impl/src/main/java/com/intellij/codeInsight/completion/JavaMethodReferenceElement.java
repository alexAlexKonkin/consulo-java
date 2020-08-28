// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.completion;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.codeInsight.lookup.LookupItem;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodReferenceExpression;
import consulo.ide.IconDescriptorUpdaters;

import javax.annotation.Nonnull;
import java.util.Objects;

class JavaMethodReferenceElement extends LookupElement
{
	private final PsiMethod myMethod;
	private final PsiElement myRefPlace;

	JavaMethodReferenceElement(PsiMethod method, PsiElement refPlace)
	{
		myMethod = method;
		myRefPlace = refPlace;
	}

	@Nonnull
	@Override
	public Object getObject()
	{
		return myMethod;
	}

	@Nonnull
	@Override
	public String getLookupString()
	{
		return myMethod.isConstructor() ? "new" : myMethod.getName();
	}

	@Override
	public void renderElement(LookupElementPresentation presentation)
	{
		presentation.setIcon(IconDescriptorUpdaters.getIcon(myMethod, Iconable.ICON_FLAG_VISIBILITY));
		super.renderElement(presentation);
	}

	@Override
	public void handleInsert(@Nonnull InsertionContext context)
	{
		if(!(myRefPlace instanceof PsiMethodReferenceExpression))
		{
			PsiClass containingClass = Objects.requireNonNull(myMethod.getContainingClass());
			String qualifiedName = Objects.requireNonNull(containingClass.getQualifiedName());

			final Editor editor = context.getEditor();
			final Document document = editor.getDocument();
			final int startOffset = context.getStartOffset();

			document.insertString(startOffset, qualifiedName + "::");
			JavaCompletionUtil.shortenReference(context.getFile(), startOffset + qualifiedName.length() - 1);
		}
		JavaCompletionUtil
				.insertTail(context, this, LookupItem.handleCompletionChar(context.getEditor(), this, context.getCompletionChar()), false);
	}
}
