/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
import com.intellij.openapi.util.AtomicNotNullLazyValue;
import com.intellij.openapi.util.AtomicNullableLazyValue;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.NullableLazyValue;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiImplUtil;
import com.intellij.psi.impl.PsiJavaParserFacadeImpl;
import com.intellij.psi.impl.cache.TypeInfo;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.impl.source.tree.JavaElementType;
import com.intellij.psi.impl.source.tree.TreeElement;

public class ClsTypeElementImpl extends ClsElementImpl implements PsiTypeElement
{
	@NonNls
	static final char VARIANCE_NONE = '\0';
	@NonNls
	static final char VARIANCE_EXTENDS = '+';
	@NonNls
	static final char VARIANCE_SUPER = '-';
	@NonNls
	static final char VARIANCE_INVARIANT = '*';
	@NonNls
	static final String VARIANCE_EXTENDS_PREFIX = "? extends ";
	@NonNls
	static final String VARIANCE_SUPER_PREFIX = "? super ";

	private final PsiElement myParent;
	private final String myTypeText;
	private final char myVariance;
	private final NullableLazyValue<ClsElementImpl> myChild;
	private final NotNullLazyValue<PsiType> myCachedType;

	public ClsTypeElementImpl(@Nonnull PsiElement parent, @Nonnull String typeText, char variance)
	{
		myParent = parent;
		myTypeText = TypeInfo.internFrequentType(typeText);
		myVariance = variance;
		myChild = new AtomicNullableLazyValue<ClsElementImpl>()
		{
			@Override
			protected ClsElementImpl compute()
			{
				return calculateChild();
			}
		};
		myCachedType = new AtomicNotNullLazyValue<PsiType>()
		{
			@Nonnull
			@Override
			protected PsiType compute()
			{
				return calculateType();
			}
		};
	}

	@Override
	@Nonnull
	public PsiElement[] getChildren()
	{
		ClsElementImpl child = myChild.getValue();
		return child != null ? new PsiElement[]{child} : PsiElement.EMPTY_ARRAY;
	}

	@Override
	public PsiElement getParent()
	{
		return myParent;
	}

	@Override
	public String getText()
	{
		final String shortClassName = PsiNameHelper.getShortClassName(myTypeText);
		return decorateTypeText(shortClassName);
	}

	private String decorateTypeText(final String shortClassName)
	{
		switch(myVariance)
		{
			case VARIANCE_NONE:
				return shortClassName;
			case VARIANCE_EXTENDS:
				return VARIANCE_EXTENDS_PREFIX + shortClassName;
			case VARIANCE_SUPER:
				return VARIANCE_SUPER_PREFIX + shortClassName;
			case VARIANCE_INVARIANT:
				return "?";
			default:
				assert false : myVariance;
				return null;
		}
	}

	public String getCanonicalText()
	{
		return decorateTypeText(myTypeText);
	}

	@Override
	public void appendMirrorText(int indentLevel, @Nonnull StringBuilder buffer)
	{
		buffer.append(decorateTypeText(myTypeText));
	}

	@Override
	public void setMirror(@Nonnull TreeElement element) throws InvalidMirrorException
	{
		setMirrorCheckingType(element, JavaElementType.TYPE);

		ClsElementImpl child = myChild.getValue();
		if(child != null)
		{
			child.setMirror(element.getFirstChildNode());
		}
	}

	private boolean isArray()
	{
		return myTypeText.endsWith("[]");
	}

	private boolean isVarArgs()
	{
		return myTypeText.endsWith("...");
	}

	@Override
	@Nonnull
	public PsiType getType()
	{
		return myCachedType.getValue();
	}

	@Override
	public PsiJavaCodeReferenceElement getInnermostComponentReferenceElement()
	{
		return null;
	}

	private ClsElementImpl calculateChild()
	{
		if(PsiJavaParserFacadeImpl.getPrimitiveType(myTypeText) != null)
		{
			return null;
		}
		if(isArray())
		{
			return myVariance == VARIANCE_NONE ? new ClsTypeElementImpl(this, myTypeText.substring(0, myTypeText.length() - 2),
					myVariance) : new ClsTypeElementImpl(this, myTypeText, VARIANCE_NONE);
		}
		if(isVarArgs())
		{
			return new ClsTypeElementImpl(this, myTypeText.substring(0, myTypeText.length() - 3), myVariance);
		}
		return myVariance == VARIANCE_INVARIANT ? null : new ClsJavaCodeReferenceElementImpl(this, myTypeText);
	}

	@Nonnull
	private PsiType calculateType()
	{
		PsiType result = PsiJavaParserFacadeImpl.getPrimitiveType(myTypeText);
		if(result != null)
		{
			return result;
		}

		ClsElementImpl childElement = myChild.getValue();
		if(childElement instanceof ClsTypeElementImpl)
		{
			if(isArray())
			{
				switch(myVariance)
				{
					case VARIANCE_NONE:
						return ((PsiTypeElement) childElement).getType().createArrayType();
					case VARIANCE_EXTENDS:
						return PsiWildcardType.createExtends(getManager(), ((PsiTypeElement) childElement).getType());
					case VARIANCE_SUPER:
						return PsiWildcardType.createSuper(getManager(), ((PsiTypeElement) childElement).getType());
					default:
						assert false : myVariance;
						return null;
				}
			}
			else
			{
				assert isVarArgs() : this;
				return new PsiEllipsisType(((PsiTypeElement) childElement).getType());
			}
		}
		if(childElement instanceof ClsJavaCodeReferenceElementImpl)
		{
			PsiClassReferenceType psiClassReferenceType = new PsiClassReferenceType((PsiJavaCodeReferenceElement) childElement, null);
			switch(myVariance)
			{
				case VARIANCE_NONE:
					return psiClassReferenceType;
				case VARIANCE_EXTENDS:
					return PsiWildcardType.createExtends(getManager(), psiClassReferenceType);
				case VARIANCE_SUPER:
					return PsiWildcardType.createSuper(getManager(), psiClassReferenceType);
				case VARIANCE_INVARIANT:
					return PsiWildcardType.createUnbounded(getManager());
				default:
					assert false : myVariance;
					return null;
			}
		}
		assert childElement == null : this;
		return PsiWildcardType.createUnbounded(getManager());
	}

	@Override
	public void accept(@Nonnull PsiElementVisitor visitor)
	{
		if(visitor instanceof JavaElementVisitor)
		{
			((JavaElementVisitor) visitor).visitTypeElement(this);
		}
		else
		{
			visitor.visitElement(this);
		}
	}

	@Override
	@Nonnull
	public PsiAnnotation[] getAnnotations()
	{
		throw new UnsupportedOperationException();//todo
	}

	@Override
	public PsiAnnotation findAnnotation(@Nonnull @NonNls String qualifiedName)
	{
		return PsiImplUtil.findAnnotation(this, qualifiedName);
	}

	@Override
	@Nonnull
	public PsiAnnotation addAnnotation(@Nonnull @NonNls String qualifiedName)
	{
		throw new UnsupportedOperationException();//todo
	}

	@Override
	@Nonnull
	public PsiAnnotation[] getApplicableAnnotations()
	{
		return getAnnotations();
	}

	@Override
	public String toString()
	{
		return "PsiTypeElement:" + getText();
	}
}
