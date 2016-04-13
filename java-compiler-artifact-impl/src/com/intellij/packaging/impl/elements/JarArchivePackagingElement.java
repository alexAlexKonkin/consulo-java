/*
 * Copyright 2013 Consulo.org
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
package com.intellij.packaging.impl.elements;

import org.jetbrains.annotations.NotNull;
import com.intellij.packaging.elements.ArchivePackageWriter;

/**
 * @author VISTALL
 * @since 16:05/18.06.13
 */
public class JarArchivePackagingElement extends ArchivePackagingElement
{
	public JarArchivePackagingElement()
	{
		super(JarArchiveElementType.getInstance());
	}

	public JarArchivePackagingElement(@NotNull String archiveFileName)
	{
		super(JarArchiveElementType.getInstance(), archiveFileName);
	}

	@Override
	public ArchivePackageWriter<?> getPackageWriter()
	{
		// use zip - later write own with manifect correction
		return ZipArchivePackagingElement.ZipArchivePackageWriter.INSTANCE;
	}
}
