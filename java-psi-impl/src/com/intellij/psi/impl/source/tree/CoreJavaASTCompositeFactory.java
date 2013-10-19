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

import com.intellij.lang.ASTCompositeFactory;
import com.intellij.psi.impl.source.Constants;
import com.intellij.psi.impl.source.javadoc.CorePsiDocTagValueImpl;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2:32/02.04.13
 */
public class CoreJavaASTCompositeFactory implements ASTCompositeFactory, Constants {
  @NotNull
  @Override
  public CompositeElement createComposite(IElementType type) {
    return new CorePsiDocTagValueImpl();
  }

  @Override
  public boolean apply(@Nullable IElementType input) {
    return input == DOC_TAG_VALUE_ELEMENT;
  }
}
