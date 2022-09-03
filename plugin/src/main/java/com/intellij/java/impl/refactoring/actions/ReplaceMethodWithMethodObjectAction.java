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
 * Date: 06-May-2008
 */
package com.intellij.java.impl.refactoring.actions;

import com.intellij.java.impl.refactoring.extractMethodObject.ExtractMethodObjectHandler;
import consulo.dataContext.DataContext;
import consulo.language.psi.PsiElement;
import consulo.language.editor.refactoring.action.RefactoringActionHandler;
import consulo.language.editor.refactoring.action.BaseRefactoringAction;

import javax.annotation.Nonnull;

public class ReplaceMethodWithMethodObjectAction extends BaseRefactoringAction {
  protected boolean isAvailableInEditorOnly() {
    return true;
  }

  protected boolean isEnabledOnElements(@Nonnull final PsiElement[] elements) {
    return false;
  }

  protected RefactoringActionHandler getHandler(@Nonnull final DataContext dataContext) {
    return new ExtractMethodObjectHandler();
  }
}