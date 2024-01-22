/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.java.impl.ide.fileTemplates;

import com.intellij.java.language.impl.codeInsight.template.JavaTemplateUtil;
import consulo.annotation.component.ExtensionImpl;
import consulo.fileTemplate.FileTemplate;
import consulo.language.psi.PsiDirectory;
import consulo.util.collection.ArrayUtil;

import jakarta.annotation.Nonnull;

@ExtensionImpl
public class JavaInternalTemplatesHandler extends JavaCreateFromTemplateHandler {
  @Override
  public boolean handlesTemplate(@Nonnull FileTemplate template) {
    return ArrayUtil.contains(template.getName(), JavaTemplateUtil.INTERNAL_CLASS_TEMPLATES);
  }

  @Override
  public boolean canCreate(PsiDirectory[] dirs) {
    return false;
  }
}