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
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement;
import com.intellij.openapi.command.undo.UndoUtil;
import consulo.logging.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.util.PsiFormatUtil;
import com.intellij.psi.util.PsiFormatUtilBase;
import com.intellij.util.IncorrectOperationException;

public class MethodThrowsFix extends LocalQuickFixAndIntentionActionOnPsiElement {
  private static final Logger LOG = Logger.getInstance(MethodThrowsFix.class);

  private final String myThrowsCanonicalText;
  private final boolean myShouldThrow;
  private final String myMethodName;

  public MethodThrowsFix(PsiMethod method, PsiClassType exceptionType, boolean shouldThrow, boolean showContainingClass) {
    super(method);
    myThrowsCanonicalText = exceptionType.getCanonicalText();
    myShouldThrow = shouldThrow;
    myMethodName = PsiFormatUtil.formatMethod(method,
                                              PsiSubstitutor.EMPTY,
                                              PsiFormatUtilBase.SHOW_NAME | (showContainingClass ? PsiFormatUtilBase.SHOW_CONTAINING_CLASS
                                                                                                   : 0),
                                              0);
  }

  @Nonnull
  @Override
  public String getText() {
    return JavaQuickFixBundle.message(myShouldThrow ? "fix.throws.list.add.exception" : "fix.throws.list.remove.exception",
                                  myThrowsCanonicalText,
                                  myMethodName);
  }

  @Override
  @Nonnull
  public String getFamilyName() {
    return JavaQuickFixBundle.message("fix.throws.list.family");
  }

  @Override
  public boolean isAvailable(@Nonnull Project project,
                             @Nonnull PsiFile file,
                             @Nonnull PsiElement startElement,
                             @Nonnull PsiElement endElement) {
    final PsiMethod myMethod = (PsiMethod)startElement;
    return myMethod.isValid()
        && myMethod.getManager().isInProject(myMethod);
  }

  @Override
  public void invoke(@Nonnull Project project,
                     @Nonnull PsiFile file,
                     @javax.annotation.Nullable Editor editor,
                     @Nonnull PsiElement startElement,
                     @Nonnull PsiElement endElement) {
    final PsiMethod myMethod = (PsiMethod)startElement;
    if (!FileModificationService.getInstance().prepareFileForWrite(myMethod.getContainingFile())) return;
    PsiJavaCodeReferenceElement[] referenceElements = myMethod.getThrowsList().getReferenceElements();
    try {
      boolean alreadyThrows = false;
      for (PsiJavaCodeReferenceElement referenceElement : referenceElements) {
        if (referenceElement.getCanonicalText().equals(myThrowsCanonicalText)) {
          alreadyThrows = true;
          if (!myShouldThrow) {
            referenceElement.delete();
            break;
          }
        }
      }
      if (myShouldThrow && !alreadyThrows) {
        final PsiElementFactory factory = JavaPsiFacade.getInstance(myMethod.getProject()).getElementFactory();
        final PsiClassType type = (PsiClassType)factory.createTypeFromText(myThrowsCanonicalText, myMethod);
        PsiJavaCodeReferenceElement ref = factory.createReferenceElementByType(type);
        ref = (PsiJavaCodeReferenceElement)JavaCodeStyleManager.getInstance(project).shortenClassReferences(ref);
        myMethod.getThrowsList().add(ref);
      }
      UndoUtil.markPsiFileForUndo(file);
    } catch (IncorrectOperationException e) {
      LOG.error(e);
    }
  }
}
