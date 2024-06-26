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
package com.intellij.java.impl.refactoring.openapi.impl;

import com.intellij.java.impl.refactoring.MoveClassesOrPackagesRefactoring;
import com.intellij.java.impl.refactoring.MoveDestination;
import com.intellij.java.impl.refactoring.PackageWrapper;
import com.intellij.java.impl.refactoring.move.moveClassesOrPackages.MoveClassesOrPackagesProcessor;
import consulo.language.editor.refactoring.RefactoringImpl;
import consulo.language.psi.PsiElement;
import consulo.project.Project;

import java.util.List;

/**
 * @author dsl
 */
public class MoveClassesOrPackagesRefactoringImpl extends RefactoringImpl<MoveClassesOrPackagesProcessor> implements MoveClassesOrPackagesRefactoring {


  public MoveClassesOrPackagesRefactoringImpl(Project project, PsiElement[] elements, MoveDestination moveDestination) {
    super(new MoveClassesOrPackagesProcessor(project, elements, moveDestination, true, true, null));
  }

  public List<PsiElement> getElements() {
    return myProcessor.getElements();
  }

  public PackageWrapper getTargetPackage() {
    return myProcessor.getTargetPackage();
  }

  public void setSearchInComments(boolean value) {
    myProcessor.setSearchInComments(value);
  }

  public void setSearchInNonJavaFiles(boolean value) {
    myProcessor.setSearchInNonJavaFiles(value);
  }

  public boolean isSearchInComments() {
    return myProcessor.isSearchInComments();
  }

  public boolean isSearchInNonJavaFiles() {
    return myProcessor.isSearchInNonJavaFiles();
  }
}
