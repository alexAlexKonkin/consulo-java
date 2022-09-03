/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.java.impl.ig.inheritance;

import consulo.language.editor.intention.IntentionAction;
import com.intellij.java.analysis.codeInsight.intention.QuickFixFactory;
import consulo.language.editor.inspection.ProblemDescriptor;
import com.intellij.java.language.psi.*;
import consulo.application.ApplicationManager;
import consulo.ui.ModalityState;
import consulo.application.Result;
import consulo.language.editor.WriteCommandAction;
import consulo.logging.Logger;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.project.Project;
import com.intellij.psi.*;
import consulo.language.impl.DebugUtil;
import consulo.language.psi.search.ReferencesSearch;
import com.intellij.java.language.psi.util.InheritanceUtil;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.ui.ex.awt.internal.GuiUtils;
import consulo.language.util.IncorrectOperationException;
import consulo.application.util.query.Query;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.InspectionGadgetsFix;
import com.siyeh.ig.psiutils.ClassUtils;

import javax.annotation.Nonnull;

import java.lang.reflect.InvocationTargetException;

/**
 * User: cdr
 */
class StaticInheritanceFix extends InspectionGadgetsFix {
  private static final Logger LOG = Logger.getInstance(StaticInheritanceFix.class);
  private final boolean myReplaceInWholeProject;

  StaticInheritanceFix(boolean replaceInWholeProject) {
    myReplaceInWholeProject = replaceInWholeProject;
  }

  @Nonnull
  public String getName() {
    String scope =
      myReplaceInWholeProject ? InspectionGadgetsBundle.message("the.whole.project") : InspectionGadgetsBundle.message("this.class");
    return InspectionGadgetsBundle.message("static.inheritance.replace.quickfix", scope);
  }

  public void doFix(final Project project, final ProblemDescriptor descriptor) throws IncorrectOperationException {
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      public void run() {
        dodoFix(project, descriptor);
      }
    }, ModalityState.NON_MODAL, project.getDisposed());
  }

  private void dodoFix(final Project project, ProblemDescriptor descriptor) throws IncorrectOperationException {
    final PsiJavaCodeReferenceElement referenceElement = (PsiJavaCodeReferenceElement)descriptor.getPsiElement();
    final PsiClass iface = (PsiClass)referenceElement.resolve();
    assert iface != null;
    final PsiField[] allFields = iface.getAllFields();

    final PsiClass implementingClass = ClassUtils.getContainingClass(referenceElement);
    final PsiManager manager = referenceElement.getManager();
    assert implementingClass != null;
    final PsiFile file = implementingClass.getContainingFile();

    ProgressManager.getInstance().run(new Task.Modal(project, "Replacing usages of " + iface.getName(), false) {

      public void run(@Nonnull ProgressIndicator indicator) {
        for (final PsiField field : allFields) {
          final Query<PsiReference> search = ReferencesSearch.search(field, implementingClass.getUseScope(), false);
          for (PsiReference reference : search) {
            if (!(reference instanceof PsiReferenceExpression)) {
              continue;
            }
            final PsiReferenceExpression referenceExpression = (PsiReferenceExpression)reference;
            if (!myReplaceInWholeProject) {
              PsiClass aClass = PsiTreeUtil.getParentOfType(referenceExpression, PsiClass.class);
              boolean isInheritor = false;
              while (aClass != null) {
                isInheritor = InheritanceUtil.isInheritorOrSelf(aClass, implementingClass, true);
                if (isInheritor) break;
                aClass = PsiTreeUtil.getParentOfType(aClass, PsiClass.class);
              }
              if (!isInheritor) continue;
            }
            final Runnable runnable = new Runnable() {
              public void run() {
                if (isQuickFixOnReadOnlyFile(referenceExpression)) {
                  return;
                }
                final PsiElementFactory elementFactory = JavaPsiFacade.getInstance(manager.getProject()).getElementFactory();
                final PsiReferenceExpression qualified =
                  (PsiReferenceExpression)elementFactory
                    .createExpressionFromText("xxx." + referenceExpression.getText(), referenceExpression);
                final PsiReferenceExpression newReference = (PsiReferenceExpression)referenceExpression.replace(qualified);
                final PsiReferenceExpression qualifier = (PsiReferenceExpression)newReference.getQualifierExpression();
                assert qualifier != null : DebugUtil.psiToString(newReference, false);
                final PsiClass containingClass = field.getContainingClass();
                qualifier.bindToElement(containingClass);
              }
            };
            invokeWriteAction(runnable, file);
          }
        }
        final Runnable runnable = new Runnable() {
          public void run() {
            PsiClassType classType = JavaPsiFacade.getInstance(project).getElementFactory().createType(iface);
            IntentionAction fix = QuickFixFactory.getInstance().createExtendsListFix(implementingClass, classType, false);
            fix.invoke(project, null, file);
          }
        };
        invokeWriteAction(runnable, file);
      }
    });
  }

  private static void invokeWriteAction(final Runnable runnable, final PsiFile file) {
    try {
      GuiUtils.runOrInvokeAndWait(new Runnable() {
        public void run() {
          new WriteCommandAction(file.getProject(), file) {
            protected void run(Result result) throws Throwable {
              runnable.run();
            }
          }.execute();
        }
      });
    }
    catch (InvocationTargetException e) {
      LOG.error(e);
    }
    catch (InterruptedException e) {
      LOG.error(e);
    }
  }
}
