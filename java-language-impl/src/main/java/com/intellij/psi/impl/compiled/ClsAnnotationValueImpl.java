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
package com.intellij.psi.impl.compiled;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NonNls;

import javax.annotation.Nullable;
import com.intellij.pom.Navigatable;
import com.intellij.java.language.psi.JavaElementVisitor;
import com.intellij.java.language.psi.PsiAnnotation;
import com.intellij.java.language.psi.PsiAnnotationMemberValue;
import com.intellij.java.language.psi.PsiAnnotationParameterList;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.java.language.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.impl.PsiImplUtil;
import com.intellij.psi.impl.meta.MetaRegistry;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.psi.meta.PsiMetaData;

/**
 * @author ven
 */
abstract class ClsAnnotationValueImpl extends ClsElementImpl implements PsiAnnotation, Navigatable
{
	private final ClsElementImpl myParent;
	private final ClsJavaCodeReferenceElementImpl myReferenceElement;
	private final ClsAnnotationParameterListImpl myParameterList;

	@SuppressWarnings("AbstractMethodCallInConstructor")
	ClsAnnotationValueImpl(@Nonnull ClsElementImpl parent)
	{
		myParent = parent;
		myReferenceElement = createReference();
		myParameterList = createParameterList();
	}

	protected abstract ClsAnnotationParameterListImpl createParameterList();

	protected abstract ClsJavaCodeReferenceElementImpl createReference();

	@Override
	public void appendMirrorText(int indentLevel, @Nonnull StringBuilder buffer)
	{
		buffer.append("@").append(myReferenceElement.getCanonicalText());
		myParameterList.appendMirrorText(indentLevel, buffer);
	}

	@Override
	public void setMirror(@Nonnull TreeElement element) throws InvalidMirrorException
	{
		setMirrorCheckingType(element, null);

		PsiAnnotation mirror = SourceTreeToPsiMap.treeToPsiNotNull(element);
		setMirror(getNameReferenceElement(), mirror.getNameReferenceElement());
		setMirror(getParameterList(), mirror.getParameterList());
	}

	@Override
	@Nonnull
	public PsiElement[] getChildren()
	{
		return new PsiElement[]{
				myReferenceElement,
				myParameterList
		};
	}

	@Override
	public PsiElement getParent()
	{
		return myParent;
	}

	@Override
	public void accept(@Nonnull PsiElementVisitor visitor)
	{
		if(visitor instanceof JavaElementVisitor)
		{
			((JavaElementVisitor) visitor).visitAnnotation(this);
		}
		else
		{
			visitor.visitElement(this);
		}
	}

	@Override
	@Nonnull
	public PsiAnnotationParameterList getParameterList()
	{
		return myParameterList;
	}

	@Override
	@javax.annotation.Nullable
	public String getQualifiedName()
	{
		return myReferenceElement != null ? myReferenceElement.getCanonicalText() : null;
	}

	@Override
	public PsiJavaCodeReferenceElement getNameReferenceElement()
	{
		return myReferenceElement;
	}

	@Override
	public PsiAnnotationMemberValue findAttributeValue(String attributeName)
	{
		return PsiImplUtil.findAttributeValue(this, attributeName);
	}

	@Override
	@Nullable
	public PsiAnnotationMemberValue findDeclaredAttributeValue(@NonNls final String attributeName)
	{
		return PsiImplUtil.findDeclaredAttributeValue(this, attributeName);
	}

	@Override
	public <T extends PsiAnnotationMemberValue> T setDeclaredAttributeValue(@NonNls String attributeName, T value)
	{
		throw cannotModifyException(this);
	}

	@Override
	public String getText()
	{
		final StringBuilder buffer = new StringBuilder();
		appendMirrorText(0, buffer);
		return buffer.toString();
	}

	@Override
	public PsiMetaData getMetaData()
	{
		return MetaRegistry.getMetaBase(this);
	}
}
