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

import com.intellij.java.language.psi.*;
import consulo.language.impl.ast.TreeElement;
import consulo.language.impl.psi.SourceTreeToPsiMap;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import jakarta.annotation.Nonnull;

/**
 * @author ven
 */
public class ClsAnnotationParameterListImpl extends ClsElementImpl implements PsiAnnotationParameterList {
  private final PsiAnnotation myParent;
  private final ClsNameValuePairImpl[] myAttributes;

  public ClsAnnotationParameterListImpl(@Nonnull PsiAnnotation parent, @Nonnull PsiNameValuePair[] psiAttributes) {
    myParent = parent;
    myAttributes = new ClsNameValuePairImpl[psiAttributes.length];
    for (int i = 0; i < myAttributes.length; i++) {
      String name = psiAttributes[i].getName();
      PsiAnnotationMemberValue value = psiAttributes[i].getValue();
      assert value != null : "name=" + name + " value" + value;
      myAttributes[i] = new ClsNameValuePairImpl(this, name, value);
    }
  }

  @Override
  public void appendMirrorText(int indentLevel, @Nonnull StringBuilder buffer) {
    if (myAttributes.length != 0) {
      buffer.append("(");
      for (int i = 0; i < myAttributes.length; i++) {
        if (i > 0) buffer.append(", ");
        myAttributes[i].appendMirrorText(indentLevel, buffer);
      }
      buffer.append(")");
    }
  }

  @Override
  public void setMirror(@Nonnull TreeElement element) throws InvalidMirrorException {
    setMirrorCheckingType(element, null);
    setMirrors(myAttributes, SourceTreeToPsiMap.<PsiAnnotationParameterList>treeToPsiNotNull(element).getAttributes());
  }

  @Override
  @Nonnull
  public PsiElement[] getChildren() {
    return myAttributes;
  }

  @Override
  public PsiElement getParent() {
    return myParent;
  }

  @Override
  public void accept(@Nonnull PsiElementVisitor visitor) {
    if (visitor instanceof JavaElementVisitor) {
      ((JavaElementVisitor) visitor).visitAnnotationParameterList(this);
    } else {
      visitor.visitElement(this);
    }
  }

  @Override
  @Nonnull
  public PsiNameValuePair[] getAttributes() {
    return myAttributes;
  }
}
