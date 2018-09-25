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

import static com.intellij.util.ObjectUtil.assertNotNull;

import gnu.trove.THashSet;

import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.ItemPresentationProviders;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.util.AtomicNotNullLazyValue;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.NullableLazyValue;
import com.intellij.openapi.util.VolatileNullableLazyValue;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiClassImplUtil;
import com.intellij.psi.impl.PsiConstantEvaluationHelperImpl;
import com.intellij.psi.impl.PsiImplUtil;
import com.intellij.psi.impl.PsiVariableEx;
import com.intellij.psi.impl.cache.TypeInfo;
import com.intellij.psi.impl.java.stubs.JavaStubElementTypes;
import com.intellij.psi.impl.java.stubs.PsiFieldStub;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.psi.search.SearchScope;
import com.intellij.util.IncorrectOperationException;

public class ClsFieldImpl extends ClsMemberImpl<PsiFieldStub> implements PsiField, PsiVariableEx, ClsModifierListOwner
{
	private final NotNullLazyValue<PsiTypeElement> myTypeElement;
	private final NullableLazyValue<PsiExpression> myInitializer;

	public ClsFieldImpl(@Nonnull PsiFieldStub stub)
	{
		super(stub);
		myTypeElement = new AtomicNotNullLazyValue<PsiTypeElement>()
		{
			@Nonnull
			@Override
			protected PsiTypeElement compute()
			{
				PsiFieldStub stub = getStub();
				String typeText = TypeInfo.createTypeText(stub.getType(false));
				assert typeText != null : stub;
				return new ClsTypeElementImpl(ClsFieldImpl.this, typeText, ClsTypeElementImpl.VARIANCE_NONE);
			}
		};
		myInitializer = new VolatileNullableLazyValue<PsiExpression>()
		{
			@Nullable
			@Override
			protected PsiExpression compute()
			{
				String initializerText = getStub().getInitializerText();
				return initializerText != null && !Comparing.equal(PsiFieldStub.INITIALIZER_TOO_LONG, initializerText) ? ClsParsingUtil.createExpressionFromText(initializerText, getManager(),
						ClsFieldImpl.this) : null;
			}
		};
	}

	@Override
	@Nonnull
	public PsiElement[] getChildren()
	{
		return getChildren(getDocComment(), getModifierList(), getTypeElement(), getNameIdentifier());
	}

	@Override
	public PsiClass getContainingClass()
	{
		return (PsiClass) getParent();
	}

	@Override
	@Nonnull
	public PsiType getType()
	{
		return assertNotNull(getTypeElement()).getType();
	}

	@Override
	public PsiTypeElement getTypeElement()
	{
		return myTypeElement.getValue();
	}

	@Override
	public PsiModifierList getModifierList()
	{
		return assertNotNull(getStub().findChildStubByType(JavaStubElementTypes.MODIFIER_LIST)).getPsi();
	}

	@Override
	public boolean hasModifierProperty(@Nonnull String name)
	{
		return assertNotNull(getModifierList()).hasModifierProperty(name);
	}

	@Override
	public PsiExpression getInitializer()
	{
		return myInitializer.getValue();
	}

	@Override
	public boolean hasInitializer()
	{
		return getInitializer() != null;
	}

	@Override
	public Object computeConstantValue()
	{
		return computeConstantValue(new THashSet<>());
	}

	@Override
	public Object computeConstantValue(Set<PsiVariable> visitedVars)
	{
		if(!hasModifierProperty(PsiModifier.FINAL))
		{
			return null;
		}

		PsiExpression initializer = getInitializer();
		if(initializer == null)
		{
			return null;
		}

		PsiClass containingClass = getContainingClass();
		if(containingClass != null)
		{
			String qName = containingClass.getQualifiedName();
			if("java.lang.Float".equals(qName))
			{
				String name = getName();
				if("POSITIVE_INFINITY".equals(name))
				{
					return Float.POSITIVE_INFINITY;
				}
				if("NEGATIVE_INFINITY".equals(name))
				{
					return Float.NEGATIVE_INFINITY;
				}
				if("NaN".equals(name))
				{
					return Float.NaN;
				}
			}
			else if("java.lang.Double".equals(qName))
			{
				String name = getName();
				if("POSITIVE_INFINITY".equals(name))
				{
					return Double.POSITIVE_INFINITY;
				}
				if("NEGATIVE_INFINITY".equals(name))
				{
					return Double.NEGATIVE_INFINITY;
				}
				if("NaN".equals(name))
				{
					return Double.NaN;
				}
			}
		}

		return PsiConstantEvaluationHelperImpl.computeCastTo(initializer, getType(), visitedVars);
	}

	@Override
	public boolean isDeprecated()
	{
		return getStub().isDeprecated() || PsiImplUtil.isDeprecatedByAnnotation(this);
	}

	@Override
	public void normalizeDeclaration() throws IncorrectOperationException
	{
	}

	@Override
	public void appendMirrorText(int indentLevel, @Nonnull StringBuilder buffer)
	{
		appendText(getDocComment(), indentLevel, buffer, NEXT_LINE);
		appendText(getModifierList(), indentLevel, buffer, "");
		appendText(getTypeElement(), indentLevel, buffer, " ");
		appendText(getNameIdentifier(), indentLevel, buffer);

		PsiExpression initializer = getInitializer();
		if(initializer != null)
		{
			buffer.append(" = ");
			buffer.append(initializer.getText());
		}

		buffer.append(';');
	}

	@Override
	public void setMirror(@Nonnull TreeElement element) throws InvalidMirrorException
	{
		setMirrorCheckingType(element, null);

		PsiField mirror = SourceTreeToPsiMap.treeToPsiNotNull(element);
		setMirrorIfPresent(getDocComment(), mirror.getDocComment());
		setMirror(getModifierList(), mirror.getModifierList());
		setMirror(getTypeElement(), mirror.getTypeElement());
		setMirror(getNameIdentifier(), mirror.getNameIdentifier());
	}

	@Override
	public void accept(@Nonnull PsiElementVisitor visitor)
	{
		if(visitor instanceof JavaElementVisitor)
		{
			((JavaElementVisitor) visitor).visitField(this);
		}
		else
		{
			visitor.visitElement(this);
		}
	}

	@Override
	@Nonnull
	@SuppressWarnings({
			"Duplicates",
			"deprecation"
	})
	public PsiElement getNavigationElement()
	{
		for(ClsCustomNavigationPolicy customNavigationPolicy : Extensions.getExtensions(ClsCustomNavigationPolicy.EP_NAME))
		{
			try
			{
				PsiElement navigationElement = customNavigationPolicy.getNavigationElement(this);
				if(navigationElement != null)
				{
					return navigationElement;
				}
			}
			catch(IndexNotReadyException ignore)
			{
			}
		}

		try
		{
			PsiClass sourceClassMirror = ((ClsClassImpl) getParent()).getSourceMirrorClass();
			PsiElement sourceFieldMirror = sourceClassMirror != null ? sourceClassMirror.findFieldByName(getName(), false) : null;
			return sourceFieldMirror != null ? sourceFieldMirror.getNavigationElement() : this;
		}
		catch(IndexNotReadyException e)
		{
			return this;
		}
	}

	@Override
	public ItemPresentation getPresentation()
	{
		return ItemPresentationProviders.getItemPresentation(this);
	}

	@Override
	public void setInitializer(PsiExpression initializer) throws IncorrectOperationException
	{
		throw new IncorrectOperationException();
	}

	@Override
	public boolean isEquivalentTo(final PsiElement another)
	{
		return PsiClassImplUtil.isFieldEquivalentTo(this, another);
	}

	@Override
	@Nonnull
	public SearchScope getUseScope()
	{
		return PsiImplUtil.getMemberUseScope(this);
	}

	@Override
	public String toString()
	{
		return "PsiField:" + getName();
	}
}