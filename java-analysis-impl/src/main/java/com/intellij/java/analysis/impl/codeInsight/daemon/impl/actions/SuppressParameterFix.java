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
package com.intellij.java.analysis.impl.codeInsight.daemon.impl.actions;

import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInsight.daemon.impl.actions.AbstractBatchSuppressByNoInspectionCommentFix;
import com.intellij.java.analysis.impl.codeInspection.JavaSuppressionUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author ven
 */
public class SuppressParameterFix extends AbstractBatchSuppressByNoInspectionCommentFix {
  public SuppressParameterFix(@Nonnull HighlightDisplayKey key) {
    this(key.getID());
  }

  public SuppressParameterFix(String ID) {
    super(ID, false);
  }

  @Override
  @Nonnull
  public String getText() {
    return "Suppress for parameter";
  }

  @Nullable
  @Override
  public PsiElement getContainer(PsiElement context) {
    PsiParameter psiParameter = PsiTreeUtil.getParentOfType(context, PsiParameter.class, false);
    return psiParameter != null && JavaSuppressionUtil.canHave15Suppressions(psiParameter) ? psiParameter : null;
  }

  @Override
  protected boolean replaceSuppressionComments(PsiElement container) {
    return false;
  }

  @Override
  protected void createSuppression(@Nonnull Project project, @Nonnull PsiElement element,
                                   @Nonnull PsiElement cont) throws IncorrectOperationException {
    PsiModifierListOwner container = (PsiModifierListOwner) cont;
    final PsiModifierList modifierList = container.getModifierList();
    if (modifierList != null) {
      JavaSuppressionUtil.addSuppressAnnotation(project, container, container, myID);
    }
  }
}
