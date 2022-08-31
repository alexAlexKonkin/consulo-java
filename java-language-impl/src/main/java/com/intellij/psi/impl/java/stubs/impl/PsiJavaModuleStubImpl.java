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

import com.intellij.java.language.psi.PsiJavaModule;
import com.intellij.psi.impl.java.stubs.JavaStubElementTypes;
import com.intellij.psi.impl.java.stubs.PsiJavaModuleStub;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;

public class PsiJavaModuleStubImpl extends StubBase<PsiJavaModule> implements PsiJavaModuleStub
{
	private final String myName;

	public PsiJavaModuleStubImpl(StubElement parent, String name)
	{
		super(parent, JavaStubElementTypes.MODULE);
		myName = name;
	}

	@Override
	public String getName()
	{
		return myName;
	}

	@Override
	public String toString()
	{
		return "PsiJavaModuleStub:" + getName();
	}
}