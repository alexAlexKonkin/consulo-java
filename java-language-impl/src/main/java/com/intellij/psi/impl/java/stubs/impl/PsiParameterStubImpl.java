// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.impl.java.stubs.impl;

import com.intellij.java.language.psi.PsiParameter;
import com.intellij.psi.impl.cache.TypeInfo;
import com.intellij.psi.impl.java.stubs.JavaStubElementTypes;
import com.intellij.psi.impl.java.stubs.PsiModifierListStub;
import com.intellij.psi.impl.java.stubs.PsiParameterStub;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.BitUtil;
import javax.annotation.Nonnull;

public class PsiParameterStubImpl extends StubBase<PsiParameter> implements PsiParameterStub
{
	private final static byte ELLIPSIS = 0x01;
	private final static byte GENERATED_NAME = 0x02;

	private static byte packFlags(boolean isEllipsis, boolean generatedName)
	{
		byte flags = 0;
		flags = BitUtil.set(flags, ELLIPSIS, isEllipsis);
		flags = BitUtil.set(flags, GENERATED_NAME, generatedName);
		return flags;
	}

	private String myName;
	private final TypeInfo myType;
	private byte myFlags;

	public PsiParameterStubImpl(StubElement parent, @Nonnull String name, @Nonnull TypeInfo type, boolean ellipsis, boolean generatedName)
	{
		this(parent, name, type, packFlags(ellipsis, generatedName));
	}

	public PsiParameterStubImpl(StubElement parent, @Nonnull String name, @Nonnull TypeInfo type, byte flags)
	{
		super(parent, JavaStubElementTypes.PARAMETER);
		myName = name;
		myType = type;
		myFlags = flags;
	}

	@Override
	public boolean isParameterTypeEllipsis()
	{
		return BitUtil.isSet(myFlags, ELLIPSIS);
	}

	@Override
	@Nonnull
	public TypeInfo getType()
	{
		return myType;
	}

	@Override
	public PsiModifierListStub getModList()
	{
		for(StubElement child : getChildrenStubs())
		{
			if(child instanceof PsiModifierListStub)
			{
				return (PsiModifierListStub) child;
			}
		}
		return null;
	}

	@Nonnull
	@Override
	public String getName()
	{
		return myName;
	}

	public void setName(String name)
	{
		myName = name;
		myFlags = BitUtil.set(myFlags, GENERATED_NAME, false);
	}

	public boolean isAutoGeneratedName()
	{
		return BitUtil.isSet(myFlags, GENERATED_NAME);
	}

	public byte getFlags()
	{
		return myFlags;
	}

	@Override
	public String toString()
	{
		return "PsiParameterStub[" + myName + ':' + myType + ']';
	}
}