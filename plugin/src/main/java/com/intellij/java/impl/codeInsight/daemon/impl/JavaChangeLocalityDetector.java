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
 * @author max
 */
package com.intellij.java.impl.codeInsight.daemon.impl;

import javax.annotation.Nonnull;

import consulo.language.editor.ChangeLocalityDetector;
import com.intellij.java.language.psi.PsiAnonymousClass;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiCodeBlock;
import com.intellij.java.language.psi.PsiMethod;
import consulo.language.psi.*;

import javax.annotation.Nullable;

public class JavaChangeLocalityDetector implements ChangeLocalityDetector {
  @Override
  @Nullable
  public PsiElement getChangeHighlightingDirtyScopeFor(@Nonnull final PsiElement element) {
    // optimization
    PsiElement parent = element.getParent();
    PsiElement grand;
    if (element instanceof PsiCodeBlock
        && parent instanceof PsiMethod
        && !((PsiMethod)parent).isConstructor()
        && (grand = parent.getParent()) instanceof PsiClass
        && !(grand instanceof PsiAnonymousClass)) {
      // for changes inside method, rehighlight codeblock only
      // do not use this optimization for constructors and class initializers - to update non-initialized fields
      return parent;
    }
    return null;
  }
}