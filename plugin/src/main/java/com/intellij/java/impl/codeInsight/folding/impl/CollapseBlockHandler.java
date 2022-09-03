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
package com.intellij.java.impl.codeInsight.folding.impl;

import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.ide.impl.idea.codeInsight.folding.impl.EditorFoldingInfo;
import consulo.ide.impl.idea.codeInsight.folding.impl.FoldingUtil;
import com.intellij.java.language.psi.JavaTokenType;
import com.intellij.java.language.psi.PsiCodeBlock;
import com.intellij.java.language.psi.PsiJavaToken;
import consulo.codeEditor.Editor;
import consulo.codeEditor.FoldRegion;
import consulo.codeEditor.FoldingModelEx;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.logging.Logger;

import javax.annotation.Nonnull;

/**
 * @author ven
 */
public class CollapseBlockHandler implements CodeInsightActionHandler {
  public static final String ourPlaceHolderText = "{...}";
  private static final Logger LOG = Logger.getInstance(CollapseBlockHandler.class);

  @Override
  public void invoke(@Nonnull final Project project, @Nonnull final Editor editor, @Nonnull final PsiFile file) {
    editor.getFoldingModel().runBatchFoldingOperation(new Runnable() {
      @Override
      public void run() {
        final EditorFoldingInfo info = EditorFoldingInfo.get(editor);
        FoldingModelEx model = (FoldingModelEx) editor.getFoldingModel();
        PsiElement element = file.findElementAt(editor.getCaretModel().getOffset() - 1);
        if (!(element instanceof PsiJavaToken) || ((PsiJavaToken) element).getTokenType() != JavaTokenType.RBRACE) {
          element = file.findElementAt(editor.getCaretModel().getOffset());
        }
        if (element == null) return;
        PsiCodeBlock block = PsiTreeUtil.getParentOfType(element, PsiCodeBlock.class);
        FoldRegion previous = null;
        FoldRegion myPrevious = null;
        while (block != null) {
          int start = block.getTextRange().getStartOffset();
          int end = block.getTextRange().getEndOffset();
          FoldRegion existing = FoldingUtil.findFoldRegion(editor, start, end);
          if (existing != null) {
            previous = existing;
            if (info.getPsiElement(existing) == null) myPrevious = existing;
            block = PsiTreeUtil.getParentOfType(block, PsiCodeBlock.class);
            continue;
          }
          if (!model.intersectsRegion(start, end)) {
            FoldRegion region = model.addFoldRegion(start, end, ourPlaceHolderText);
            LOG.assertTrue(region != null);
            region.setExpanded(false);
            if (myPrevious != null && info.getPsiElement(region) == null) {
              info.removeRegion(myPrevious);
              model.removeFoldRegion(myPrevious);
            }
            int offset = block.getTextRange().getEndOffset() < editor.getCaretModel().getOffset() ?
                start : end;
            editor.getCaretModel().moveToOffset(offset);
            return;
          } else break;
        }
        if (previous != null) {
          previous.setExpanded(false);
          if (myPrevious != null) {
            info.removeRegion(myPrevious);
            model.removeFoldRegion(myPrevious);
          }
          editor.getCaretModel().moveToOffset(previous.getEndOffset());
        }
      }
    });
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }
}
