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
package com.intellij.ide.util;

import javax.annotation.Nonnull;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;

import javax.annotation.Nullable;

/**
 * User: anna
 * Date: Jan 25, 2005
 */
public class TreeClassChooserFactoryImpl extends TreeClassChooserFactory {
  private final Project myProject;

  public TreeClassChooserFactoryImpl(@Nonnull Project project) {
    myProject = project;
  }

  @Nonnull
  public TreeClassChooser createWithInnerClassesScopeChooser(String title,
                                                             GlobalSearchScope scope,
                                                             final ClassFilter classFilter,
                                                             PsiClass initialClass) {
    return TreeJavaClassChooserDialog.withInnerClasses(title, myProject, scope, classFilter, initialClass);
  }

  @Nonnull
  public TreeClassChooser createNoInnerClassesScopeChooser(String title,
                                                           GlobalSearchScope scope,
                                                           ClassFilter classFilter,
                                                           PsiClass initialClass) {
    return new TreeJavaClassChooserDialog(title, myProject, scope, classFilter, initialClass);
  }

  @Nonnull
  public TreeClassChooser createProjectScopeChooser(String title, PsiClass initialClass) {
    return new TreeJavaClassChooserDialog(title, myProject, initialClass);
  }

  @Nonnull
  public TreeClassChooser createProjectScopeChooser(String title) {
    return new TreeJavaClassChooserDialog(title, myProject);
  }

  @Nonnull
  public TreeClassChooser createAllProjectScopeChooser(String title) {
    return new TreeJavaClassChooserDialog(title, myProject, GlobalSearchScope.allScope(myProject), null, null);
  }

  @Nonnull
  public TreeClassChooser createInheritanceClassChooser(String title,
                                                        GlobalSearchScope scope,
                                                        PsiClass base,
                                                        boolean acceptsSelf,
                                                        boolean acceptInner,
                                                        Condition<? super PsiClass> additionalCondition) {
    ClassFilter classFilter = new TreeJavaClassChooserDialog.InheritanceJavaClassFilterImpl(base, acceptsSelf, acceptInner, additionalCondition);
    return new TreeJavaClassChooserDialog(title, myProject, scope, classFilter, base, null, false);
  }

  @Nonnull
  public TreeClassChooser createInheritanceClassChooser(String title, GlobalSearchScope scope, PsiClass base, PsiClass initialClass) {
    return createInheritanceClassChooser(title, scope, base, initialClass, null);
  }

  @Nonnull
  public TreeClassChooser createInheritanceClassChooser(String title, GlobalSearchScope scope, PsiClass base, PsiClass initialClass,
                                                        ClassFilter classFilter) {
    return new TreeJavaClassChooserDialog(title, myProject, scope, classFilter, base, initialClass, false);
  }

  @Nonnull
  public TreeFileChooser createFileChooser(@Nonnull String title,
                                           final PsiFile initialFile,
                                           FileType fileType,
                                           TreeFileChooser.PsiFileFilter filter) {
    return new TreeFileChooserDialog(myProject, title, initialFile, fileType, filter, false, false);
  }

  @Nonnull
  public
  TreeFileChooser createFileChooser(@Nonnull String title,
                                    @Nullable PsiFile initialFile,
                                    @javax.annotation.Nullable FileType fileType,
                                    @javax.annotation.Nullable TreeFileChooser.PsiFileFilter filter,
                                    boolean disableStructureProviders) {
    return new TreeFileChooserDialog(myProject, title, initialFile, fileType, filter, disableStructureProviders, false);
  }


  @Nonnull
  public TreeFileChooser createFileChooser(@Nonnull String title, @Nullable PsiFile initialFile, @Nullable FileType fileType,
                                           @Nullable TreeFileChooser.PsiFileFilter filter,
                                           boolean disableStructureProviders,
                                           boolean showLibraryContents) {
    return new TreeFileChooserDialog(myProject, title, initialFile, fileType, filter, disableStructureProviders, showLibraryContents);
  }
}
