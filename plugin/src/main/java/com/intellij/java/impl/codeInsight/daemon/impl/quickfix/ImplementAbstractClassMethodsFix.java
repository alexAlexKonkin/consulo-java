/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.java.impl.codeInsight.daemon.impl.quickfix;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import consulo.language.editor.FileModificationService;
import com.intellij.java.impl.codeInsight.generation.OverrideImplementUtil;
import com.intellij.java.impl.codeInsight.generation.PsiMethodMember;
import consulo.ide.impl.idea.ide.util.MemberChooser;
import consulo.application.Result;
import consulo.language.editor.WriteCommandAction;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import com.intellij.java.language.psi.JavaPsiFacade;
import com.intellij.java.language.psi.PsiClass;
import consulo.language.psi.PsiElement;
import com.intellij.java.language.psi.PsiElementFactory;
import consulo.language.psi.PsiFile;
import com.intellij.java.language.psi.PsiJavaCodeReferenceElement;
import com.intellij.java.language.psi.PsiNewExpression;
import com.intellij.java.language.psi.PsiSubstitutor;
import com.intellij.java.language.psi.util.TypeConversionUtil;
import consulo.language.util.IncorrectOperationException;

public class ImplementAbstractClassMethodsFix extends ImplementMethodsFix {
  public ImplementAbstractClassMethodsFix(PsiElement highlightElement) {
    super(highlightElement);
  }

  @Override
  public boolean isAvailable(@Nonnull Project project,
                             @Nonnull PsiFile file,
                             @Nonnull PsiElement startElement,
                             @Nonnull PsiElement endElement) {
    if (startElement instanceof PsiNewExpression) {
      final PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
      String startElementText = startElement.getText();
      try {
        PsiNewExpression newExpression =
          (PsiNewExpression)elementFactory.createExpressionFromText(startElementText + "{}", startElement);
        if (newExpression.getAnonymousClass() == null) {
          try {
            newExpression = (PsiNewExpression)elementFactory.createExpressionFromText(startElementText + "){}", startElement);
          }
          catch (IncorrectOperationException e) {
            return false;
          }
          if (newExpression.getAnonymousClass() == null) return false;
        }
      }
      catch (IncorrectOperationException e) {
        return false;
      }
      return true;
    }
    return false;
  }

  @Override
  public void invoke(@Nonnull final Project project,
                     @Nonnull PsiFile file,
                     @Nullable final Editor editor,
                     @Nonnull final PsiElement startElement,
                     @Nonnull PsiElement endElement) {
    final PsiFile containingFile = startElement.getContainingFile();
    if (editor == null || !FileModificationService.getInstance().prepareFileForWrite(containingFile)) return;
    PsiJavaCodeReferenceElement classReference = ((PsiNewExpression)startElement).getClassReference();
    if (classReference == null) return;
    final PsiClass psiClass = (PsiClass)classReference.resolve();
    if (psiClass == null) return;
    final MemberChooser<PsiMethodMember> chooser = chooseMethodsToImplement(editor, startElement, psiClass, false);
    if (chooser == null) return;

    final List<PsiMethodMember> selectedElements = chooser.getSelectedElements();
    if (selectedElements == null || selectedElements.isEmpty()) return;

    new WriteCommandAction(project, file) {
      @Override
      protected void run(final Result result) throws Throwable {
        PsiNewExpression newExpression =
          (PsiNewExpression)JavaPsiFacade.getElementFactory(project).createExpressionFromText(startElement.getText() + "{}", startElement);
        newExpression = (PsiNewExpression)startElement.replace(newExpression);
        final PsiClass psiClass = newExpression.getAnonymousClass();
        if (psiClass == null) return;
        Map<PsiClass, PsiSubstitutor> subst = new HashMap<PsiClass, PsiSubstitutor>();
        for (PsiMethodMember selectedElement : selectedElements) {
          final PsiClass baseClass = selectedElement.getElement().getContainingClass();
          if (baseClass != null) {
            PsiSubstitutor substitutor = subst.get(baseClass);
            if (substitutor == null) {
              substitutor = TypeConversionUtil.getSuperClassSubstitutor(baseClass, psiClass, PsiSubstitutor.EMPTY);
              subst.put(baseClass, substitutor);
            }
            selectedElement.setSubstitutor(substitutor);
          }
        }
        OverrideImplementUtil.overrideOrImplementMethodsInRightPlace(editor, psiClass, selectedElements, chooser.isCopyJavadoc(),
                                                                     chooser.isInsertOverrideAnnotation());
      }
    }.execute();
  }
}
