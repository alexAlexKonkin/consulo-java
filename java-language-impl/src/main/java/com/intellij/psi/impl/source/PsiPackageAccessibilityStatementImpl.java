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

import static com.intellij.psi.SyntaxTraverser.psiTraverser;

import java.util.List;

import javax.annotation.Nonnull;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.java.language.psi.JavaElementVisitor;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.java.language.psi.PsiJavaCodeReferenceElement;
import com.intellij.java.language.psi.PsiJavaModuleReferenceElement;
import com.intellij.java.language.psi.PsiNameHelper;
import com.intellij.java.language.psi.PsiPackageAccessibilityStatement;
import com.intellij.psi.impl.java.stubs.JavaPackageAccessibilityStatementElementType;
import com.intellij.psi.impl.java.stubs.PsiPackageAccessibilityStatementStub;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;

public class PsiPackageAccessibilityStatementImpl extends JavaStubPsiElement<PsiPackageAccessibilityStatementStub> implements PsiPackageAccessibilityStatement
{
	public PsiPackageAccessibilityStatementImpl(@Nonnull PsiPackageAccessibilityStatementStub stub)
	{
		super(stub, stub.getStubType());
	}

	public PsiPackageAccessibilityStatementImpl(@Nonnull ASTNode node)
	{
		super(node);
	}

	@Nonnull
	@Override
	public Role getRole()
	{
		return JavaPackageAccessibilityStatementElementType.typeToRole(getElementType());
	}

	@javax.annotation.Nullable
	@Override
	public PsiJavaCodeReferenceElement getPackageReference()
	{
		return PsiTreeUtil.getChildOfType(this, PsiJavaCodeReferenceElement.class);
	}

	@javax.annotation.Nullable
	@Override
	public String getPackageName()
	{
		PsiPackageAccessibilityStatementStub stub = getGreenStub();
		if(stub != null)
		{
			return StringUtil.nullize(stub.getPackageName());
		}
		else
		{
			PsiJavaCodeReferenceElement ref = getPackageReference();
			return ref != null ? PsiNameHelper.getQualifiedClassName(ref.getText(), true) : null;
		}
	}

	@Nonnull
	@Override
	public Iterable<PsiJavaModuleReferenceElement> getModuleReferences()
	{
		return psiTraverser().children(this).filter(PsiJavaModuleReferenceElement.class);
	}

	@Nonnull
	@Override
	public List<String> getModuleNames()
	{
		PsiPackageAccessibilityStatementStub stub = getGreenStub();
		if(stub != null)
		{
			return stub.getTargets();
		}
		else
		{
			List<String> targets = ContainerUtil.newSmartList();
			for(PsiJavaModuleReferenceElement refElement : getModuleReferences())
			{
				targets.add(refElement.getReferenceText());
			}
			return targets;
		}
	}

	@Override
	public void accept(@Nonnull PsiElementVisitor visitor)
	{
		if(visitor instanceof JavaElementVisitor)
		{
			((JavaElementVisitor) visitor).visitPackageAccessibilityStatement(this);
		}
		else
		{
			visitor.visitElement(this);
		}
	}

	@Override
	public String toString()
	{
		return "PsiPackageAccessibilityStatement";
	}
}