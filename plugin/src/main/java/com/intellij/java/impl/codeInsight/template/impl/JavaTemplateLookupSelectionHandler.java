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
package com.intellij.java.impl.codeInsight.template.impl;

import com.intellij.java.language.impl.codeInsight.template.JavaTemplateUtilImpl;
import consulo.document.Document;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.template.TemplateLookupSelectionHandler;
import consulo.language.psi.PsiFile;

/**
 * @author yole
 */
public class JavaTemplateLookupSelectionHandler implements TemplateLookupSelectionHandler {
  @Override
  public void itemSelected(final LookupElement item,
                           final PsiFile psiFile, final Document document, final int segmentStart, final int segmentEnd) {
    JavaTemplateUtilImpl.updateTypeBindings(item.getObject(), psiFile, document, segmentStart, segmentEnd);
  }
}
