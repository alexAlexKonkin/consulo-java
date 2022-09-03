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

/**
 * created at Sep 11, 2001
 * @author Jeka
 */
package com.intellij.java.impl.refactoring.move.moveInner;

import javax.annotation.Nonnull;

import com.intellij.java.language.psi.PsiClass;
import consulo.language.psi.PsiElement;
import consulo.language.editor.refactoring.RefactoringBundle;
import consulo.usage.UsageViewBundle;
import consulo.usage.UsageViewDescriptor;

class MoveInnerViewDescriptor implements UsageViewDescriptor {

  private final PsiClass myInnerClass;

  public MoveInnerViewDescriptor(PsiClass innerClass) {
    myInnerClass = innerClass;
  }

  @Nonnull
  public PsiElement[] getElements() {
    return new PsiElement[] {myInnerClass};
  }

  public String getProcessedElementsHeader() {
    return RefactoringBundle.message("move.inner.class.to.be.moved");
  }

  public String getCodeReferencesText(int usagesCount, int filesCount) {
    return RefactoringBundle.message("references.to.be.changed", UsageViewBundle.getReferencesString(usagesCount, filesCount));
  }

  public String getCommentReferencesText(int usagesCount, int filesCount) {
    return null;
  }

}
