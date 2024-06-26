/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.codeInsight.daemon.lambda;

import org.jetbrains.annotations.NonNls;
import com.intellij.codeInsight.daemon.LightDaemonAnalyzerTestCase;

public abstract class Interface8MethodsHighlightingTest extends LightDaemonAnalyzerTestCase {
  @NonNls static final String BASE_PATH = "/codeInsight/daemonCodeAnalyzer/lambda/interfaceMethods";

  public void testStaticMethod() { doTest(); }
  public void testNotInheritFromUnrelatedDefault() { doTest(true, false); }
  public void testDefaultMethodVisibility() { doTest(true, false); }
  public void testInheritUnrelatedDefaults() { doTest(true, false); }
  public void testExtensionMethods() { doTest(false, false); }
  public void testInheritDefaultMethodInInterface() { doTest(false, false); }
  public void testStaticMethodsInFunctionalInterface() { doTest(false, false); }
  public void testCyclicSubstitutor() { doTest(false, false); }
  public void testThisAccessibility() { doTest(false, false); }

  private void doTest() {
    doTest(false, false);
  }

  private void doTest(boolean checkWarnings, boolean checkInfos) {
    doTest(BASE_PATH + "/" + getTestName(false) + ".java", checkWarnings, checkInfos);
  }
}
