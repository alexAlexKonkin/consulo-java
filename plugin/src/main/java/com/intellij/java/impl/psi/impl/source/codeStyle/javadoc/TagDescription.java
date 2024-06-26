/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.java.impl.psi.impl.source.codeStyle.javadoc;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Dmitry Skavish
 */
public class TagDescription
{
	@Nonnull
	public final String name;
	@Nullable
	public final String desc;

	public TagDescription(@Nonnull String name, @Nullable String desc)
	{
		this.name = name;
		this.desc = desc;
	}

	@Nonnull
	@Override
	public String toString()
	{
		return name;
	}
}
