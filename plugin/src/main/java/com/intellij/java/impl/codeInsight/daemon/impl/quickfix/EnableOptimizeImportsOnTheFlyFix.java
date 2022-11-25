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
package com.intellij.java.impl.codeInsight.daemon.impl.quickfix;

import com.intellij.java.language.psi.PsiJavaFile;
import consulo.codeEditor.Editor;
import consulo.java.analysis.impl.JavaQuickFixBundle;
import consulo.language.editor.CodeInsightSettings;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.LowPriorityAction;
import consulo.language.psi.PsiFile;
import consulo.project.Project;

import javax.annotation.Nonnull;

public class EnableOptimizeImportsOnTheFlyFix implements IntentionAction, LowPriorityAction{
  @Override
  @Nonnull
  public String getText() {
    return JavaQuickFixBundle.message("enable.optimize.imports.on.the.fly");
  }

  @Override
  @Nonnull
  public String getFamilyName() {
    return getText();
  }

  @Override
  public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
    return file.getManager().isInProject(file)
           && file instanceof PsiJavaFile
           && !CodeInsightSettings.getInstance().OPTIMIZE_IMPORTS_ON_THE_FLY
      ;
  }

  @Override
  public void invoke(@Nonnull Project project, Editor editor, PsiFile file) {
    CodeInsightSettings.getInstance().OPTIMIZE_IMPORTS_ON_THE_FLY = true;
    DaemonCodeAnalyzer.getInstance(project).restart();
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }
}
