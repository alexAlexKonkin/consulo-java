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
package com.intellij.java.language.impl.psi.impl.source.tree.java;

import com.intellij.java.language.psi.JavaElementVisitor;
import consulo.language.psi.PsiElementVisitor;
import com.intellij.java.language.psi.PsiJavaToken;
import consulo.language.impl.psi.LeafPsiElement;
import consulo.language.ast.IElementType;
import jakarta.annotation.Nonnull;

public class PsiJavaTokenImpl extends LeafPsiElement implements PsiJavaToken{
  public PsiJavaTokenImpl(IElementType type, CharSequence text) {
    super(type, text);
  }

  @Override
  public IElementType getTokenType() {
    return getElementType();
  }

  @Override
  public void accept(@Nonnull PsiElementVisitor visitor) {
    if (visitor instanceof JavaElementVisitor) {
      ((JavaElementVisitor)visitor).visitJavaToken(this);
    }
    else {
      visitor.visitElement(this);
    }
  }

  public String toString(){
    return "PsiJavaToken:" + getElementType().toString();
  }
}
