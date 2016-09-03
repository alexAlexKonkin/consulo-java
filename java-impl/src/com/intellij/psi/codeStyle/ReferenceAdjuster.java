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
package com.intellij.psi.codeStyle;

import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageExtension;
import com.intellij.openapi.project.Project;

/**
 * @author Max Medvedev
 */
public interface ReferenceAdjuster {
  ASTNode process(ASTNode element, boolean addImports, boolean incompleteCode, boolean useFqInJavadoc, boolean useFqInCode);
  ASTNode process(ASTNode element, boolean addImports, boolean incompleteCode, Project project);

  void processRange(ASTNode element, int startOffset, int endOffset, boolean useFqInJavadoc, boolean useFqInCode);
  void processRange(ASTNode element, int startOffset, int endOffset, Project project);

  class Extension extends LanguageExtension<ReferenceAdjuster> {
    private static final Extension INSTANCE = new Extension();

    public Extension() {
      super("consulo.java.codeStyle.referenceAdjuster");
    }

    public static ReferenceAdjuster getReferenceAdjuster(Language language) {
      return INSTANCE.forLanguage(language);
    }
  }
}
