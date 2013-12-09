/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import java.util.Arrays;
import java.util.Comparator;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.ClassFileViewProvider;
import com.intellij.psi.stubs.BinaryFileStubBuilder;
import com.intellij.psi.stubs.PsiFileStub;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.cls.ClsFormatException;
import com.intellij.util.indexing.FileContent;

/**
 * @author max
 */
public class ClassFileStubBuilder implements BinaryFileStubBuilder
{
	private static final Logger LOG = Logger.getInstance("#com.intellij.psi.impl.compiled.ClassFileStubBuilder");

	public static final int STUB_VERSION = 7;

	@Override
	public boolean acceptsFile(final VirtualFile file)
	{
		return !ClassFileViewProvider.isInnerClass(file);
	}

	@Override
	public StubElement buildStubTree(FileContent fileContent)
	{
		try
		{
			VirtualFile file = fileContent.getFile();
			Project project = fileContent.getProject();
			byte[] content = fileContent.getContent();
			final ClsStubBuilderFactory[] factories = Extensions.getExtensions(ClsStubBuilderFactory.EP_NAME);
			for(ClsStubBuilderFactory factory : factories)
			{
				if(!factory.isInnerClass(file) && factory.canBeProcessed(file, content))
				{
					PsiFileStub stub = factory.buildFileStub(file, content, project);
					if(stub != null)
					{
						return stub;
					}
				}
			}
			if(!fileContent.getFileName().contains("$"))
			{
				LOG.info("No stub built for file " + fileContent);
			}
			return null;
		}
		catch(ClsFormatException e)
		{
			return null;
		}
	}

	@Override
	public int getStubVersion()
	{
		int version = STUB_VERSION;
		final ClsStubBuilderFactory[] factories = Extensions.getExtensions(ClsStubBuilderFactory.EP_NAME);
		Arrays.sort(factories, new Comparator<ClsStubBuilderFactory>()
		{ // stable order
			@Override
			public int compare(ClsStubBuilderFactory o1, ClsStubBuilderFactory o2)
			{
				return o1.getClass().getName().compareTo(o2.getClass().getName());
			}
		});
		for(ClsStubBuilderFactory factory : factories)
		{
			version = version * 31 + factory.getStubVersion() + factory.getClass().getName().hashCode();
		}
		return version;
	}
}
