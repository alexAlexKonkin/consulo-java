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
package com.intellij.refactoring.introduceparameterobject;

import javax.annotation.Nonnull;

import com.intellij.ide.util.SuperMethodWarningUtil;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.ScrollingModel;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.java.language.psi.PsiMethod;
import com.intellij.java.language.psi.PsiMethodCallExpression;
import com.intellij.java.language.psi.PsiParameter;
import com.intellij.java.language.psi.PsiParameterList;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.HelpID;
import com.intellij.refactoring.RefactorJBundle;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.util.CommonRefactoringUtil;

public class IntroduceParameterObjectHandler implements RefactoringActionHandler {
  private static final String REFACTORING_NAME = RefactorJBundle.message("introduce.parameter.object");

  public void invoke(@Nonnull Project project, Editor editor, PsiFile file, DataContext dataContext) {
    final ScrollingModel scrollingModel = editor.getScrollingModel();
    scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE);
    final PsiElement element = dataContext.getData(LangDataKeys.PSI_ELEMENT);
    PsiMethod selectedMethod = null;
    if (element instanceof PsiMethod) {
      selectedMethod = (PsiMethod)element;
    }
    else if (element instanceof PsiParameter && ((PsiParameter)element).getDeclarationScope() instanceof PsiMethod){
      selectedMethod = (PsiMethod)((PsiParameter)element).getDeclarationScope();
    }
    else {
      final CaretModel caretModel = editor.getCaretModel();
      final int position = caretModel.getOffset();
      final PsiElement elementAt = file.findElementAt(position);
      final PsiMethodCallExpression methodCallExpression =
       PsiTreeUtil.getParentOfType(elementAt, PsiMethodCallExpression.class);
      if (methodCallExpression != null) {
        selectedMethod = methodCallExpression.resolveMethod();
      } else {
        final PsiParameterList parameterList = PsiTreeUtil.getParentOfType(elementAt, PsiParameterList.class);
        if (parameterList != null && parameterList.getParent() instanceof PsiMethod) {
          selectedMethod = (PsiMethod)parameterList.getParent();
        }
      }
    }
    if (selectedMethod == null) {
      final String message = RefactorJBundle.message("cannot.perform.the.refactoring") +
                             RefactorJBundle.message("the.caret.should.be.positioned.at.the.name.of.the.method.to.be.refactored");
      CommonRefactoringUtil.showErrorHint(project, editor, message, REFACTORING_NAME, HelpID.IntroduceParameterObject);
      return;
    }
    invoke(project, selectedMethod, editor);
  }

  public void invoke(@Nonnull Project project, @Nonnull PsiElement[] elements, DataContext dataContext) {
    if (elements.length != 1) {
      return;
    }
    final PsiMethod method = PsiTreeUtil.getParentOfType(elements[0], PsiMethod.class, false);
    if (method == null) {
      return;
    }
    Editor editor = dataContext.getData(PlatformDataKeys.EDITOR);
    invoke(project, method, editor);
  }

  private static void invoke(final Project project, final PsiMethod selectedMethod, Editor editor) {
    PsiMethod newMethod = SuperMethodWarningUtil.checkSuperMethod(selectedMethod, RefactoringBundle.message("to.refactor"));
    if (newMethod == null) return;
    if (!CommonRefactoringUtil.checkReadOnlyStatus(project, newMethod)) return;

    final PsiParameter[] parameters = newMethod.getParameterList().getParameters();
    if (parameters.length == 0) {
      final String message =
        RefactorJBundle.message("cannot.perform.the.refactoring") + RefactorJBundle.message("method.selected.has.no.parameters");
      CommonRefactoringUtil.showErrorHint(project, editor, message, REFACTORING_NAME, HelpID.IntroduceParameterObject);
      return;
    }
    if (newMethod instanceof PsiCompiledElement) {
      CommonRefactoringUtil.showErrorHint(project, editor, RefactorJBundle.message("cannot.perform.the.refactoring") + RefactorJBundle.message(
          "the.selected.method.cannot.be.wrapped.because.it.is.defined.in.a.non.project.class"), REFACTORING_NAME, HelpID.IntroduceParameterObject);
      return;
    }
    new IntroduceParameterObjectDialog(newMethod).show();
  }
}
