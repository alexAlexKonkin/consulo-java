// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.impl.source.tree.java;

import com.intellij.java.language.psi.JavaElementVisitor;
import com.intellij.java.language.psi.PsiPatternVariable;
import com.intellij.java.language.psi.PsiTypeElement;
import com.intellij.java.language.psi.PsiTypeTestPattern;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.Constants;
import com.intellij.psi.impl.source.tree.CompositePsiElement;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PsiTypeTestPatternImpl extends CompositePsiElement implements PsiTypeTestPattern, Constants
{
	public PsiTypeTestPatternImpl()
	{
		super(TYPE_TEST_PATTERN);
	}

	@Nonnull
	@Override
	public PsiTypeElement getCheckType()
	{
		for(PsiElement child = getFirstChild(); child != null; child = child.getNextSibling())
		{
			if(child instanceof PsiTypeElement)
			{
				return (PsiTypeElement) child;
			}
			if(child instanceof PsiPatternVariable)
			{
				return ((PsiPatternVariable) child).getTypeElement();
			}
		}
		throw new IllegalStateException(this.getText());
	}

	@Nullable
	@Override
	public PsiPatternVariable getPatternVariable()
	{
		return PsiTreeUtil.getChildOfType(this, PsiPatternVariable.class);
	}


	@Override
	public void accept(@Nonnull PsiElementVisitor visitor)
	{
		if(visitor instanceof JavaElementVisitor)
		{
			((JavaElementVisitor) visitor).visitTypeTestPattern(this);
		}
		else
		{
			visitor.visitElement(this);
		}
	}

	@Override
	public boolean processDeclarations(@Nonnull PsiScopeProcessor processor, @Nonnull ResolveState state, PsiElement lastParent,
									   @Nonnull PsiElement place)
	{
		processor.handleEvent(PsiScopeProcessor.Event.SET_DECLARATION_HOLDER, this);

		PsiPatternVariable variable = getPatternVariable();
		if(variable != null && variable != lastParent)
		{
			return processor.execute(variable, state);
		}
		return true;
	}

	@Override
	public String toString()
	{
		return "PsiTypeTestPattern";
	}
}

