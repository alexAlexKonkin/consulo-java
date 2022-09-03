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
package com.intellij.java.impl.codeInsight.editorActions;

import consulo.application.CommonBundle;
import consulo.language.editor.CodeInsightBundle;
import com.intellij.java.impl.ide.util.FQNameCellRenderer;
import consulo.project.Project;
import consulo.ui.ex.awt.*;
import com.intellij.java.language.psi.PsiClass;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.DialogWrapper;

import javax.swing.*;
import java.awt.*;

import static consulo.ui.ex.awt.UIUtil.ComponentStyle.SMALL;
import static consulo.ui.ex.awt.UIUtil.FontColor.BRIGHTER;

class RestoreReferencesDialog extends DialogWrapper {
  private final Object[] myNamedElements;
  private JList myList;
  private Object[] mySelectedElements = PsiClass.EMPTY_ARRAY;
  private boolean myContainsClassesOnly = true;

  public RestoreReferencesDialog(final Project project, final Object[] elements) {
    super(project, true);
    myNamedElements = elements;
    for (Object element : elements) {
      if (!(element instanceof PsiClass)) {
        myContainsClassesOnly = false;
        break;
      }
    }
    if (myContainsClassesOnly) {
      setTitle(CodeInsightBundle.message("dialog.import.on.paste.title"));
    }
    else {
      setTitle(CodeInsightBundle.message("dialog.import.on.paste.title2"));
    }
    init();

    myList.setSelectionInterval(0, myNamedElements.length - 1);
  }

  @Override
  protected void doOKAction() {
    Object[] values = myList.getSelectedValues();
    mySelectedElements = new Object[values.length];
    System.arraycopy(values, 0, mySelectedElements, 0, values.length);
    super.doOKAction();
  }

  @Override
  protected JComponent createCenterPanel() {
    final JPanel panel = new JPanel(new BorderLayout(UIUtil.DEFAULT_HGAP, UIUtil.DEFAULT_VGAP));
    myList = new JBList(myNamedElements);
    myList.setCellRenderer(new FQNameCellRenderer());
    panel.add(ScrollPaneFactory.createScrollPane(myList), BorderLayout.CENTER);

    panel.add(new JBLabel(myContainsClassesOnly ?
                          CodeInsightBundle.message("dialog.paste.on.import.text") :
                          CodeInsightBundle.message("dialog.paste.on.import.text2"), SMALL, BRIGHTER), BorderLayout.NORTH);

    final JPanel buttonPanel = new JPanel(new VerticalFlowLayout());
    final JButton okButton = new JButton(CommonBundle.getOkButtonText());
    getRootPane().setDefaultButton(okButton);
    buttonPanel.add(okButton);
    final JButton cancelButton = new JButton(CommonBundle.getCancelButtonText());
    buttonPanel.add(cancelButton);

    panel.setPreferredSize(new Dimension(500, 400));

    return panel;
  }


  @Override
  protected String getDimensionServiceKey(){
    return "#com.intellij.codeInsight.editorActions.RestoreReferencesDialog";
  }

  public Object[] getSelectedElements(){
    return mySelectedElements;
  }
}
