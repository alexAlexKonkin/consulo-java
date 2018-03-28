/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.codeInsight.daemon.impl.analysis;

import javax.annotation.Nonnull;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiThisExpression;
import com.intellij.refactoring.util.RefactoringChangeUtil;
import com.intellij.util.IncorrectOperationException;

/**
* User: anna
*/
class QualifyWithThisFix implements IntentionAction {
  private final PsiClass myContainingClass;
  private final PsiElement myExpression;

  public QualifyWithThisFix(PsiClass containingClass, PsiElement expression) {
    myContainingClass = containingClass;
    myExpression = expression;
  }

  @Nonnull
  @Override
  public String getText() {
    return "Qualify with " + myContainingClass.getName() + ".this";
  }

  @Nonnull
  @Override
  public String getFamilyName() {
    return getText();
  }

  @Override
  public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
    return true;
  }

  @Override
  public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    final PsiThisExpression thisExpression =
      RefactoringChangeUtil.createThisExpression(PsiManager.getInstance(project), myContainingClass);
    ((PsiReferenceExpression)myExpression).setQualifierExpression(thisExpression);
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }
}
