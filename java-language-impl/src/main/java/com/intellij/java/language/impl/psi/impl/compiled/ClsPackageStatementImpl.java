/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.java.language.impl.psi.impl.compiled;

import jakarta.annotation.Nonnull;

import com.intellij.java.language.psi.JavaElementVisitor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import com.intellij.java.language.psi.PsiJavaCodeReferenceElement;
import com.intellij.java.language.psi.PsiModifierList;
import com.intellij.java.language.psi.PsiPackageStatement;
import com.intellij.java.language.impl.psi.impl.source.tree.JavaElementType;
import consulo.language.impl.ast.TreeElement;

class ClsPackageStatementImpl extends ClsElementImpl implements PsiPackageStatement
{
	static ClsPackageStatementImpl NULL_PACKAGE = new ClsPackageStatementImpl();

	private final ClsFileImpl myFile;
	private final String myPackageName;

	private ClsPackageStatementImpl()
	{
		myFile = null;
		myPackageName = null;
	}

	public ClsPackageStatementImpl(@Nonnull ClsFileImpl file, String packageName)
	{
		myFile = file;
		myPackageName = packageName;
	}

	@Override
	public PsiElement getParent()
	{
		return myFile;
	}

	@Override
	public PsiJavaCodeReferenceElement getPackageReference()
	{
		throw new UnsupportedOperationException("Method not implemented");
	}

	@Override
	public PsiModifierList getAnnotationList()
	{
		throw new UnsupportedOperationException("Method not implemented");
	}

	@Override
	@Nonnull
	public PsiElement[] getChildren()
	{
		throw new UnsupportedOperationException("Method not implemented");
	}

	@Override
	public String getPackageName()
	{
		return myPackageName;
	}

	@Override
	public void appendMirrorText(final int indentLevel, @Nonnull final StringBuilder buffer)
	{
		if(myPackageName != null)
		{
			buffer.append("package ").append(getPackageName()).append(';');
		}
	}

	@Override
	public void setMirror(@Nonnull TreeElement element) throws InvalidMirrorException
	{
		setMirrorCheckingType(element, JavaElementType.PACKAGE_STATEMENT);
	}

	@Override
	public void accept(@Nonnull PsiElementVisitor visitor)
	{
		if(visitor instanceof JavaElementVisitor)
		{
			((JavaElementVisitor) visitor).visitPackageStatement(this);
		}
		else
		{
			visitor.visitElement(this);
		}
	}

	@Override
	public String toString()
	{
		return "PsiPackageStatement:" + getPackageName();
	}
}