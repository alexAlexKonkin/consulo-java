/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.refactoring.safeDelete;

import java.util.List;

import com.intellij.lang.LanguageExtension;
import com.intellij.java.language.psi.PsiMethod;
import com.intellij.java.language.psi.PsiParameter;
import com.intellij.psi.PsiReference;
import com.intellij.usageView.UsageInfo;

/**
 * @author Max Medvedev
 */
public interface JavaSafeDeleteDelegate {
  LanguageExtension<JavaSafeDeleteDelegate> EP =
    new LanguageExtension<JavaSafeDeleteDelegate>("consulo.java.refactoring.safeDelete.JavaSafeDeleteDelegate");

  void createUsageInfoForParameter(final PsiReference reference,
                                   final List<UsageInfo> usages,
                                   final PsiParameter parameter,
                                   final PsiMethod method);
}
