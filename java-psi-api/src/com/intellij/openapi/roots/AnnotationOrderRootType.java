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
package com.intellij.openapi.roots;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import consulo.roots.OrderEntryWithTracking;

/**
 * @author yole
 */
public class AnnotationOrderRootType extends OrderRootType
{
	@NotNull
	public static OrderRootType getInstance()
	{
		return getOrderRootType(AnnotationOrderRootType.class);
	}

	public AnnotationOrderRootType()
	{
		super("javaExternalAnnotations");
	}

	@NotNull
	public static VirtualFile[] getFiles(@NotNull OrderEntry entry)
	{
		List<VirtualFile> result = new ArrayList<VirtualFile>();
		RootPolicy<List<VirtualFile>> policy = new RootPolicy<List<VirtualFile>>()
		{
			@Override
			public List<VirtualFile> visitOrderEntry(OrderEntry orderEntry, List<VirtualFile> value)
			{
				if(orderEntry instanceof OrderEntryWithTracking)
				{
					Collections.addAll(value, orderEntry.getFiles(getInstance()));
				}
				return value;
			}
		};
		entry.accept(policy, result);
		return VfsUtilCore.toVirtualFileArray(result);
	}

	@NotNull
	public static String[] getUrls(@NotNull OrderEntry entry)
	{
		List<String> result = new ArrayList<String>();
		RootPolicy<List<String>> policy = new RootPolicy<List<String>>()
		{
			@Override
			public List<String> visitOrderEntry(OrderEntry orderEntry, List<String> value)
			{
				if(orderEntry instanceof OrderEntryWithTracking)
				{
					Collections.addAll(value, orderEntry.getUrls(getInstance()));
				}
				return value;
			}
		};
		entry.accept(policy, result);
		return ArrayUtil.toStringArray(result);
	}
}
