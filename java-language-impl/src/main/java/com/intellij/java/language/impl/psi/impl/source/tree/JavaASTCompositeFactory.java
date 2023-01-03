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
package com.intellij.java.language.impl.psi.impl.source.tree;

import com.intellij.java.language.impl.psi.impl.source.javadoc.PsiDocTagValueImpl;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.impl.ast.CompositeElement;
import consulo.language.ast.IElementType;
import consulo.java.language.impl.psi.CoreJavaASTCompositeFactory;

import javax.annotation.Nonnull;

/**
 * @author max
 */
@ExtensionImpl
public class JavaASTCompositeFactory extends CoreJavaASTCompositeFactory {
  @Override
  @Nonnull
  public CompositeElement createComposite(final IElementType type) {
    if (type == DOC_TAG_VALUE_ELEMENT) {
      return new PsiDocTagValueImpl();
    }

    return null;
  }
}
