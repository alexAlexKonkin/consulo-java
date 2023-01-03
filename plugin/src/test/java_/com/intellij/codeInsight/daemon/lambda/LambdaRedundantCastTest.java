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

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NonNls;
import com.intellij.codeInsight.daemon.LightDaemonAnalyzerTestCase;
import consulo.language.editor.inspection.LocalInspectionTool;
import com.intellij.java.analysis.impl.codeInspection.redundantCast.RedundantCastInspection;

public abstract class LambdaRedundantCastTest extends LightDaemonAnalyzerTestCase {
  @NonNls static final String BASE_PATH = "/codeInsight/daemonCodeAnalyzer/lambda/redundantCast";

  @Nonnull
  @Override
  protected LocalInspectionTool[] configureLocalInspectionTools() {
    return new LocalInspectionTool[]{
      new RedundantCastInspection()
    };
  }

  public void testIntersection() { doTest(); }
  public void testSer() { doTest(); }
  private void doTest() {
    doTest(BASE_PATH + "/" + getTestName(false) + ".java", true, false);
  }
}