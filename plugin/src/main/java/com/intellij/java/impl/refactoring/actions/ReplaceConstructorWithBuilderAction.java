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

/*
 * User: anna
 * Date: 07-May-2008
 */
package com.intellij.java.impl.refactoring.actions;

import com.intellij.java.impl.refactoring.replaceConstructorWithBuilder.ReplaceConstructorWithBuilderHandler;
import com.intellij.java.language.psi.PsiClass;
import consulo.dataContext.DataContext;
import consulo.codeEditor.Editor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.editor.refactoring.action.RefactoringActionHandler;
import consulo.language.editor.refactoring.action.BaseRefactoringAction;

import jakarta.annotation.Nonnull;

public class ReplaceConstructorWithBuilderAction extends BaseRefactoringAction {
  protected boolean isAvailableInEditorOnly() {
    return true;
  }

  @Override
  protected boolean isAvailableOnElementInEditorAndFile(@Nonnull PsiElement element, @Nonnull Editor editor, @Nonnull PsiFile file, @Nonnull DataContext context) {
    final int offset = editor.getCaretModel().getOffset();
    final PsiElement elementAt = file.findElementAt(offset);
    final PsiClass psiClass = ReplaceConstructorWithBuilderHandler.getParentNamedClass(elementAt);
    return psiClass != null && psiClass.getConstructors().length > 0;
  }

  protected boolean isEnabledOnElements(@Nonnull final PsiElement[] elements) {
    return false;
  }

  protected RefactoringActionHandler getHandler(@Nonnull final DataContext dataContext) {
    return new ReplaceConstructorWithBuilderHandler();
  }
}
