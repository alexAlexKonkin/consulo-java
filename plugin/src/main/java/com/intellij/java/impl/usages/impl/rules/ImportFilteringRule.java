/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.java.impl.usages.impl.rules;

import com.intellij.java.language.psi.PsiImportList;
import com.intellij.java.language.psi.PsiJavaFile;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.usage.Usage;
import consulo.usage.rule.PsiElementUsage;

import jakarta.annotation.Nonnull;

/**
 * @author Eugene Zhuravlev
 *         Date: Jan 17, 2005
 */
@ExtensionImpl
public class ImportFilteringRule implements consulo.usage.rule.ImportFilteringRule {
  public boolean isVisible(@Nonnull Usage usage) {
    if (usage instanceof PsiElementUsage) {
      final PsiElement psiElement = ((PsiElementUsage)usage).getElement();
      final PsiFile containingFile = psiElement.getContainingFile();
      if (containingFile instanceof PsiJavaFile) {
        // check whether the element is in the import list
        final PsiImportList importList = PsiTreeUtil.getParentOfType(psiElement, PsiImportList.class, true);
        return importList == null;
      }
    }
    return true;
  }
}
