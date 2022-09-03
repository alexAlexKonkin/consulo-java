/*
 * Copyright 2003-2006 Dave Griffith, Bas Leijdekkers
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
package com.intellij.java.impl.ig.fixes;

import javax.annotation.Nonnull;

import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.dataContext.DataManager;
import consulo.dataContext.DataContext;
import consulo.application.ApplicationManager;
import consulo.project.Project;
import com.intellij.java.language.psi.PsiAnonymousClass;
import consulo.language.psi.PsiElement;
import com.intellij.java.analysis.refactoring.JavaRefactoringActionHandlerFactory;
import consulo.language.editor.refactoring.action.RefactoringActionHandler;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.InspectionGadgetsFix;

public class MoveAnonymousToInnerClassFix extends InspectionGadgetsFix {

  private final String name;

  public MoveAnonymousToInnerClassFix(String name) {
    this.name = name;
  }

  public MoveAnonymousToInnerClassFix() {
    name = InspectionGadgetsBundle.message(
      "move.anonymous.to.inner.quickfix");
  }

  @Nonnull
  public String getName() {
    return name;
  }

  public void doFix(@Nonnull final Project project, ProblemDescriptor descriptor) {
    final PsiElement nameElement = descriptor.getPsiElement();
    final PsiAnonymousClass aClass =
      (PsiAnonymousClass)nameElement.getParent();
    final JavaRefactoringActionHandlerFactory factory =
      JavaRefactoringActionHandlerFactory.getInstance();
    final RefactoringActionHandler anonymousToInner =
      factory.createAnonymousToInnerHandler();
    final DataManager dataManager = DataManager.getInstance();
    final DataContext dataContext = dataManager.getDataContext();
    final Runnable runnable = new Runnable() {
      public void run() {
        anonymousToInner.invoke(project, new PsiElement[]{aClass}, dataContext);
      }
    };
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      runnable.run();
    }
    else {
      ApplicationManager.getApplication().invokeLater(runnable, project.getDisposed());
    }
  }
}
