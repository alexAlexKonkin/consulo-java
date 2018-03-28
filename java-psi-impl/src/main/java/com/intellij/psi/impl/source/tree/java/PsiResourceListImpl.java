/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.psi.impl.source.tree.java;

import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;

import com.intellij.lang.ASTNode;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiResourceList;
import com.intellij.psi.PsiResourceListElement;
import com.intellij.psi.PsiResourceVariable;
import com.intellij.psi.ResolveState;
import com.intellij.psi.impl.PsiImplUtil;
import com.intellij.psi.impl.source.tree.CompositePsiElement;
import com.intellij.psi.impl.source.tree.JavaElementType;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.util.PsiTreeUtil;

public class PsiResourceListImpl extends CompositePsiElement implements PsiResourceList
{
	public PsiResourceListImpl()
	{
		super(JavaElementType.RESOURCE_LIST);
	}

	@Override
	public int getResourceVariablesCount()
	{
		int count = 0;
		for(PsiElement child = getFirstChild(); child != null; child = child.getNextSibling())
		{
			if(child instanceof PsiResourceListElement)
			{
				++count;
			}
		}
		return count;
	}

	@Deprecated
	@Override
	public List<PsiResourceVariable> getResourceVariables()
	{
		return PsiTreeUtil.getChildrenOfTypeAsList(this, PsiResourceVariable.class);
	}

	@Nonnull
	@Override
	public Iterator<PsiResourceListElement> iterator()
	{
		return PsiTreeUtil.childIterator(this, PsiResourceListElement.class);
	}

	@Override
	public void accept(@Nonnull PsiElementVisitor visitor)
	{
		if(visitor instanceof JavaElementVisitor)
		{
			((JavaElementVisitor) visitor).visitResourceList(this);
		}
		else
		{
			visitor.visitElement(this);
		}
	}

	@Override
	public boolean processDeclarations(@Nonnull PsiScopeProcessor processor,
			@Nonnull ResolveState state,
			PsiElement lastParent,
			@Nonnull PsiElement place)
	{
		return PsiImplUtil.processDeclarationsInResourceList(this, processor, state, lastParent);
	}

	@Override
	public void deleteChildInternal(@Nonnull ASTNode child)
	{
		if(child.getPsi() instanceof PsiResourceListElement && getResourceVariablesCount() == 1)
		{
			getTreeParent().deleteChildInternal(this);
			return;
		}

		super.deleteChildInternal(child);
	}

	@Override
	public String toString()
	{
		return "PsiResourceList:" + getText();
	}
}
