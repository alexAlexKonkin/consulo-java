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
 * User: anna
 * Date: 27-Aug-2008
 */
package com.intellij.java.impl.refactoring.inlineSuperClass.usageInfo;

import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiJavaCodeReferenceElement;
import com.intellij.java.impl.refactoring.safeDelete.usageInfo.SafeDeleteExtendsClassUsageInfo;
import com.intellij.java.impl.refactoring.util.FixableUsageInfo;
import consulo.language.util.IncorrectOperationException;

public class ReplaceExtendsListUsageInfo extends FixableUsageInfo {
  private final SafeDeleteExtendsClassUsageInfo mySafeDeleteUsageInfo;

  public ReplaceExtendsListUsageInfo(PsiJavaCodeReferenceElement element, final PsiClass superClass, final PsiClass targetClass) {
    super(element);
    mySafeDeleteUsageInfo = new SafeDeleteExtendsClassUsageInfo(element, superClass, targetClass);
  }

  public void fixUsage() throws IncorrectOperationException {
    if (mySafeDeleteUsageInfo.isSafeDelete()) {
      mySafeDeleteUsageInfo.deleteElement();
    }
  }
}
