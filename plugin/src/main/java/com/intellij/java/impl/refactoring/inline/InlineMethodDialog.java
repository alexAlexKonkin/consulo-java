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
package com.intellij.java.impl.refactoring.inline;

import com.intellij.java.impl.refactoring.HelpID;
import com.intellij.java.impl.refactoring.JavaRefactoringSettings;
import com.intellij.java.language.psi.PsiJavaCodeReferenceElement;
import com.intellij.java.language.psi.PsiMethod;
import com.intellij.java.language.psi.PsiSubstitutor;
import com.intellij.java.language.psi.util.PsiFormatUtil;
import consulo.application.HelpManager;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.editor.refactoring.RefactoringBundle;
import consulo.ide.impl.idea.refactoring.inline.InlineOptionsWithSearchSettingsDialog;

public class InlineMethodDialog extends InlineOptionsWithSearchSettingsDialog {
  public static final String REFACTORING_NAME = RefactoringBundle.message("inline.method.title");
  private final PsiJavaCodeReferenceElement myReferenceElement;
  private final Editor myEditor;
  private final boolean myAllowInlineThisOnly;

  private final PsiMethod myMethod;

  private int myOccurrencesNumber = -1;

  public InlineMethodDialog(Project project, PsiMethod method, PsiJavaCodeReferenceElement ref, Editor editor,
                            final boolean allowInlineThisOnly) {
    super(project, true, method);
    myMethod = method;
    myReferenceElement = ref;
    myEditor = editor;
    myAllowInlineThisOnly = allowInlineThisOnly;
    myInvokedOnReference = ref != null;

    setTitle(REFACTORING_NAME);
    myOccurrencesNumber = initOccurrencesNumber(method);
    init();
  }

  protected String getNameLabelText() {
    String methodText = PsiFormatUtil.formatMethod(myMethod,
        PsiSubstitutor.EMPTY, PsiFormatUtil.SHOW_NAME | PsiFormatUtil.SHOW_PARAMETERS, PsiFormatUtil.SHOW_TYPE);
    return RefactoringBundle.message("inline.method.method.label", methodText);
  }

  protected String getBorderTitle() {
    return RefactoringBundle.message("inline.method.border.title");
  }

  protected String getInlineThisText() {
    return RefactoringBundle.message("this.invocation.only.and.keep.the.method");
  }

  protected String getInlineAllText() {
    final String occurrencesString = myOccurrencesNumber > -1 ? " (" + myOccurrencesNumber + " occurrence" + (myOccurrencesNumber == 1 ? ")" : "s)") : "";
    return (myMethod.isWritable()
        ? RefactoringBundle.message("all.invocations.and.remove.the.method")
        : RefactoringBundle.message("all.invocations.in.project")) + occurrencesString;
  }

  protected void doAction() {
    super.doAction();
    invokeRefactoring(
        new InlineMethodProcessor(getProject(), myMethod, myReferenceElement, myEditor, isInlineThisOnly(), isSearchInCommentsAndStrings(),
            isSearchForTextOccurrences()));
    JavaRefactoringSettings settings = JavaRefactoringSettings.getInstance();
    if (myRbInlineThisOnly.isEnabled() && myRbInlineAll.isEnabled()) {
      settings.INLINE_METHOD_THIS = isInlineThisOnly();
    }
  }

  protected void doHelpAction() {
    if (myMethod.isConstructor()) HelpManager.getInstance().invokeHelp(HelpID.INLINE_CONSTRUCTOR);
    else HelpManager.getInstance().invokeHelp(HelpID.INLINE_METHOD);
  }

  protected boolean canInlineThisOnly() {
    return InlineMethodHandler.checkRecursive(myMethod) || myAllowInlineThisOnly;
  }

  protected boolean isInlineThis() {
    return JavaRefactoringSettings.getInstance().INLINE_METHOD_THIS;
  }

  @Override
  protected boolean isSearchInCommentsAndStrings() {
    return JavaRefactoringSettings.getInstance().RENAME_SEARCH_IN_COMMENTS_FOR_METHOD;
  }

  @Override
  protected void saveSearchInCommentsAndStrings(boolean searchInComments) {
    JavaRefactoringSettings.getInstance().RENAME_SEARCH_IN_COMMENTS_FOR_METHOD = searchInComments;
  }

  @Override
  protected boolean isSearchForTextOccurrences() {
    return JavaRefactoringSettings.getInstance().RENAME_SEARCH_FOR_TEXT_FOR_METHOD;
  }

  @Override
  protected void saveSearchInTextOccurrences(boolean searchInTextOccurrences) {
    JavaRefactoringSettings.getInstance().RENAME_SEARCH_FOR_TEXT_FOR_METHOD = searchInTextOccurrences;
  }
}
