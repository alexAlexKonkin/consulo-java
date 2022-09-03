/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package com.intellij.java.execution.impl;

import consulo.execution.ExecutionBundle;
import com.intellij.java.language.impl.codeInsight.StructureNodeRenderer;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiMethod;
import com.intellij.java.language.psi.PsiSubstitutor;
import com.intellij.java.language.psi.util.PsiFormatUtil;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.event.DoubleClickListener;
import consulo.ui.ex.awt.tree.TreeUIHelper;
import consulo.util.lang.function.Condition;
import com.intellij.ui.*;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.awt.DialogWrapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Comparator;

// Author: dyoma

public class MethodListDlg extends DialogWrapper {
  private final PsiClass myClass;
  private static final Comparator<PsiMethod> METHOD_NAME_COMPARATOR = (psiMethod, psiMethod1) -> psiMethod.getName().compareToIgnoreCase(psiMethod1.getName());
  private final SortedListModel<PsiMethod> myListModel = new SortedListModel<>(METHOD_NAME_COMPARATOR);
  private final JList myList = new JBList(myListModel);
  private final JPanel myWholePanel = new JPanel(new BorderLayout());

  public MethodListDlg(final PsiClass psiClass, final Condition<PsiMethod> filter, final JComponent parent) {
    super(parent, false);
    myClass = psiClass;
    createList(psiClass.getAllMethods(), filter);
    myWholePanel.add(ScrollPaneFactory.createScrollPane(myList));
    myList.setCellRenderer(new ColoredListCellRenderer() {
      protected void customizeCellRenderer(@Nonnull final JList list, final Object value, final int index, final boolean selected, final boolean hasFocus) {
        final PsiMethod psiMethod = (PsiMethod) value;
        append(PsiFormatUtil.formatMethod(psiMethod, PsiSubstitutor.EMPTY, PsiFormatUtil.SHOW_NAME, 0), StructureNodeRenderer.applyDeprecation(psiMethod, SimpleTextAttributes
            .REGULAR_ATTRIBUTES));
        final PsiClass containingClass = psiMethod.getContainingClass();
        if (!myClass.equals(containingClass)) {
          append(" (" + containingClass.getQualifiedName() + ")", StructureNodeRenderer.applyDeprecation(containingClass, SimpleTextAttributes.GRAY_ATTRIBUTES));
        }
      }
    });
    myList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    new DoubleClickListener() {
      @Override
      protected boolean onDoubleClick(MouseEvent e) {
        MethodListDlg.this.close(OK_EXIT_CODE);
        return true;
      }
    }.installOn(myList);

    ScrollingUtil.ensureSelectionExists(myList);
    TreeUIHelper.getInstance().installListSpeedSearch(myList);
    setTitle(ExecutionBundle.message("choose.test.method.dialog.title"));
    init();
  }

  private void createList(final PsiMethod[] allMethods, final Condition<PsiMethod> filter) {
    for (int i = 0; i < allMethods.length; i++) {
      final PsiMethod method = allMethods[i];
      if (filter.value(method)) {
        myListModel.add(method);
      }
    }
  }

  protected JComponent createCenterPanel() {
    return myWholePanel;
  }

  @Nullable
  @Override
  public JComponent getPreferredFocusedComponent() {
    return myList;
  }

  public PsiMethod getSelected() {
    return (PsiMethod) myList.getSelectedValue();
  }
}
