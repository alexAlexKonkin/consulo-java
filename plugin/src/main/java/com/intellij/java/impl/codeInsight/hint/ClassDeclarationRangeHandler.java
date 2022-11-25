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
package com.intellij.java.impl.codeInsight.hint;

import consulo.language.editor.hint.DeclarationRangeHandler;
import com.intellij.java.language.psi.*;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;

import javax.annotation.Nonnull;

public abstract class ClassDeclarationRangeHandler implements DeclarationRangeHandler {
  @Override
  @Nonnull
  public TextRange getDeclarationRange(@Nonnull final PsiElement container) {
    PsiClass aClass = (PsiClass) container;
    if (aClass instanceof PsiAnonymousClass) {
      PsiConstructorCall call = (PsiConstructorCall) aClass.getParent();
      int startOffset = call.getTextRange().getStartOffset();
      int endOffset = call.getArgumentList().getTextRange().getEndOffset();
      return new TextRange(startOffset, endOffset);
    } else {
      final PsiModifierList modifierList = aClass.getModifierList();
      int startOffset = modifierList == null ? aClass.getTextRange().getStartOffset() : modifierList.getTextRange().getStartOffset();
      final PsiReferenceList implementsList = aClass.getImplementsList();
      int endOffset = implementsList == null ? aClass.getTextRange().getEndOffset() : implementsList.getTextRange().getEndOffset();
      return new TextRange(startOffset, endOffset);
    }
  }
}
