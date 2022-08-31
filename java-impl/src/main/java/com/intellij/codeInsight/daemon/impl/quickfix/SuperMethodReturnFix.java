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
import com.intellij.java.analysis.impl.codeInsight.daemon.impl.analysis.JavaHighlightUtil;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.java.language.psi.PsiMethod;
import com.intellij.java.language.psi.PsiSubstitutor;
import com.intellij.java.language.psi.PsiType;
import com.intellij.java.language.psi.util.PsiFormatUtil;
import com.intellij.psi.util.PsiFormatUtilBase;
import com.intellij.refactoring.changeSignature.ChangeSignatureProcessor;
import com.intellij.refactoring.changeSignature.ParameterInfoImpl;

public class SuperMethodReturnFix implements IntentionAction {

  private final PsiType mySuperMethodType;
  private final PsiMethod mySuperMethod;

  public SuperMethodReturnFix(PsiMethod superMethod, PsiType superMethodType) {
    mySuperMethodType = superMethodType;
    mySuperMethod = superMethod;
  }

  @Override
  @Nonnull
  public String getText() {
    String name = PsiFormatUtil.formatMethod(
            mySuperMethod,
            PsiSubstitutor.EMPTY, PsiFormatUtilBase.SHOW_NAME | PsiFormatUtilBase.SHOW_CONTAINING_CLASS,
            0
    );
    return JavaQuickFixBundle.message("fix.super.method.return.type.text",
                                  name,
                                  JavaHighlightUtil.formatType(mySuperMethodType));
  }

  @Override
  @Nonnull
  public String getFamilyName() {
    return JavaQuickFixBundle.message("fix.super.method.return.type.family");
  }

  @Override
  public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
    return
            mySuperMethod != null
            && mySuperMethod.isValid()
            && mySuperMethod.getManager().isInProject(mySuperMethod)
            && mySuperMethodType != null
            && mySuperMethodType.isValid();
  }

  @Override
  public void invoke(@Nonnull Project project, Editor editor, PsiFile file) {
    if (!FileModificationService.getInstance().prepareFileForWrite(mySuperMethod.getContainingFile())) return;
    ChangeSignatureProcessor processor = new ChangeSignatureProcessor(
            project,
            mySuperMethod,
            false, null,
            mySuperMethod.getName(),
            mySuperMethodType,
            ParameterInfoImpl.fromMethod(mySuperMethod));
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      processor.run();
    } else {
      processor.run();
    }
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }
}
