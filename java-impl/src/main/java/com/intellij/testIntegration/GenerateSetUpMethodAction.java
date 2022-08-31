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
package com.intellij.testIntegration;

import com.intellij.java.language.testIntegration.TestFramework;
import com.intellij.java.language.psi.PsiClass;

public class GenerateSetUpMethodAction extends BaseGenerateTestSupportMethodAction {
  public GenerateSetUpMethodAction() {
    super(TestIntegrationUtils.MethodKind.SET_UP);
  }

  @Override
  protected boolean isValidFor(PsiClass targetClass, TestFramework framework) {
    return super.isValidFor(targetClass, framework) && framework.findSetUpMethod(targetClass) == null;
  }
}
