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
package com.intellij.java.impl.internal.psiView;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.impl.idea.util.PlatformIcons;
import consulo.language.psi.PsiElement;
import consulo.project.Project;

import javax.swing.*;

/**
 * @author yole
 * @author Konstantin Bulenkov
 */
@ExtensionImpl
public class PsiViewerCodeFragmentExtension extends JavaPsiViewerExtension {
  public String getName() {
    return "Java Code Block";
  }

  public Icon getIcon() {
    return PlatformIcons.CLASS_INITIALIZER;
  }

  public PsiElement createElement(Project project, String text) {
    return getFactory(project).createCodeBlockFromText(text, null);
  }
}
