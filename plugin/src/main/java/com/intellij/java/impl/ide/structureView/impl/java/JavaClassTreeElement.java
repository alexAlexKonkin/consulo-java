// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.java.impl.ide.structureView.impl.java;

import consulo.fileEditor.structureView.StructureViewTreeElement;
import com.intellij.java.language.psi.*;
import consulo.language.psi.*;
import com.intellij.java.language.impl.psi.impl.PsiImplUtil;
import consulo.language.impl.psi.LightElement;
import javax.annotation.Nonnull;

import java.util.*;

/**
 * @author Konstantin Bulenkov
 */
public class JavaClassTreeElement extends JavaClassTreeElementBase<PsiClass>
{
	public JavaClassTreeElement(PsiClass cls, boolean inherited)
	{
		super(inherited, cls);
	}

	/**
	 * @noinspection unused
	 * @deprecated use {@link #JavaClassTreeElement(PsiClass, boolean)}
	 */
	@Deprecated
	public JavaClassTreeElement(PsiClass cls, boolean inherited, Set<PsiClass> parents)
	{
		this(cls, inherited);
	}

	@Override
	@Nonnull
	public Collection<StructureViewTreeElement> getChildrenBase()
	{
		return getClassChildren(getElement());
	}

	static Collection<StructureViewTreeElement> getClassChildren(PsiClass aClass)
	{
		if(aClass == null)
		{
			return Collections.emptyList();
		}

		LinkedHashSet<PsiElement> members = getOwnChildren(aClass);
		List<StructureViewTreeElement> children = new ArrayList<>(members.size());

		for(PsiElement child : members)
		{
			if(!child.isValid())
			{
				continue;
			}
			if(child instanceof PsiClass)
			{
				children.add(new JavaClassTreeElement((PsiClass) child, false));
			}
			else if(child instanceof PsiField)
			{
				children.add(new PsiFieldTreeElement((PsiField) child, false));
			}
			else if(child instanceof PsiMethod)
			{
				children.add(new PsiMethodTreeElement((PsiMethod) child, false));
			}
			else if(child instanceof PsiClassInitializer)
			{
				children.add(new ClassInitializerTreeElement((PsiClassInitializer) child));
			}
		}
		return children;
	}

	static LinkedHashSet<PsiElement> getOwnChildren(@Nonnull PsiClass aClass)
	{
		LinkedHashSet<PsiElement> members = new LinkedHashSet<>();
		addPhysicalElements(aClass.getFields(), members, aClass);
		addPhysicalElements(aClass.getMethods(), members, aClass);
		addPhysicalElements(aClass.getInnerClasses(), members, aClass);
		addPhysicalElements(aClass.getInitializers(), members, aClass);
		return members;
	}

	private static void addPhysicalElements(@Nonnull PsiMember[] elements, @Nonnull Collection<? super PsiElement> to, @Nonnull PsiClass aClass)
	{
		for(PsiMember element : elements)
		{
			PsiElement mirror = PsiImplUtil.handleMirror(element);
			if(mirror instanceof LightElement)
			{
				continue;
			}
			if(mirror instanceof PsiMember && aClass.equals(((PsiMember) mirror).getContainingClass()))
			{
				to.add(mirror);
			}
		}
	}

	@Override
	public String getPresentableText()
	{
		PsiClass o = getElement();
		return o == null ? "" : o.getName();
	}

	@Override
	public boolean isPublic()
	{
		PsiClass o = getElement();
		return o != null && o.getParent() instanceof PsiFile || super.isPublic();
	}
}