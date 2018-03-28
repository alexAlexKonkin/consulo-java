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
package com.intellij.codeInsight.javadoc;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.PsiDocCommentOwner;
import com.intellij.psi.PsiMember;

public abstract class DocumentationDelegateProvider
{

	public static final ExtensionPointName<DocumentationDelegateProvider> EP_NAME = ExtensionPointName.create("consulo.java.documentationDelegateProvider");

	/**
	 * <p>
	 * Computes PsiDocCommentOwner to get documentation from.
	 * </p>
	 * <p>
	 * Suppose there is a {@code Foo#bar()} with doc and {@code Baz#bar()} without doc:
	 * <pre>
	 * class Foo {
	 *   /**
	 *   * Some javadoc
	 *   *&#47;
	 *   void bar() {}
	 * }
	 * class Baz {
	 *   void bar() {}
	 * }
	 * </pre>
	 * If it is needed to display doc from {@code Foo#bar()} when doc for {@code Baz#bar()} is queried
	 * then this method should return PsiMethod corresponding to {@code Foo#bar()} for PsiMethod corresponding to {@code Baz#bar()}.
	 * <br>
	 * The copied documentation will include <i>Description copied from</i> link.
	 * </p>
	 *
	 * @param member method to search delegate for.
	 * @return delegate PsiDocCommentOwner instance.
	 */
	@Nullable
	public abstract PsiDocCommentOwner computeDocumentationDelegate(@Nonnull PsiMember member);

	@Nullable
	public static PsiDocCommentOwner findDocumentationDelegate(@Nonnull PsiMember method)
	{
		for(DocumentationDelegateProvider delegator : EP_NAME.getExtensions())
		{
			PsiDocCommentOwner type = delegator.computeDocumentationDelegate(method);
			if(type != null)
			{
				return type;
			}
		}
		return null;
	}
}
