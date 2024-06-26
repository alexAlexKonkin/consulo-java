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
package com.intellij.java.analysis.impl.psi.util;

import com.intellij.java.language.psi.*;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiMatcherExpression;

public class EnclosingLoopMatcherExpression implements PsiMatcherExpression {
  public static final PsiMatcherExpression INSTANCE = new EnclosingLoopMatcherExpression();

  @Override
  public Boolean match(PsiElement element) {
    if (element instanceof PsiForStatement) return Boolean.TRUE;
    if (element instanceof PsiForeachStatement) return Boolean.TRUE;
    if (element instanceof PsiWhileStatement) return Boolean.TRUE;
    if (element instanceof PsiDoWhileStatement) return Boolean.TRUE;
    if (element instanceof PsiMethod || element instanceof PsiClassInitializer) return null;
    return Boolean.FALSE;
  }
}