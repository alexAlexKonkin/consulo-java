// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.java.language.psi;

import consulo.language.psi.PsiElement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface PsiRecordHeader extends PsiElement {
  @Nonnull
  PsiRecordComponent[] getRecordComponents();

  @Nullable
  PsiClass getContainingClass();
}