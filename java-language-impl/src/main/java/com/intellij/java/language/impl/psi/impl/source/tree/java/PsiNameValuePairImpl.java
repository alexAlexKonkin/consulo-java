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
package com.intellij.java.language.impl.psi.impl.source.tree.java;

import com.intellij.java.language.impl.psi.impl.PsiImplUtil;
import com.intellij.java.language.impl.psi.impl.java.stubs.JavaStubElementTypes;
import com.intellij.java.language.impl.psi.impl.java.stubs.PsiNameValuePairStub;
import com.intellij.java.language.impl.psi.impl.source.JavaStubPsiElement;
import com.intellij.java.language.impl.psi.impl.source.tree.ChildRole;
import com.intellij.java.language.impl.psi.impl.source.tree.ElementType;
import com.intellij.java.language.psi.*;
import com.intellij.java.language.psi.util.MethodSignature;
import com.intellij.java.language.psi.util.MethodSignatureUtil;
import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.impl.psi.SourceTreeToPsiMap;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiReference;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Dmitry Avdeev
 * Date: 7/27/12
 */
public class PsiNameValuePairImpl extends JavaStubPsiElement<PsiNameValuePairStub> implements PsiNameValuePair {

  public PsiNameValuePairImpl(@Nonnull PsiNameValuePairStub stub) {
    super(stub, JavaStubElementTypes.NAME_VALUE_PAIR);
  }

  public PsiNameValuePairImpl(@Nonnull ASTNode node) {
    super(node);
  }

  @Nonnull
  @Override
  public NameValuePairElement getNode() {
    return (NameValuePairElement) super.getNode();
  }

  @Override
  public String getName() {
    PsiNameValuePairStub stub = getStub();
    if (stub == null) {
      PsiIdentifier nameIdentifier = getNameIdentifier();
      return nameIdentifier == null ? null : nameIdentifier.getText();
    } else {
      return stub.getName();
    }
  }

  @Override
  public String getLiteralValue() {
    PsiNameValuePairStub stub = getStub();
    return stub == null ? null : stub.getValue();
  }

  @Override
  public PsiIdentifier getNameIdentifier() {
    ASTNode node = getNode().findChildByRole(ChildRole.NAME);
    return node == null ? null : (PsiIdentifier) node.getPsi();
  }

  @Override
  public PsiAnnotationMemberValue getValue() {
    ASTNode node = getNode().findChildByRole(ChildRole.ANNOTATION_VALUE);
    return node == null ? null : (PsiAnnotationMemberValue) node.getPsi();
  }

  @Nonnull
  @Override
  public PsiAnnotationMemberValue setValue(@Nonnull PsiAnnotationMemberValue newValue) {
    getValue().replace(newValue);
    return getValue();
  }


  @Override
  public PsiReference getReference() {
    return new PsiReference() {
      @Nullable
      private PsiClass getReferencedClass() {
        LOG.assertTrue(getParent() instanceof PsiAnnotationParameterList && getParent().getParent() instanceof PsiAnnotation);
        PsiAnnotation annotation = (PsiAnnotation) getParent().getParent();
        PsiJavaCodeReferenceElement nameRef = annotation.getNameReferenceElement();
        if (nameRef == null) return null;
        PsiElement target = nameRef.resolve();
        return target instanceof PsiClass ? (PsiClass) target : null;
      }

      @Override
      public PsiElement getElement() {
        PsiIdentifier nameIdentifier = getNameIdentifier();
        if (nameIdentifier != null) {
          return nameIdentifier;
        }
        return PsiNameValuePairImpl.this;
      }

      @Override
      public TextRange getRangeInElement() {
        PsiIdentifier id = getNameIdentifier();
        if (id != null) {
          return new TextRange(0, id.getTextLength());
        }
        return TextRange.EMPTY_RANGE;
      }

      @Override
      public PsiElement resolve() {
        PsiClass refClass = getReferencedClass();
        if (refClass == null) return null;
        String name = getName();
        if (name == null) name = PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME;
        MethodSignature signature = MethodSignatureUtil
            .createMethodSignature(name, PsiType.EMPTY_ARRAY, PsiTypeParameter.EMPTY_ARRAY, PsiSubstitutor.EMPTY);
        return MethodSignatureUtil.findMethodBySignature(refClass, signature, false);
      }

      @Override
      @Nonnull
      public String getCanonicalText() {
        String name = getName();
        return name != null ? name : PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME;
      }

      @Override
      public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
        PsiIdentifier nameIdentifier = getNameIdentifier();
        if (nameIdentifier != null) {
          PsiImplUtil.setName(nameIdentifier, newElementName);
        } else if (ElementType.ANNOTATION_MEMBER_VALUE_BIT_SET.contains(getNode().getFirstChildNode().getElementType())) {
          PsiElementFactory factory = JavaPsiFacade.getInstance(getProject()).getElementFactory();
          nameIdentifier = factory.createIdentifier(newElementName);
          addBefore(nameIdentifier, SourceTreeToPsiMap.treeElementToPsi(getNode().getFirstChildNode()));
        }

        return PsiNameValuePairImpl.this;
      }

      @Override
      public PsiElement bindToElement(@Nonnull PsiElement element) throws IncorrectOperationException {
        throw new IncorrectOperationException("Not implemented");
      }

      @Override
      public boolean isReferenceTo(PsiElement element) {
        return element instanceof PsiMethod && element.equals(resolve());
      }

      @Override
      public boolean isSoft() {
        return false;
      }
    };
  }

  @Override
  public final void accept(@Nonnull PsiElementVisitor visitor) {
    if (visitor instanceof JavaElementVisitor) {
      ((JavaElementVisitor) visitor).visitNameValuePair(this);
    } else {
      visitor.visitElement(this);
    }
  }

  @Override
  public String toString() {
    return "PsiNameValuePair";
  }

  private static final Logger LOG = Logger.getInstance(PsiNameValuePairImpl.class);

}
