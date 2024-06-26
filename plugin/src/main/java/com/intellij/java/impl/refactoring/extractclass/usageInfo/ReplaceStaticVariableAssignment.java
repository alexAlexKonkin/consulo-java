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

import com.intellij.java.language.psi.PsiReferenceExpression;
import com.intellij.java.impl.refactoring.psi.MutationUtils;
import com.intellij.java.impl.refactoring.util.FixableUsageInfo;
import consulo.language.util.IncorrectOperationException;

public class ReplaceStaticVariableAssignment extends FixableUsageInfo {
    private final PsiReferenceExpression reference;
    private final String originalClassName;

    public ReplaceStaticVariableAssignment(PsiReferenceExpression reference,
                                    String originalClassName) {
        super(reference);
        this.originalClassName = originalClassName;
        this.reference = reference;
    }

    public void fixUsage() throws IncorrectOperationException {
      MutationUtils.replaceExpression(originalClassName + '.' + reference.getText(), reference);
    }
}
