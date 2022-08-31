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
package com.intellij.util.xml.ui;

import javax.annotation.Nonnull;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.java.language.psi.JavaPsiFacade;
import com.intellij.psi.PsiManager;
import com.intellij.java.language.psi.PsiType;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.ReferenceEditorWithBrowseButton;
import com.intellij.ui.JavaReferenceEditorUtil;
import com.intellij.util.Function;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.xml.AbstractConvertContext;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.JvmPsiTypeConverterImpl;

/**
 * @author peter
 */
public class PsiTypeControl extends EditorTextFieldControl<PsiTypePanel> {

  public PsiTypeControl(final DomWrapper<String> domWrapper, final boolean commitOnEveryChange) {
    super(domWrapper, commitOnEveryChange);
  }

  @Nonnull
  protected String getValue() {
    final String rawValue = super.getValue();
    try {
      final PsiType psiType = JavaPsiFacade.getInstance(getProject()).getElementFactory().createTypeFromText(rawValue, null);
      final String s = JvmPsiTypeConverterImpl.convertToString(psiType);
      if (s != null) {
        return s;
      }
    }
    catch (IncorrectOperationException e) {
    }
    return rawValue;
  }

  private PsiManager getPsiManager() {
    return PsiManager.getInstance(getProject());
  }

  protected void setValue(String value) {
    final PsiType type = JvmPsiTypeConverterImpl.convertFromString(value, new AbstractConvertContext() {
      @Nonnull
      public DomElement getInvocationElement() {
        return getDomElement();
      }

      public PsiManager getPsiManager() {
        return PsiTypeControl.this.getPsiManager();
      }
    });
    if (type != null) {
      value = type.getCanonicalText();
    }
    super.setValue(value);
  }

  protected EditorTextField getEditorTextField(@Nonnull final PsiTypePanel component) {
    return ((ReferenceEditorWithBrowseButton)component.getComponent(0)).getEditorTextField();
  }

  protected PsiTypePanel createMainComponent(PsiTypePanel boundedComponent, final Project project) {
    if (boundedComponent == null) {
      boundedComponent = new PsiTypePanel();
    }
    return PsiClassControl.initReferenceEditorWithBrowseButton(boundedComponent,
                                                                new ReferenceEditorWithBrowseButton(null, project, new Function<String, Document>() {
                                                                  public Document fun(final String s) {
                                                                    return JavaReferenceEditorUtil.createTypeDocument(s, project);
                                                                  }
                                                                }, ""), this);
  }


}
