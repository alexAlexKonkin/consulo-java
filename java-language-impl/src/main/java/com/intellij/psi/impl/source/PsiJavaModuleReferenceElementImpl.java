// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.impl.source;

import com.intellij.java.language.psi.JavaElementVisitor;
import com.intellij.java.language.psi.PsiJavaModule;
import com.intellij.java.language.psi.PsiJavaModuleReference;
import com.intellij.java.language.psi.PsiJavaModuleReferenceElement;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.CompositePsiElement;
import com.intellij.psi.impl.source.tree.JavaElementType;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import javax.annotation.Nonnull;

public class PsiJavaModuleReferenceElementImpl extends CompositePsiElement implements PsiJavaModuleReferenceElement
{
	public PsiJavaModuleReferenceElementImpl()
	{
		super(JavaElementType.MODULE_REFERENCE);
	}

	@Nonnull
	@Override
	public String getReferenceText()
	{
		StringBuilder sb = new StringBuilder();
		for(PsiElement e = getFirstChild(); e != null; e = e.getNextSibling())
		{
			if(!(e instanceof PsiWhiteSpace) && !(e instanceof PsiComment))
			{
				sb.append(e.getText());
			}
		}
		return sb.toString();
	}

	@Override
	public PsiJavaModuleReference getReference()
	{
		if(getParent() instanceof PsiJavaModule && !(getContainingFile() instanceof JavaDummyHolder))
		{
			return null;  // module name identifier is not a reference
		}
		else
		{
			return CachedValuesManager.getCachedValue(this, () -> CachedValueProvider.Result.create(new PsiJavaModuleReferenceImpl(this), this));
		}
	}

	@Override
	public void accept(@Nonnull PsiElementVisitor visitor)
	{
		if(visitor instanceof JavaElementVisitor)
		{
			((JavaElementVisitor) visitor).visitModuleReferenceElement(this);
		}
		else
		{
			visitor.visitElement(this);
		}
	}

	@Override
	public String toString()
	{
		return "PsiJavaModuleReference";
	}
}