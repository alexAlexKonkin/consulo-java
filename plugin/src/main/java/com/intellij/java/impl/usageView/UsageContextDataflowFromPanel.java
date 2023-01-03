/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package com.intellij.java.impl.usageView;

import consulo.ide.impl.idea.usages.impl.UsageViewImpl;
import consulo.project.Project;
import consulo.usage.UsageContextPanel;
import consulo.usage.UsageView;
import consulo.usage.UsageViewPresentation;

import javax.annotation.Nonnull;

public class UsageContextDataflowFromPanel extends UsageContextDataflowToPanel {

  public UsageContextDataflowFromPanel(@Nonnull Project project, @Nonnull UsageViewPresentation presentation) {
    super(project, presentation);
  }


  @Override
  protected boolean isDataflowToThis() {
    return false;
  }
}