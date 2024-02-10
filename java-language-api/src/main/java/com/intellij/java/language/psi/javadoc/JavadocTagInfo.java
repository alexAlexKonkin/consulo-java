/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.java.language.psi.javadoc;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import jakarta.annotation.Nullable;

/**
 * @author mike
 */
@ExtensionAPI(ComponentScope.PROJECT)
public interface JavadocTagInfo {
  String getName();

  boolean isInline();

  boolean isValidInContext(PsiElement element);

  /**
   * Checks the tag value for correctness.
   *
   * @param value Doc tag to check.
   * @return Returns null if correct, error message otherwise.
   */
  @Nullable
  String checkTagValue(PsiDocTagValue value);

  @Nullable
  PsiReference getReference(PsiDocTagValue value);
}