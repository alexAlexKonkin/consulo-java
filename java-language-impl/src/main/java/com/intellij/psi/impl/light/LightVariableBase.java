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
package com.intellij.psi.impl.light;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.intellij.codeInsight.completion.originInfo.OriginInfoAwareElement;
import com.intellij.lang.Language;
import com.intellij.java.language.JavaLanguage;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.java.language.psi.JavaPsiFacade;
import com.intellij.psi.PsiElement;
import com.intellij.java.language.psi.PsiExpression;
import com.intellij.java.language.psi.PsiIdentifier;
import com.intellij.psi.PsiManager;
import com.intellij.java.language.psi.PsiModifierList;
import com.intellij.java.language.psi.PsiType;
import com.intellij.java.language.psi.PsiTypeElement;
import com.intellij.java.language.psi.PsiVariable;
import com.intellij.psi.impl.PsiImplUtil;
import com.intellij.util.IncorrectOperationException;

/**
 * @author ven
 */
public abstract class LightVariableBase extends LightElement implements PsiVariable, OriginInfoAwareElement {
  protected PsiElement myScope;
  protected PsiIdentifier myNameIdentifier;
  protected PsiType myType;
  protected final PsiModifierList myModifierList;
  protected boolean myWritable;
  private String myOriginInfo = null;

  public LightVariableBase(PsiManager manager, PsiIdentifier nameIdentifier, PsiType type, boolean writable, PsiElement scope) {
    this(manager, nameIdentifier, JavaLanguage.INSTANCE, type, writable, scope);
  }

  public LightVariableBase(PsiManager manager, PsiIdentifier nameIdentifier, Language language, PsiType type, boolean writable, PsiElement scope) {
    super(manager, language);
    myNameIdentifier = nameIdentifier;
    myWritable = writable;
    myType = type;
    myScope = scope;
    myModifierList = createModifierList();
  }

  protected PsiModifierList createModifierList() {
    return new LightModifierList(getManager());
  }

  @Nonnull
  public PsiElement getDeclarationScope() {
    return myScope;
  }

  @Override
  public PsiIdentifier getNameIdentifier() {
    return myNameIdentifier;
  }

  @Override
  public boolean isValid() {
    return myNameIdentifier == null || myNameIdentifier.isValid();
  }

  @Override
  @Nonnull
  public String getName() {
    return StringUtil.notNullize(getNameIdentifier().getText());
  }

  @Override
  public PsiElement setName(@Nonnull String name) throws IncorrectOperationException{
    PsiImplUtil.setName(getNameIdentifier(), name);
    return this;
  }

  @Override
  @Nonnull
  public PsiType getType() {
    if (myType == null) {
      myType = computeType();
    }
    return myType;
  }

  @Nonnull
  protected PsiType computeType() {
    return PsiType.VOID;
  }

  @Override
  @Nonnull
  public PsiTypeElement getTypeElement() {
    return JavaPsiFacade.getInstance(getProject()).getElementFactory().createTypeElement(myType);
  }

  @Override
  public PsiModifierList getModifierList() {
    return myModifierList;
  }

  @Override
  public boolean hasModifierProperty(@Nonnull String name) {
    return getModifierList().hasModifierProperty(name);
  }

  @Override
  public PsiExpression getInitializer() {
    return null;
  }

  @Override
  public boolean hasInitializer() {
    return false;
  }

  @Override
  public String getText() {
    return myNameIdentifier.getText();
  }

  @Override
  public Object computeConstantValue() {
    return null;
  }

  @Override
  public void normalizeDeclaration() throws IncorrectOperationException {
  }

  @Override
  public boolean isWritable() {
    return myWritable;
  }

  @Nullable
  @Override
  public String getOriginInfo() {
    return myOriginInfo;
  }

  public void setOriginInfo(String originInfo) {
    myOriginInfo = originInfo;
  }
}
