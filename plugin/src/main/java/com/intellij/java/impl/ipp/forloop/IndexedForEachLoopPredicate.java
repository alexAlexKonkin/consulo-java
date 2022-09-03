/*
 * Copyright 2003-2009 Dave Griffith, Bas Leijdekkers
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
package com.intellij.java.impl.ipp.forloop;

import com.intellij.java.language.psi.*;
import consulo.project.Project;
import com.intellij.psi.*;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.ast.IElementType;
import com.intellij.java.language.psi.util.InheritanceUtil;
import com.intellij.java.impl.ipp.base.PsiElementPredicate;
import com.intellij.java.impl.ipp.psiutils.ErrorUtil;
import consulo.java.language.module.util.JavaClassNames;

class IndexedForEachLoopPredicate implements PsiElementPredicate {

  public boolean satisfiedBy(PsiElement element) {
    if (!(element instanceof PsiJavaToken)) {
      return false;
    }
    final PsiJavaToken token = (PsiJavaToken)element;
    final IElementType tokenType = token.getTokenType();
    if (!JavaTokenType.FOR_KEYWORD.equals(tokenType)) {
      return false;
    }
    final PsiElement parent = element.getParent();
    if (!(parent instanceof PsiForeachStatement)) {
      return false;
    }
    final PsiForeachStatement foreachStatement =
      (PsiForeachStatement)parent;
    final PsiExpression iteratedValue = foreachStatement.getIteratedValue();
    if (iteratedValue == null) {
      return false;
    }
    final PsiType type = iteratedValue.getType();
    if (!(type instanceof PsiArrayType)) {
      if (!(type instanceof PsiClassType)) {
        return false;
      }
      final PsiClassType classType = (PsiClassType)type;
      final PsiClass aClass = classType.resolve();
      if (aClass == null) {
        return false;
      }
      final Project project = element.getProject();
      final JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
      final PsiClass listInterface =
        psiFacade.findClass(JavaClassNames.JAVA_UTIL_LIST,
                            GlobalSearchScope.allScope(project));
      if (listInterface == null ||
          !InheritanceUtil.isInheritorOrSelf(aClass,
                                             listInterface, true)) {
        return false;
      }
    }
    return !ErrorUtil.containsError(parent);
  }
}