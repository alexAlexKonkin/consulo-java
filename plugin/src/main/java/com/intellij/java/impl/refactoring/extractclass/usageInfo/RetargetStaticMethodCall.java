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
package com.intellij.java.impl.refactoring.extractclass.usageInfo;

import com.intellij.java.language.psi.PsiExpression;
import com.intellij.java.language.psi.PsiImportStaticStatement;
import com.intellij.java.language.psi.PsiMethodCallExpression;
import com.intellij.java.language.psi.PsiReferenceExpression;
import consulo.language.psi.*;
import com.intellij.java.impl.refactoring.psi.MutationUtils;
import com.intellij.java.impl.refactoring.util.FixableUsageInfo;
import consulo.language.util.IncorrectOperationException;

public class RetargetStaticMethodCall extends FixableUsageInfo {
    private final String delegateClassName;
    private final PsiMethodCallExpression call;

     public RetargetStaticMethodCall(PsiMethodCallExpression call, String delegateClassName) {
        super(call);
        this.call = call;
        this.delegateClassName = delegateClassName;
    }

    public void fixUsage() throws IncorrectOperationException {
        final PsiReferenceExpression methodExpression = call.getMethodExpression();
        final PsiExpression qualifier = (PsiExpression) methodExpression.getQualifier();
        if (qualifier == null) {
          final PsiElement resolveScope = call.resolveMethodGenerics().getCurrentFileResolveScope();
          if (!(resolveScope instanceof PsiImportStaticStatement)) {
            MutationUtils.replaceExpression(delegateClassName + '.' + call.getText(), call);
          }
        } else {
            MutationUtils.replaceExpression(delegateClassName , qualifier);
        }
    }
}
