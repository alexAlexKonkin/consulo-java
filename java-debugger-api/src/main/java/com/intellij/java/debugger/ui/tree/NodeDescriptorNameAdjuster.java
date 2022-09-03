/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.java.debugger.ui.tree;

import consulo.component.extension.ExtensionPointName;

import javax.annotation.Nonnull;

/**
 * @author Nikolay.Tropin
 */
public abstract class NodeDescriptorNameAdjuster
{
	public static ExtensionPointName<NodeDescriptorNameAdjuster> EP_NAME = ExtensionPointName.create("consulo.java.debugger.nodeNameAdjuster");

	public abstract boolean isApplicable(@Nonnull NodeDescriptor descriptor);

	public abstract String fixName(String name, @Nonnull NodeDescriptor descriptor);

	public static NodeDescriptorNameAdjuster findFor(@Nonnull NodeDescriptor descriptor)
	{
		for(NodeDescriptorNameAdjuster adjuster : EP_NAME.getExtensions())
		{
			if(adjuster.isApplicable(descriptor))
			{
				return adjuster;
			}
		}
		return null;
	}
}
