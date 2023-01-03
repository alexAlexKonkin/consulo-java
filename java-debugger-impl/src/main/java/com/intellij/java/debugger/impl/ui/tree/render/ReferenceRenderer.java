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
package com.intellij.java.debugger.impl.ui.tree.render;

import javax.annotation.Nonnull;
import com.intellij.java.debugger.engine.DebuggerUtils;
import consulo.internal.com.sun.jdi.ReferenceType;
import consulo.internal.com.sun.jdi.Type;

public abstract class ReferenceRenderer extends TypeRenderer
{
	protected ReferenceRenderer()
	{
	}

	protected ReferenceRenderer(@Nonnull String className)
	{
		super(className);
	}

	public boolean isApplicable(Type type)
	{
		return type instanceof ReferenceType && DebuggerUtils.instanceOf(type, getClassName());
	}
}