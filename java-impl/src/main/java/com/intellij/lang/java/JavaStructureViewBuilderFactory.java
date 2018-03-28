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

/*
 * @author max
 */
package com.intellij.lang.java;

import javax.annotation.Nonnull;

import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder;
import com.intellij.ide.structureView.impl.java.JavaFileTreeModel;
import com.intellij.lang.PsiStructureViewFactory;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;

public class JavaStructureViewBuilderFactory implements PsiStructureViewFactory
{
	@Override
	@javax.annotation.Nullable
	public StructureViewBuilder getStructureViewBuilder(final PsiFile psiFile)
	{
		if(!(psiFile instanceof PsiJavaFile))
		{
			return null;
		}
		return new TreeBasedStructureViewBuilder()
		{
			@Override
			@Nonnull
			public StructureViewModel createStructureViewModel(@javax.annotation.Nullable Editor editor)
			{
				return new JavaFileTreeModel((PsiJavaFile) psiFile, editor);
			}

			@Override
			public boolean isRootNodeShown()
			{
				return false;
			}
		};
	}
}