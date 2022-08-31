/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
 * Date: 14-Jan-2008
 */
package com.intellij.java.impl.analysis;

import com.intellij.analysis.AnalysisScope;
import com.intellij.analysis.AnalysisScopeBundle;
import com.intellij.java.language.psi.*;
import com.intellij.java.language.psi.search.PackageScope;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import consulo.psi.PsiPackage;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

public class JavaAnalysisScope extends AnalysisScope {
  public static final int PACKAGE = 5;

  public JavaAnalysisScope(PsiPackage pack, Module module) {
    super(pack.getProject());
    myModule = module;
    myElement = pack;
    myType = PACKAGE;
  }

  public JavaAnalysisScope(final PsiJavaFile psiFile) {
    super(psiFile);
  }

  @Override
  @Nonnull
  public AnalysisScope getNarrowedComplementaryScope(@Nonnull Project defaultProject) {
    final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(defaultProject).getFileIndex();
    final HashSet<Module> modules = new HashSet<Module>();
    if (myType == FILE) {
      if (myElement instanceof PsiJavaFile/* && !FileTypeUtils.isInServerPageFile(myElement)*/) {
        PsiJavaFile psiJavaFile = (PsiJavaFile) myElement;
        final PsiClass[] classes = psiJavaFile.getClasses();
        boolean onlyPackLocalClasses = true;
        for (final PsiClass aClass : classes) {
          if (aClass.hasModifierProperty(PsiModifier.PUBLIC)) {
            onlyPackLocalClasses = false;
          }
        }
        if (onlyPackLocalClasses) {
          final PsiDirectory psiDirectory = psiJavaFile.getContainingDirectory();
          if (psiDirectory != null) {
            return new JavaAnalysisScope(JavaDirectoryService.getInstance().getPackage(psiDirectory),
                null);
          }
        }
      }
    } else if (myType == PACKAGE) {
      final PsiDirectory[] directories = ((PsiPackage) myElement).getDirectories();
      for (PsiDirectory directory : directories) {
        modules.addAll(getAllInterestingModules(fileIndex, directory.getVirtualFile()));
      }
      return collectScopes(defaultProject, modules);
    }
    return super.getNarrowedComplementaryScope(defaultProject);
  }


  @Nonnull
  @Override
  public String getShortenName() {
    if (myType == PACKAGE) {
      return AnalysisScopeBundle.message("scope.package", ((PsiPackage) myElement).getQualifiedName());
    }
    return super.getShortenName();
  }

  @Nonnull
  @Override
  public String getDisplayName() {
    if (myType == PACKAGE) {
      return AnalysisScopeBundle.message("scope.package", ((PsiPackage) myElement).getQualifiedName());
    }
    return super.getDisplayName();
  }

  @Override
  protected void initFilesSet() {
    if (myType == PACKAGE) {
      myFilesSet = new HashSet<VirtualFile>();
      accept(createFileSearcher());
      return;
    }
    super.initFilesSet();
  }

  @Override
  public boolean accept(@Nonnull Processor<VirtualFile> processor) {
    if (myElement instanceof PsiJavaPackage) {
      final PsiJavaPackage pack = (PsiJavaPackage) myElement;
      final Set<PsiDirectory> dirs = new HashSet<PsiDirectory>();
      ApplicationManager.getApplication().runReadAction(new Runnable() {
        @Override
        public void run() {
          ContainerUtil.addAll(dirs, pack.getDirectories(GlobalSearchScope.projectScope(myElement.getProject
              ())));
        }
      });
      for (PsiDirectory dir : dirs) {
        if (!accept(dir, processor)) {
          return false;
        }
      }
      return true;
    }
    return super.accept(processor);
  }

  @Nonnull
  @Override
  public SearchScope toSearchScope() {
    if (myType == PACKAGE) {
      return new PackageScope((PsiJavaPackage) myElement, true, true);
    }
    return super.toSearchScope();
  }
}
