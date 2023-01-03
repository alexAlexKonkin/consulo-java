/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.java.language.impl.psi.presentation.java;

import com.intellij.java.language.psi.PsiMethod;
import consulo.annotation.component.ExtensionImpl;
import consulo.navigation.ItemPresentation;
import consulo.navigation.ItemPresentationProvider;

import javax.annotation.Nonnull;

/**
 * @author yole
 */
@ExtensionImpl
public class MethodPresentationProvider implements ItemPresentationProvider<PsiMethod> {
  @Nonnull
  @Override
  public Class<PsiMethod> getItemClass() {
    return PsiMethod.class;
  }

  @Override
  public ItemPresentation getPresentation(PsiMethod item) {
    return JavaPresentationUtil.getMethodPresentation(item);
  }
}
