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

/*
 * Created by IntelliJ IDEA.
 * User: dsl
 * Date: 04.07.2002
 * Time: 13:54:39
 * To change template for new class use 
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.intellij.java.impl.refactoring.makeStatic;

import consulo.application.HelpManager;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import com.intellij.java.language.psi.PsiTypeParameterListOwner;
import com.intellij.java.impl.refactoring.HelpID;
import consulo.language.editor.refactoring.RefactoringBundle;
import com.intellij.java.analysis.impl.refactoring.util.VariableData;
import consulo.usage.UsageViewUtil;

import javax.swing.*;
import java.awt.*;

public class SimpleMakeStaticDialog extends AbstractMakeStaticDialog {
  JCheckBox myCbReplaceUsages;

  public SimpleMakeStaticDialog(Project project, PsiTypeParameterListOwner member) {
    super(project, member);
    String type = UsageViewUtil.getType(myMember);
    setTitle(RefactoringBundle.message("make.0.static", StringUtil.capitalize(type)));
    init();
  }

  protected boolean validateData() {
    return true;
  }

  public boolean isMakeClassParameter() {
    return false;
  }

  public String getClassParameterName() {
    return null;
  }

  public VariableData[] getVariableData() {
    return null;
  }

  public boolean isReplaceUsages() {
    return myCbReplaceUsages.isSelected();
  }

  protected void doHelpAction() {
    HelpManager.getInstance().invokeHelp(HelpID.MAKE_METHOD_STATIC_SIMPLE);
  }

  protected JComponent createNorthPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints gbConstraints = new GridBagConstraints();

    gbConstraints.insets = new Insets(4, 8, 4, 8);
    gbConstraints.weighty = 1;
    gbConstraints.weightx = 1;
    gbConstraints.gridy = 0;
    gbConstraints.gridwidth = GridBagConstraints.REMAINDER;
    gbConstraints.fill = GridBagConstraints.BOTH;
    gbConstraints.anchor = GridBagConstraints.WEST;
    panel.add(createDescriptionLabel(), gbConstraints);

    gbConstraints.gridy++;
    myCbReplaceUsages = new JCheckBox(RefactoringBundle.message("replace.instance.qualifiers.with.class.references"));
    panel.add(myCbReplaceUsages, gbConstraints);
    myCbReplaceUsages.setSelected(true);
    return panel;
  }

  protected JComponent createCenterPanel() {
    return null;
  }
}
