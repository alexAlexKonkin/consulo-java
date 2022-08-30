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

/**
 * @author cdr
 */
package com.intellij.codeInsight.intention.impl;

import com.intellij.codeInsight.FileModificationService;
import com.intellij.java.analysis.impl.codeInsight.daemon.impl.quickfix.SimplifyBooleanExpressionFix;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import javax.annotation.Nonnull;
import consulo.java.analysis.impl.JavaQuickFixBundle;

public class SimplifyBooleanExpressionAction implements IntentionAction{
  @Override
  @Nonnull
  public String getText() {
    return getFamilyName();
  }

  @Override
  @Nonnull
  public String getFamilyName() {
    return JavaQuickFixBundle.message("simplify.boolean.expression.family");
  }

  @Override
  public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
    PsiExpression expression = getExpressionToSimplify(editor, file);
    return expression != null && SimplifyBooleanExpressionFix.canBeSimplified(expression);
  }

  private static PsiExpression getExpressionToSimplify(final Editor editor, final PsiFile file) {
    int offset = editor.getCaretModel().getOffset();
    PsiElement element = file.findElementAt(offset);
    if (element == null) return null;
    PsiExpression expression = PsiTreeUtil.getParentOfType(element, PsiExpression.class);
    PsiElement parent = expression;
    while (parent instanceof PsiExpression && (PsiType.BOOLEAN.equals(((PsiExpression)parent).getType()) || parent instanceof PsiConditionalExpression)) {
      expression = (PsiExpression)parent;
      parent = parent.getParent();
    }
    return expression;
  }

  @Override
  public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    if (!FileModificationService.getInstance().prepareFileForWrite(file)) return;
    PsiExpression expression = getExpressionToSimplify(editor, file);
    SimplifyBooleanExpressionFix.simplifyExpression(expression);
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }
}