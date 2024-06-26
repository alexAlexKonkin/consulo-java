/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.siyeh.ig;

import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.scheme.LocalInspectionToolWrapper;
import com.intellij.openapi.application.PluginPathManager;
import com.intellij.testFramework.InspectionTestCase;
import org.jetbrains.annotations.NonNls;

/**
 * @author Alexey
 */
public abstract class IGInspectionTestCase extends InspectionTestCase {
  @Override
  protected String getTestDataPath() {
    return PluginPathManager.getPluginHomePath("InspectionGadgets") + "/test";
  }

  @Override
  public void doTest(@NonNls final String folderName, final LocalInspectionTool tool) {
    super.doTest(folderName, new LocalInspectionToolWrapper(tool), "java 1.5");
  }
}
