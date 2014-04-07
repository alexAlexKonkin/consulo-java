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

/*
 * User: anna
 * Date: 04-Feb-2008
 */
package com.intellij.ide.navigationToolbar;

import java.util.Collection;
import java.util.Collections;

import org.jetbrains.annotations.Nullable;
import com.intellij.analysis.AnalysisScopeBundle;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiJavaPackage;
import com.intellij.psi.presentation.java.ClassPresentationUtil;

public class JavaNavBarExtension implements NavBarModelExtension{
  public String getPresentableText(final Object object) {
    if (object instanceof PsiClass) {
      return ClassPresentationUtil.getNameForClass((PsiClass)object, false);
    }
    else if (object instanceof PsiJavaPackage) {
      final String name = ((PsiJavaPackage)object).getName();
      return name != null ? name : AnalysisScopeBundle.message("dependencies.tree.node.default.package.abbreviation");
    }
    return null;
  }

  public PsiElement getParent(final PsiElement psiElement) {
    if (psiElement instanceof PsiJavaPackage) {
      final PsiJavaPackage parentPackage = ((PsiJavaPackage)psiElement).getParentPackage();
      if (parentPackage != null && parentPackage.getQualifiedName().length() > 0) {
        return parentPackage;
      }
    }
    return null;
  }


  @Nullable
  public PsiElement adjustElement(final PsiElement psiElement) {
    final ProjectFileIndex index = ProjectRootManager.getInstance(psiElement.getProject()).getFileIndex();
    final PsiFile containingFile = psiElement.getContainingFile();
    if (containingFile != null) {
      final VirtualFile file = containingFile.getVirtualFile();
      if (file != null && (index.isInSourceContent(file) || index.isInLibraryClasses(file) || index.isInLibrarySource(file))) {
        if (psiElement instanceof PsiJavaFile) {
          final PsiJavaFile psiJavaFile = (PsiJavaFile)psiElement;
          if (psiJavaFile.getViewProvider().getBaseLanguage() == JavaLanguage.INSTANCE) {
            final PsiClass[] psiClasses = psiJavaFile.getClasses();
            if (psiClasses.length == 1) {
              return psiClasses[0];
            }
          }
        }
        if (psiElement instanceof PsiClass) return psiElement;
      }
      return containingFile;
    }
    return psiElement.isPhysical() ? psiElement : null;
  }

  @Override
  public Collection<VirtualFile> additionalRoots(Project project) {
    return Collections.emptyList();
  }
}
