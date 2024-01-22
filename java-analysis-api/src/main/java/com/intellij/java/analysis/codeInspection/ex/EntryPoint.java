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

package com.intellij.java.analysis.codeInspection.ex;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.language.editor.inspection.reference.RefElement;
import consulo.language.psi.PsiElement;
import consulo.logging.Logger;
import consulo.util.xml.serializer.JDOMExternalizable;
import org.jdom.Element;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ExtensionAPI(ComponentScope.APPLICATION)
@Deprecated
public abstract class EntryPoint implements JDOMExternalizable, Cloneable {
  private static final Logger LOG = Logger.getInstance(EntryPoint.class);

  @Nonnull
  public abstract String getDisplayName();

  public abstract boolean isEntryPoint(RefElement refElement, PsiElement psiElement);

  public abstract boolean isEntryPoint(PsiElement psiElement);

  public abstract boolean isSelected();

  public abstract void setSelected(boolean selected);

  public boolean showUI() {
    return true;
  }

  @Nullable
  public String[] getIgnoreAnnotations() {
    return null;
  }

  @Override
  public EntryPoint clone() throws CloneNotSupportedException {
    final EntryPoint clone = (EntryPoint) super.clone();
    final Element element = new Element("root");
    try {
      writeExternal(element);
      clone.readExternal(element);
    } catch (Exception e) {
      LOG.error(e);
    }
    return clone;
  }
}