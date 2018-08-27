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
package com.intellij.codeInspection;

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Singleton;

import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInsight.daemon.impl.actions.SuppressAllForClassFix;
import com.intellij.codeInsight.daemon.impl.actions.SuppressByJavaCommentFix;
import com.intellij.codeInsight.daemon.impl.actions.SuppressFix;
import com.intellij.codeInsight.daemon.impl.actions.SuppressForClassFix;
import com.intellij.codeInsight.daemon.impl.actions.SuppressLocalWithCommentFix;
import com.intellij.codeInsight.daemon.impl.actions.SuppressParameterFix;
import com.intellij.psi.PsiDocCommentOwner;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifierListOwner;

@Singleton
public class BatchSuppressManagerImpl implements BatchSuppressManager {
  @Nonnull
  @Override
  public SuppressQuickFix[] createBatchSuppressActions(@Nonnull HighlightDisplayKey displayKey) {
    return new SuppressQuickFix[] {
        new SuppressByJavaCommentFix(displayKey),
        new SuppressLocalWithCommentFix(displayKey),
        new SuppressParameterFix(displayKey),
        new SuppressFix(displayKey),
        new SuppressForClassFix(displayKey),
        new SuppressAllForClassFix()
      };

  }

  @Override
  public boolean isSuppressedFor(@Nonnull final PsiElement element, final String toolId) {
    return JavaSuppressionUtil.getElementToolSuppressedIn(element, toolId) != null;
  }

  @Override
  @Nullable
  public PsiElement getElementMemberSuppressedIn(@Nonnull final PsiDocCommentOwner owner, final String inspectionToolID) {
    return JavaSuppressionUtil.getElementMemberSuppressedIn(owner, inspectionToolID);
  }

  @Override
  @Nullable
  public PsiElement getAnnotationMemberSuppressedIn(@Nonnull final PsiModifierListOwner owner, final String inspectionToolID) {
    return JavaSuppressionUtil.getAnnotationMemberSuppressedIn(owner, inspectionToolID);
  }

  @Override
  @Nullable
  public PsiElement getDocCommentToolSuppressedIn(@Nonnull final PsiDocCommentOwner owner, final String inspectionToolID) {
    return JavaSuppressionUtil.getDocCommentToolSuppressedIn(owner, inspectionToolID);
  }

  @Override
  @Nonnull
  public Collection<String> getInspectionIdsSuppressedInAnnotation(@Nonnull final PsiModifierListOwner owner) {
    return JavaSuppressionUtil.getInspectionIdsSuppressedInAnnotation(owner);
  }

  @Override
  @Nullable
  public String getSuppressedInspectionIdsIn(@Nonnull PsiElement element) {
    return JavaSuppressionUtil.getSuppressedInspectionIdsIn(element);
  }

  @Override
  @javax.annotation.Nullable
  public PsiElement getElementToolSuppressedIn(@Nonnull final PsiElement place, final String toolId) {
    return JavaSuppressionUtil.getElementToolSuppressedIn(place, toolId);
  }

  @Override
  public boolean canHave15Suppressions(@Nonnull final PsiElement file) {
    return JavaSuppressionUtil.canHave15Suppressions(file);
  }

  @Override
  public boolean alreadyHas14Suppressions(@Nonnull final PsiDocCommentOwner commentOwner) {
    return JavaSuppressionUtil.alreadyHas14Suppressions(commentOwner);
  }
}
