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
import consulo.java.JavaQuickFixBundle;
import com.intellij.codeInsight.daemon.impl.analysis.JavaHighlightUtil;
import com.intellij.codeInsight.intention.IntentionAction;
import consulo.logging.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiCatchSection;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiTryStatement;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.IncorrectOperationException;

public class MoveCatchUpFix implements IntentionAction {
  private static final Logger LOG = Logger.getInstance("#com.intellij.codeInsight.daemon.impl.quickfix.DeleteCatchFix");

  private final PsiCatchSection myCatchSection;
  private final PsiCatchSection myMoveBeforeSection;

  public MoveCatchUpFix(@Nonnull PsiCatchSection catchSection, @Nonnull PsiCatchSection moveBeforeSection) {
    this.myCatchSection = catchSection;
    myMoveBeforeSection = moveBeforeSection;
  }

  @Override
  @Nonnull
  public String getText() {
    return JavaQuickFixBundle.message("move.catch.up.text",
                                  JavaHighlightUtil.formatType(myCatchSection.getCatchType()),
                                  JavaHighlightUtil.formatType(myMoveBeforeSection.getCatchType()));
  }

  @Override
  @Nonnull
  public String getFamilyName() {
    return JavaQuickFixBundle.message("move.catch.up.family");
  }

  @Override
  public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
    return myCatchSection.isValid()
           && myCatchSection.getManager().isInProject(myCatchSection)
           && myMoveBeforeSection.isValid()
           && myCatchSection.getCatchType() != null
           && PsiUtil.resolveClassInType(myCatchSection.getCatchType()) != null
           && myMoveBeforeSection.getCatchType() != null
           && PsiUtil.resolveClassInType(myMoveBeforeSection.getCatchType()) != null
           && !myCatchSection.getManager().areElementsEquivalent(
      PsiUtil.resolveClassInType(myCatchSection.getCatchType()),
      PsiUtil.resolveClassInType(myMoveBeforeSection.getCatchType()));
  }

  @Override
  public void invoke(@Nonnull Project project, Editor editor, PsiFile file) {
    if (!FileModificationService.getInstance().prepareFileForWrite(myCatchSection.getContainingFile())) return;
    try {
      PsiTryStatement statement = myCatchSection.getTryStatement();
      statement.addBefore(myCatchSection, myMoveBeforeSection);
      myCatchSection.delete();
    }
    catch (IncorrectOperationException e) {
      LOG.error(e);
    }
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }
}
