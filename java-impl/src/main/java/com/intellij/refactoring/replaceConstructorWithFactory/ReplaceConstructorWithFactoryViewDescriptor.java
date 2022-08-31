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
package com.intellij.refactoring.replaceConstructorWithFactory;

import com.intellij.java.language.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.java.language.psi.PsiMethod;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.ui.UsageViewDescriptorAdapter;
import javax.annotation.Nonnull;

/**
 * @author dsl
 */
class ReplaceConstructorWithFactoryViewDescriptor extends UsageViewDescriptorAdapter {
  private final PsiMethod myConstructor;
  private final PsiClass myClass;

  public ReplaceConstructorWithFactoryViewDescriptor(
    PsiMethod constructor) {
    super();
    myConstructor = constructor;
    myClass = null;
  }

  public ReplaceConstructorWithFactoryViewDescriptor(PsiClass aClass) {
    super();
    myClass = aClass;
    myConstructor = null;
  }

  @Nonnull
  public PsiElement[] getElements() {
    if (myConstructor != null) {
      return new PsiElement[] {myConstructor};
    } else {
      return new PsiElement[] {myClass};
    }
  }

  public String getProcessedElementsHeader() {
    if (myConstructor != null) {
      return RefactoringBundle.message("replace.constructor.with.factory.method");
    } else {
      return RefactoringBundle.message("replace.default.constructor.with.factory.method");
    }
  }
}
