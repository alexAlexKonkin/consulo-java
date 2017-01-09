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
package com.intellij.psi.impl.java.stubs.impl;

import org.jetbrains.annotations.NotNull;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.impl.cache.TypeInfo;
import com.intellij.psi.impl.java.stubs.JavaStubElementTypes;
import com.intellij.psi.impl.java.stubs.PsiModifierListStub;
import com.intellij.psi.impl.java.stubs.PsiParameterStub;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.BitUtil;

/**
 * @author max
 */
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

	public PsiParameterStubImpl(StubElement parent, @NotNull String name, @NotNull TypeInfo type, boolean ellipsis, boolean generatedName)
	{
		this(parent, name, type, packFlags(ellipsis, generatedName));
	}

	public PsiParameterStubImpl(StubElement parent, @NotNull String name, @NotNull TypeInfo type, byte flags)
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
	@NotNull
	public TypeInfo getType(boolean doResolve)
	{
		return doResolve ? myType.applyAnnotations(this) : myType;
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

	@NotNull
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