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
package com.intellij.java.impl.refactoring.actions;

import com.intellij.java.impl.refactoring.anonymousToInner.AnonymousToInnerHandler;
import com.intellij.java.language.psi.PsiAnonymousClass;
import com.intellij.java.language.psi.PsiNewExpression;
import consulo.dataContext.DataContext;
import consulo.codeEditor.Editor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.editor.refactoring.action.RefactoringActionHandler;
import consulo.language.editor.refactoring.action.BaseRefactoringAction;

import jakarta.annotation.Nonnull;

public class AnonymousToInnerAction extends BaseRefactoringAction {
  public boolean isAvailableInEditorOnly() {
    return true;
  }

  public boolean isEnabledOnElements(@Nonnull PsiElement[] elements) {
    return false;
  }

  protected boolean isAvailableOnElementInEditorAndFile(@Nonnull final PsiElement element, @Nonnull final Editor editor, @Nonnull PsiFile file, @Nonnull DataContext context) {
    final PsiElement targetElement = file.findElementAt(editor.getCaretModel().getOffset());
    if (PsiTreeUtil.getParentOfType(targetElement, PsiAnonymousClass.class) != null) {
      return true;
    }
    if (PsiTreeUtil.getParentOfType(element, PsiAnonymousClass.class) != null) {
      return true;
    }
    final PsiNewExpression newExpression = PsiTreeUtil.getParentOfType(element, PsiNewExpression.class);
    return newExpression != null && newExpression.getAnonymousClass() != null;
  }

  public RefactoringActionHandler getHandler(@Nonnull DataContext dataContext) {
    return new AnonymousToInnerHandler();
  }
}
