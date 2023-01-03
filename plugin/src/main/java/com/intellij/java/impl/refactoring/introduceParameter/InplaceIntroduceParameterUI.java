/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.java.impl.refactoring.introduceParameter;

import consulo.project.Project;
import consulo.ui.ex.awt.LabeledComponent;
import com.intellij.java.language.psi.PsiExpression;
import com.intellij.java.language.psi.PsiLocalVariable;
import com.intellij.java.language.psi.PsiMethod;
import com.intellij.java.language.psi.PsiParameter;
import consulo.language.codeStyle.CodeStyleSettingsManager;
import com.intellij.java.language.psi.util.PsiUtil;
import com.intellij.java.impl.refactoring.IntroduceParameterRefactoring;
import com.intellij.java.impl.refactoring.JavaRefactoringSettings;
import consulo.language.editor.refactoring.RefactoringBundle;
import consulo.ide.impl.idea.refactoring.introduce.inplace.KeyboardComboSwitcher;
import com.intellij.java.impl.refactoring.ui.TypeSelectorManager;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.ui.ex.awt.ListCellRendererWrapper;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.collection.primitive.ints.IntList;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * User: anna
 */
public abstract class InplaceIntroduceParameterUI extends IntroduceParameterSettingsUI {
  private JComboBox myReplaceFieldsCb;
  private boolean myHasWriteAccess = false;
  private final Project myProject;
  private final TypeSelectorManager myTypeSelectorManager;
  private final PsiExpression[] myOccurrences;

  public InplaceIntroduceParameterUI(Project project,
                                     PsiLocalVariable onLocalVariable,
                                     PsiExpression onExpression,
                                     PsiMethod methodToReplaceIn,
                                     IntList parametersToRemove,
                                     TypeSelectorManager typeSelectorManager,
                                     PsiExpression[] occurrences) {
    super(onLocalVariable, onExpression, methodToReplaceIn, parametersToRemove);
    myProject = project;
    myTypeSelectorManager = typeSelectorManager;
    myOccurrences = occurrences;

    for (PsiExpression occurrence : myOccurrences) {
      if (PsiUtil.isAccessedForWriting(occurrence)) {
        myHasWriteAccess = true;
        break;
      }
    }
  }

  protected abstract PsiParameter getParameter();

  @Override
  protected JPanel createReplaceFieldsWithGettersPanel() {
    final LabeledComponent<JComboBox> component = new LabeledComponent<JComboBox>();
    myReplaceFieldsCb = new JComboBox(new Integer[]{IntroduceParameterRefactoring.REPLACE_FIELDS_WITH_GETTERS_ALL,
      IntroduceParameterRefactoring.REPLACE_FIELDS_WITH_GETTERS_INACCESSIBLE,
      IntroduceParameterRefactoring.REPLACE_FIELDS_WITH_GETTERS_NONE});
    myReplaceFieldsCb.setRenderer(new ListCellRendererWrapper<Integer>() {
      @Override
      public void customize(JList list, Integer value, int index, boolean selected, boolean hasFocus) {
        switch (value) {
          case IntroduceParameterRefactoring.REPLACE_FIELDS_WITH_GETTERS_NONE:
            setText(UIUtil.removeMnemonic(RefactoringBundle.message("do.not.replace")));
            break;
          case IntroduceParameterRefactoring.REPLACE_FIELDS_WITH_GETTERS_INACCESSIBLE:
            setText(UIUtil.removeMnemonic(RefactoringBundle.message("replace.fields.inaccessible.in.usage.context")));
            break;
          default:
            setText(UIUtil.removeMnemonic(RefactoringBundle.message("replace.all.fields")));
        }
      }
    });
    myReplaceFieldsCb.setSelectedItem(JavaRefactoringSettings.getInstance().INTRODUCE_PARAMETER_REPLACE_FIELDS_WITH_GETTERS);
    KeyboardComboSwitcher.setupActions(myReplaceFieldsCb, myProject);
    component.setComponent(myReplaceFieldsCb);
    component.setText(RefactoringBundle.message("replace.fields.used.in.expressions.with.their.getters"));
    component.getLabel().setDisplayedMnemonic('u');
    component.setLabelLocation(BorderLayout.NORTH);
    component.setBorder(IdeBorderFactory.createEmptyBorder(3, 3, 2, 2));
    return component;
  }

  @Override
  protected int getReplaceFieldsWithGetters() {
    return myReplaceFieldsCb != null
           ? (Integer)myReplaceFieldsCb.getSelectedItem()
           : IntroduceParameterRefactoring.REPLACE_FIELDS_WITH_GETTERS_INACCESSIBLE;
  }

  @Override
  protected TypeSelectorManager getTypeSelectionManager() {
    return myTypeSelectorManager;
  }

  public boolean isGenerateFinal() {
    return hasFinalModifier();
  }

  public void appendOccurrencesDelegate(JPanel myWholePanel) {
    final GridBagConstraints gc =
      new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    if (myOccurrences.length > 1 && !myIsInvokedOnDeclaration) {
      gc.gridy++;
      createOccurrencesCb(gc, myWholePanel, myOccurrences.length);
      myCbReplaceAllOccurences.addItemListener(
        new ItemListener() {
          public void itemStateChanged(ItemEvent e) {
            updateControls(new JCheckBox[0]);
          }
        }
      );
    }
    gc.gridy++;
    gc.insets.left = 0;
    createDelegateCb(gc, myWholePanel);
  }

  public boolean hasFinalModifier() {
    if (myHasWriteAccess) return false;
    final Boolean createFinals = JavaRefactoringSettings.getInstance().INTRODUCE_PARAMETER_CREATE_FINALS;
    return createFinals == null ? CodeStyleSettingsManager.getSettings(myProject).GENERATE_FINAL_PARAMETERS : createFinals.booleanValue();
  }
}