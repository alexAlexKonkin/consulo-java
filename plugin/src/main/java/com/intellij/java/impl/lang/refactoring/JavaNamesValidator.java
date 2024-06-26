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
package com.intellij.java.impl.lang.refactoring;

import com.intellij.java.language.JavaLanguage;
import com.intellij.java.language.psi.PsiNameHelper;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.refactoring.NamesValidator;
import consulo.project.Project;

import jakarta.annotation.Nonnull;

/**
 * Default NamesValidator interface implementation. Uses java language keyword set and java language rules for identifier.
 */
@ExtensionImpl
public class JavaNamesValidator implements NamesValidator {
  @Override
  public boolean isKeyword(String name, Project project) {
    return PsiNameHelper.getInstance(project).isKeyword(name);
  }

  @Override
  public boolean isIdentifier(String name, Project project) {
    return PsiNameHelper.getInstance(project).isIdentifier(name);
  }

  @Nonnull
  @Override
  public Language getLanguage() {
    return JavaLanguage.INSTANCE;
  }
}
