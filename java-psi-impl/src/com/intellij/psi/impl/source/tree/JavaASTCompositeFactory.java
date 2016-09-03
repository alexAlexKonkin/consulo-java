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
package com.intellij.psi.impl.source.tree;

import com.intellij.psi.impl.source.javadoc.PsiDocTagValueImpl;
import com.intellij.psi.tree.IElementType;
import consulo.java.psi.impl.source.tree.CoreJavaASTCompositeFactory;
import org.jetbrains.annotations.NotNull;

/**
 * @author max
 */
public class JavaASTCompositeFactory extends CoreJavaASTCompositeFactory
{
  @Override
  @NotNull
  public CompositeElement createComposite(final IElementType type) {
    if (type == DOC_TAG_VALUE_ELEMENT) {
      return new PsiDocTagValueImpl();
    }

    return null;
  }
}
