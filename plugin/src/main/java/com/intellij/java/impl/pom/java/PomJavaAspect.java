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
package com.intellij.java.impl.pom.java;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;
import consulo.language.pom.PomModel;
import consulo.language.pom.PomModelAspect;

@ServiceAPI(value = ComponentScope.PROJECT, lazy = false)
public abstract class PomJavaAspect implements PomModelAspect {
  public static PomJavaAspect getInstance(Project project) {
    return project.getComponent(PomJavaAspect.class);
  }

  public static PomJavaAspect getInstance(PomModel model) {
    return model.getModelAspect(PomJavaAspect.class);
  }
}