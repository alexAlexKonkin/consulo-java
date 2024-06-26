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
package com.intellij.java.debugger.impl.breakpoints.properties;

import consulo.util.xml.serializer.annotation.Attribute;

import jakarta.annotation.Nullable;

/**
 * @author egor
 */
public class JavaExceptionBreakpointProperties extends JavaBreakpointProperties<JavaExceptionBreakpointProperties>
{
	public boolean NOTIFY_CAUGHT = true;
	public boolean NOTIFY_UNCAUGHT = true;

	@Attribute("class")
	public String myQualifiedName;

	@Attribute("package")
	public String myPackageName;

	public JavaExceptionBreakpointProperties(String qualifiedName, String packageName)
	{
		myQualifiedName = qualifiedName;
		myPackageName = packageName;
	}

	public JavaExceptionBreakpointProperties()
	{
	}

	@Nullable
	@Override
	public JavaExceptionBreakpointProperties getState()
	{
		return this;
	}

	@Override
	public void loadState(JavaExceptionBreakpointProperties state)
	{
		super.loadState(state);

		NOTIFY_CAUGHT = state.NOTIFY_CAUGHT;
		NOTIFY_UNCAUGHT = state.NOTIFY_UNCAUGHT;
		myQualifiedName = state.myQualifiedName;
		myPackageName = state.myPackageName;
	}
}
