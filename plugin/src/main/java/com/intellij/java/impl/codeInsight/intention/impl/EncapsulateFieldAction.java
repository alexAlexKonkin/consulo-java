/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.java.impl.codeInsight.intention.impl;

import com.intellij.java.impl.refactoring.encapsulateFields.EncapsulateFieldsHandler;
import com.intellij.java.language.psi.*;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.language.editor.CodeInsightBundle;
import consulo.language.editor.intention.IntentionMetaData;
import consulo.language.editor.refactoring.action.BaseRefactoringIntentionAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.SyntheticElement;
import consulo.language.util.IncorrectOperationException;
import consulo.project.Project;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Danila Ponomarenko
 */
@ExtensionImpl
@IntentionMetaData(ignoreId = "java.EncapsulateFieldAction", categories = {"Java", "Refactorings"}, fileExtensions = "java")
public class EncapsulateFieldAction extends BaseRefactoringIntentionAction {

  @Nonnull
  @Override
  public String getText() {
    return CodeInsightBundle.message("intention.encapsulate.field.text");
  }

  @Nonnull
  @Override
  public final String getFamilyName() {
    return getText();
  }

  @Override
  public boolean isAvailable(@Nonnull Project project, Editor editor, @Nonnull PsiElement element) {
    if (element instanceof SyntheticElement){
      return false;
    }

    final PsiField field = getField(element);
    return field != null && !field.hasModifierProperty(PsiModifier.FINAL) && !field.hasModifierProperty(PsiModifier.PRIVATE);
  }

  @Override
  public void invoke(@Nonnull Project project, Editor editor, @Nonnull PsiElement element) throws IncorrectOperationException {
    final PsiField field = getField(element);
    if (field == null) {
      return;
    }

    new EncapsulateFieldsHandler().invoke(project, new PsiElement[]{field}, null);
  }


  @Nullable
  protected static PsiField getField(@Nullable PsiElement element) {
    if (element == null || !(element instanceof PsiIdentifier)) {
      return null;
    }

    final PsiElement parent = element.getParent();
    if (parent == null || !(parent instanceof PsiReferenceExpression)) {
      return null;
    }
    final PsiReferenceExpression ref = (PsiReferenceExpression)parent;
    final PsiExpression qualifier = ref.getQualifierExpression();
    if (qualifier == null || qualifier instanceof PsiThisExpression) {
      return null;
    }

    final PsiElement resolved = ref.resolve();
    if (resolved == null || !(resolved instanceof PsiField)) {
      return null;
    }
    return (PsiField)resolved;
  }
}