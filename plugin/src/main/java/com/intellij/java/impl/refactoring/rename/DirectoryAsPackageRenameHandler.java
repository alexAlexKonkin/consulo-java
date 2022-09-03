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
package com.intellij.java.impl.refactoring.rename;

import com.intellij.java.language.psi.JavaDirectoryService;
import com.intellij.java.language.psi.PsiJavaPackage;
import com.intellij.java.language.psi.PsiNameHelper;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiDirectory;
import consulo.language.editor.refactoring.BaseRefactoringProcessor;
import consulo.language.editor.refactoring.RefactoringBundle;
import com.intellij.refactoring.move.moveClassesOrPackages.MoveDirectoryWithClassesProcessor;
import consulo.ide.impl.idea.refactoring.rename.DirectoryAsPackageRenameHandlerBase;

/**
 * @author yole
 */
public class DirectoryAsPackageRenameHandler extends DirectoryAsPackageRenameHandlerBase<PsiJavaPackage> {

  @Override
  protected VirtualFile[] occursInPackagePrefixes(PsiJavaPackage aPackage) {
    return aPackage.occursInPackagePrefixes();
  }

  @Override
  protected boolean isIdentifier(String name, Project project) {
    return PsiNameHelper.getInstance(project).isIdentifier(name);
  }

  @Override
  protected String getQualifiedName(PsiJavaPackage aPackage) {
    return aPackage.getQualifiedName();
  }

  @Override
  protected PsiJavaPackage getPackage(PsiDirectory psiDirectory) {
    return JavaDirectoryService.getInstance().getPackage(psiDirectory);
  }

  @Override
  protected BaseRefactoringProcessor createProcessor(final String newQName,
                                                     Project project,
                                                     final PsiDirectory[] dirsToRename,
                                                     boolean searchInComments, boolean searchInNonJavaFiles) {
    return new MoveDirectoryWithClassesProcessor(project, dirsToRename, null, searchInComments, searchInNonJavaFiles, false, null) {
      @Override
      public TargetDirectoryWrapper getTargetDirectory(final PsiDirectory dir) {
        return new TargetDirectoryWrapper(dir.getParentDirectory(), StringUtil.getShortName(newQName));
      }

      @Override
      protected String getTargetName() {
        return newQName;
      }

      @Override
      protected String getCommandName() {
        return RefactoringBundle.message(dirsToRename.length == 1 ? "rename.directory.command.name" : "rename.directories.command.name");
      }
    };
  }
}
