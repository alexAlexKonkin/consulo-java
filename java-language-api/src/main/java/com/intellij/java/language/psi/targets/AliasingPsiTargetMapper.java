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
package com.intellij.java.language.psi.targets;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.language.pom.PomTarget;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface AliasingPsiTargetMapper {
  ExtensionPointName<AliasingPsiTargetMapper> EP_NAME = ExtensionPointName.create(AliasingPsiTargetMapper.class);

  @NotNull
  Set<AliasingPsiTarget> getTargets(@NotNull PomTarget target);
}
