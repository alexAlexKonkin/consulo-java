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

/*
 * @author max
 */
package com.intellij.psi.impl.java.stubs.index;

import java.util.Collection;

import javax.annotation.Nonnull;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.impl.search.JavaSourceFilterScope;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.psi.stubs.StubIndexKey;

public class JavaAnnotationIndex extends StringStubIndexExtension<PsiAnnotation>
{
	private static final JavaAnnotationIndex ourInstance = new JavaAnnotationIndex();

	public static JavaAnnotationIndex getInstance()
	{
		return ourInstance;
	}

	@Nonnull
	@Override
	public StubIndexKey<String, PsiAnnotation> getKey()
	{
		return JavaStubIndexKeys.ANNOTATIONS;
	}

	@Override
	public Collection<PsiAnnotation> get(@Nonnull final String s, @Nonnull final Project project, @Nonnull final GlobalSearchScope scope)
	{
		return StubIndex.getElements(getKey(), s, project, new JavaSourceFilterScope(scope), PsiAnnotation.class);
	}
}