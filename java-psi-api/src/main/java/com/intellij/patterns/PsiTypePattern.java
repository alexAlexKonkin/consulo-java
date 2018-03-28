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
package com.intellij.patterns;

import javax.annotation.Nonnull;

import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiType;
import com.intellij.util.ProcessingContext;

/**
 * @author peter
 */
public class PsiTypePattern extends ObjectPattern<PsiType,PsiTypePattern> {
  protected PsiTypePattern() {
    super(PsiType.class);
  }

  public PsiTypePattern arrayOf(final ElementPattern pattern) {
    return with(new PatternCondition<PsiType>("arrayOf") {
      public boolean accepts(@Nonnull final PsiType psiType, final ProcessingContext context) {
        return psiType instanceof PsiArrayType &&
               pattern.getCondition().accepts(((PsiArrayType)psiType).getComponentType(), context);
      }
    });
  }

  public PsiTypePattern classType(final ElementPattern<? extends PsiClass> pattern) {
    return with(new PatternCondition<PsiType>("classType") {
      public boolean accepts(@Nonnull final PsiType psiType, final ProcessingContext context) {
        return psiType instanceof PsiClassType &&
               pattern.getCondition().accepts(((PsiClassType)psiType).resolve(), context);
      }
    });
  }
}
