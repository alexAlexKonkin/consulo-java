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
package com.intellij.codeInsight.intention.impl;

import javax.annotation.Nonnull;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.BaseRefactoringIntentionAction;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.util.IncorrectOperationException;
import consulo.ui.image.Image;

/**
 * User: anna
 * Date: 9/5/11
 */
public class RunRefactoringAction extends BaseRefactoringIntentionAction {
  private final RefactoringActionHandler myHandler;
  private final String myCommandName;

  public RunRefactoringAction(RefactoringActionHandler handler, String commandName) {
    myHandler = handler;
    myCommandName = commandName;
  }

  @Nonnull
  @Override
  public String getText() {
    return myCommandName;
  }

  @Nonnull
  @Override
  public final String getFamilyName() {
    return getText();
  }

  @Override
  public boolean isAvailable(@Nonnull Project project, Editor editor, @Nonnull PsiElement element) {
    return true;
  }

  @Override
  public void invoke(@Nonnull Project project, Editor editor, @Nonnull PsiElement element) throws IncorrectOperationException {
    myHandler.invoke(project, editor, element.getContainingFile(), null);
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  @Override
  public Image getIcon(@IconFlags int flags) {
    return AllIcons.Actions.RefactoringBulb;
  }
}
