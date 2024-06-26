/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.java.language.impl.psi.impl.compiled;

import com.intellij.java.language.psi.JavaElementVisitor;
import com.intellij.java.language.psi.PsiAnnotationMemberValue;
import com.intellij.java.language.psi.PsiArrayInitializerMemberValue;
import consulo.language.impl.ast.TreeElement;
import consulo.language.impl.psi.SourceTreeToPsiMap;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;

import jakarta.annotation.Nonnull;

/**
 * @author ven
 */
public class ClsArrayInitializerMemberValueImpl extends ClsElementImpl implements PsiArrayInitializerMemberValue {
  private final ClsElementImpl myParent;
  private final PsiAnnotationMemberValue[] myInitializers;

  public ClsArrayInitializerMemberValueImpl(@Nonnull ClsElementImpl parent, @Nonnull PsiAnnotationMemberValue[] initializers) {
    myParent = parent;
    myInitializers = initializers;
  }

  @Override
  public String getText() {
    final StringBuilder buffer = new StringBuilder();
    appendMirrorText(0, buffer);
    return buffer.toString();
  }

  @Override
  public void appendMirrorText(int indentLevel, @Nonnull StringBuilder buffer) {
    buffer.append('{');
    for (int i = 0; i < myInitializers.length; i++) {
      if (i > 0) buffer.append(", ");
      appendText(myInitializers[i], 0, buffer);
    }
    buffer.append('}');
  }

  @Override
  public void setMirror(@Nonnull TreeElement element) throws InvalidMirrorException {
    setMirrorCheckingType(element, null);
    setMirrors(getInitializers(), SourceTreeToPsiMap.<PsiArrayInitializerMemberValue>treeToPsiNotNull(element).getInitializers());
  }

  @Override
  @Nonnull
  public PsiElement[] getChildren() {
    return myInitializers;
  }

  @Override
  public PsiElement getParent() {
    return myParent;
  }

  @Override
  public void accept(@Nonnull PsiElementVisitor visitor) {
    if (visitor instanceof JavaElementVisitor) {
      ((JavaElementVisitor) visitor).visitAnnotationArrayInitializer(this);
    } else {
      visitor.visitElement(this);
    }
  }

  @Override
  @Nonnull
  public PsiAnnotationMemberValue[] getInitializers() {
    return myInitializers;
  }
}
