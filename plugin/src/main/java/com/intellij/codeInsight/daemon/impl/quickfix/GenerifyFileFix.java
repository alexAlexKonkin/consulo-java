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
package com.intellij.codeInsight.daemon.impl.quickfix;

import javax.annotation.Nonnull;

import com.intellij.codeInsight.FileModificationService;
import consulo.java.analysis.impl.JavaQuickFixBundle;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.refactoring.actions.TypeCookAction;

public class GenerifyFileFix implements IntentionAction, LocalQuickFix {
  private String myFileName;

  @Override
  @Nonnull
  public String getText() {
    return JavaQuickFixBundle.message("generify.text", myFileName);
  }

  @Nonnull
  @Override
  public String getName() {
    return getText();
  }

  @Override
  @Nonnull
  public String getFamilyName() {
    return JavaQuickFixBundle.message("generify.family");
  }

  @Override
  public void applyFix(@Nonnull final Project project, @Nonnull final ProblemDescriptor descriptor) {
    final PsiElement element = descriptor.getPsiElement();
    if (element == null) return;
    final PsiFile file = element.getContainingFile();
    if (isAvailable(project, null, file)) {
      myFileName = file.getName();
      new WriteCommandAction(project) {
        @Override
        protected void run(Result result) throws Throwable {
          invoke(project, FileEditorManager.getInstance(project).getSelectedTextEditor(), file);
        }
      }.execute();
    }
  }

  @Override
  public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
    if (file != null && file.isValid()) {
      myFileName = file.getName();
      return PsiManager.getInstance(project).isInProject(file);
    }
    else {
      return false;
    }
  }

  @Override
  public void invoke(@Nonnull Project project, Editor editor, PsiFile file) {
    if (!FileModificationService.getInstance().prepareFileForWrite(file)) return;
    new TypeCookAction().getHandler().invoke(project, editor, file, null);
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }
}
