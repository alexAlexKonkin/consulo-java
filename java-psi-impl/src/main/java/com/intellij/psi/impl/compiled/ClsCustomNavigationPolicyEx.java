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
package com.intellij.psi.impl.compiled;

import javax.annotation.Nonnull;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

import javax.annotation.Nullable;

/**
 * @author peter
 */
public abstract class ClsCustomNavigationPolicyEx implements ClsCustomNavigationPolicy {
  @Nullable
  @Override
  public PsiElement getNavigationElement(@Nonnull ClsClassImpl clsClass) {
    return null;
  }

  @Nullable
  @Override
  public PsiElement getNavigationElement(@Nonnull ClsMethodImpl clsMethod) {
    return null;
  }

  @Nullable
  @Override
  public PsiElement getNavigationElement(@Nonnull ClsFieldImpl clsField) {
    return null;
  }

  @javax.annotation.Nullable
  public PsiFile getFileNavigationElement(@Nonnull ClsFileImpl file) {
    return null;
  }
}
