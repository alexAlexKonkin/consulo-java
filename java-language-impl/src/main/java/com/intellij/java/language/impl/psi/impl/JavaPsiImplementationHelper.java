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
package com.intellij.java.language.impl.psi.impl;

import com.intellij.java.language.LanguageLevel;
import com.intellij.java.language.psi.*;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.ide.ServiceManager;
import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author yole
 */
@ServiceAPI(ComponentScope.PROJECT)
public abstract class JavaPsiImplementationHelper {
  public static JavaPsiImplementationHelper getInstance(Project project) {
    return ServiceManager.getService(project, JavaPsiImplementationHelper.class);
  }

  public abstract PsiClass getOriginalClass(PsiClass psiClass);

  public abstract PsiElement getClsFileNavigationElement(PsiJavaFile clsFile);

  /**
   * For files under a library source root, returns the language level configured for the corresponding classes root.
   *
   * @param virtualFile virtual file for which language level is requested.
   * @return language level for classes root or null if file is not under a library source root or no matching classes root is found.
   */
  @Nullable
  public abstract LanguageLevel getClassesLanguageLevel(VirtualFile virtualFile);

  public abstract ASTNode getDefaultImportAnchor(PsiImportList list, PsiImportStatementBase statement);

  @Nullable
  public abstract PsiElement getDefaultMemberAnchor(@Nonnull PsiClass psiClass, @Nonnull PsiMember firstPsi);

  public abstract void setupCatchBlock(String exceptionName, PsiElement context, PsiCatchSection element);
}