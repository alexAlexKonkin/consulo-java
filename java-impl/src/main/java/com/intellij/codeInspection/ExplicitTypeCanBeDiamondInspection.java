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
package com.intellij.codeInspection;

import javax.annotation.Nonnull;

import com.intellij.codeInsight.daemon.GroupNames;
import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.java.analysis.impl.codeInspection.BaseJavaLocalInspectionTool;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiDiamondTypeUtil;
import consulo.logging.Logger;
import org.jetbrains.annotations.Nls;

/**
 * User: anna
 * Date: 1/28/11
 */
public class ExplicitTypeCanBeDiamondInspection extends BaseJavaLocalInspectionTool
{
  public static final Logger LOG = Logger.getInstance(ExplicitTypeCanBeDiamondInspection.class);

  @Nls
  @Nonnull
  @Override
  public String getGroupDisplayName() {
    return GroupNames.LANGUAGE_LEVEL_SPECIFIC_GROUP_NAME;
  }

  @Nls
  @Nonnull
  @Override
  public String getDisplayName() {
    return "Explicit type can be replaced with <>";
  }

  @Override
  public boolean isEnabledByDefault() {
    return true;
  }

  @Nonnull
  @Override
  public String getShortName() {
    return "Convert2Diamond";
  }

  @Nonnull
  @Override
  public PsiElementVisitor buildVisitor(@Nonnull final ProblemsHolder holder, boolean isOnTheFly) {
    return new JavaElementVisitor() {
      @Override
      public void visitNewExpression(PsiNewExpression expression) {
        if (PsiDiamondTypeUtil.canCollapseToDiamond(expression, expression, null)) {
          final PsiJavaCodeReferenceElement classReference = expression.getClassOrAnonymousClassReference();
          LOG.assertTrue(classReference != null);
          final PsiReferenceParameterList parameterList = classReference.getParameterList();
          LOG.assertTrue(parameterList != null);
          holder.registerProblem(parameterList,  "Explicit type argument #ref #loc can be replaced with <>",
                                 ProblemHighlightType.LIKE_UNUSED_SYMBOL, new ReplaceWithDiamondFix());
        }
      }
    };
  }

  private static class ReplaceWithDiamondFix implements LocalQuickFix, HighPriorityAction {
    @Nonnull
    @Override
    public String getName() {
      return "Replace with <>";
    }

    @Nonnull
    @Override
    public String getFamilyName() {
      return getName();
    }

    @Override
    public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
      PsiDiamondTypeUtil.replaceExplicitWithDiamond(descriptor.getPsiElement());
    }
  }
}
