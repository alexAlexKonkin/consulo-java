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
package com.intellij.psi.impl.source;

import static com.intellij.openapi.util.text.StringUtil.nullize;

import javax.annotation.Nonnull;

import com.intellij.lang.ASTNode;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiJavaModuleReferenceElement;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiRequiresStatement;
import com.intellij.psi.impl.java.stubs.JavaStubElementTypes;
import com.intellij.psi.impl.java.stubs.PsiRequiresStatementStub;
import com.intellij.psi.util.PsiTreeUtil;

public class PsiRequiresStatementImpl extends JavaStubPsiElement<PsiRequiresStatementStub> implements PsiRequiresStatement
{
	public PsiRequiresStatementImpl(@Nonnull PsiRequiresStatementStub stub)
	{
		super(stub, JavaStubElementTypes.REQUIRES_STATEMENT);
	}

	public PsiRequiresStatementImpl(@Nonnull ASTNode node)
	{
		super(node);
	}

	@Override
	public PsiJavaModuleReferenceElement getReferenceElement()
	{
		return PsiTreeUtil.getChildOfType(this, PsiJavaModuleReferenceElement.class);
	}

	@Override
	public String getModuleName()
	{
		PsiRequiresStatementStub stub = getGreenStub();
		if(stub != null)
		{
			return nullize(stub.getModuleName());
		}
		else
		{
			PsiJavaModuleReferenceElement refElement = getReferenceElement();
			return refElement != null ? refElement.getReferenceText() : null;
		}
	}

	@Override
	public PsiModifierList getModifierList()
	{
		return getStubOrPsiChild(JavaStubElementTypes.MODIFIER_LIST);
	}

	@Override
	public boolean hasModifierProperty(@Nonnull String name)
	{
		PsiModifierList modifierList = getModifierList();
		return modifierList != null && modifierList.hasModifierProperty(name);
	}

	@Override
	public void accept(@Nonnull PsiElementVisitor visitor)
	{
		if(visitor instanceof JavaElementVisitor)
		{
			((JavaElementVisitor) visitor).visitRequiresStatement(this);
		}
		else
		{
			visitor.visitElement(this);
		}
	}

	@Override
	public String toString()
	{
		return "PsiRequiresStatement";
	}
}