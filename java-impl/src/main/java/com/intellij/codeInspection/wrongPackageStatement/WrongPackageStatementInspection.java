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
package com.intellij.codeInspection.wrongPackageStatement;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NonNls;
import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInsight.daemon.JavaErrorBundle;
import com.intellij.java.analysis.impl.codeInspection.BaseJavaLocalInspectionTool;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.InspectionsBundle;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.MoveToPackageFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.java.language.psi.JavaDirectoryService;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.java.language.psi.PsiJavaCodeReferenceElement;
import com.intellij.java.language.psi.PsiJavaFile;
import com.intellij.java.language.psi.PsiJavaPackage;
import com.intellij.java.language.psi.PsiPackageStatement;
import com.intellij.java.language.psi.PsiSyntheticClass;

/**
 * User: anna
 * Date: 14-Nov-2005
 */
public class WrongPackageStatementInspection extends BaseJavaLocalInspectionTool {
  @Override
  @javax.annotation.Nullable
  public ProblemDescriptor[] checkFile(@Nonnull PsiFile file, @Nonnull InspectionManager manager, boolean isOnTheFly) {
    // does not work in tests since CodeInsightTestCase copies file into temporary location
    if (ApplicationManager.getApplication().isUnitTestMode()) return null;
    if (file instanceof PsiJavaFile) {
      PsiJavaFile javaFile = (PsiJavaFile)file;

      PsiDirectory directory = javaFile.getContainingDirectory();
      if (directory == null) return null;
      PsiJavaPackage dirPackage = JavaDirectoryService.getInstance().getPackage(directory);
      if (dirPackage == null) return null;
      PsiPackageStatement packageStatement = javaFile.getPackageStatement();

      // highlight the first class in the file only
      PsiClass[] classes = javaFile.getClasses();
      if (classes.length == 0 && packageStatement == null || classes.length == 1 && classes[0] instanceof PsiSyntheticClass) return null;

      String packageName = dirPackage.getQualifiedName();
      if (!Comparing.strEqual(packageName, "", true) && packageStatement == null) {
        String description = JavaErrorBundle.message("missing.package.statement", packageName);

        return new ProblemDescriptor[]{manager.createProblemDescriptor(classes[0].getNameIdentifier(), description,
                                                                       new AdjustPackageNameFix(packageName),
                                                                       ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly)};
      }
      if (packageStatement != null) {
        final PsiJavaCodeReferenceElement packageReference = packageStatement.getPackageReference();
        PsiJavaPackage classPackage = (PsiJavaPackage)packageReference.resolve();
        List<LocalQuickFix> availableFixes = new ArrayList<LocalQuickFix>();
        if (classPackage == null || !Comparing.equal(dirPackage.getQualifiedName(), packageReference.getQualifiedName(), true)) {
          availableFixes.add(new AdjustPackageNameFix(packageName));
          MoveToPackageFix moveToPackageFix = new MoveToPackageFix(classPackage != null ? classPackage.getQualifiedName() : packageReference.getQualifiedName());
          if (moveToPackageFix.isAvailable(file)) {
            availableFixes.add(moveToPackageFix);
          }
        }
        if (!availableFixes.isEmpty()){
          String description = JavaErrorBundle.message("package.name.file.path.mismatch",
                                                         packageReference.getQualifiedName(),
                                                         dirPackage.getQualifiedName());
          LocalQuickFix[] fixes = availableFixes.toArray(new LocalQuickFix[availableFixes.size()]);
          ProblemDescriptor descriptor =
            manager.createProblemDescriptor(packageStatement.getPackageReference(), description, isOnTheFly,
                                            fixes, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
          return new ProblemDescriptor[]{descriptor};

        }
      }
    }
    return null;
  }

  @Override
  @Nonnull
  public String getGroupDisplayName() {
    return "";
  }

  @Override
  @Nonnull
  public HighlightDisplayLevel getDefaultLevel() {
    return HighlightDisplayLevel.ERROR;
  }

  @Override
  @Nonnull
  public String getDisplayName() {
    return InspectionsBundle.message("wrong.package.statement");
  }

  @Override
  @Nonnull
  @NonNls
  public String getShortName() {
    return "WrongPackageStatement";
  }

  @Override
  public boolean isEnabledByDefault() {
    return true;
  }
}
