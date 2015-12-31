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
package com.intellij.ide.util.projectWizard;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.jetbrains.annotations.NotNull;
import com.intellij.CommonBundle;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkTable;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.roots.ui.configuration.SdkComboBox;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Condition;

/**
 * @author Dmitry Avdeev
 *         Date: 10/26/12
 */
public class SdkSettingsStep extends ModuleWizardStep {
  protected final SdkComboBox mySdkComboBox;
  protected final WizardContext myWizardContext;
  protected final ProjectSdksModel myModel;
  private final ModuleBuilder myModuleBuilder;

  public SdkSettingsStep(SettingsStep settingsStep, ModuleBuilder moduleBuilder, @NotNull Condition<SdkTypeId> sdkFilter) {
    myModuleBuilder = moduleBuilder;

    myWizardContext = settingsStep.getContext();
    myModel = new ProjectSdksModel();
    Project project = myWizardContext.getProject();
    myModel.reset(project);

    mySdkComboBox = new SdkComboBox(myModel, sdkFilter, false);

    final PropertiesComponent component = project == null ? PropertiesComponent.getInstance() : PropertiesComponent.getInstance(project);
    final String selectedJdkProperty = "jdk.selected";
    mySdkComboBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        Sdk jdk = mySdkComboBox.getSelectedSdk();
        if (jdk != null) {
          component.setValue(selectedJdkProperty, jdk.getName());
        }
      }
    });

    if (project != null) {
     /* Sdk sdk = ProjectRootManager.getInstance(project).getProjectSdk();
      if (sdk != null && moduleBuilder.isSuitableSdkType(sdk.getSdkType())) {
        // use project SDK
        return;
      }*/
    }
    else {
      // set default project SDK
     /* Project defaultProject = ProjectManager.getInstance().getDefaultProject();
      Sdk sdk = ProjectRootManager.getInstance(defaultProject).getProjectSdk();
      if (sdk != null && sdkFilter.value(sdk.getSdkType())) {
        mySdkComboBox.setSelectedSdk(sdk);
      }         */
    }

    String value = component.getValue(selectedJdkProperty);
    if (value != null) {
      Sdk jdk = SdkTable.getInstance().findSdk(value);
      if (jdk != null) {
        mySdkComboBox.setSelectedSdk(jdk);
      }
    }

    JButton button = new JButton("Ne\u001Bw...");
    mySdkComboBox.setSetupButton(button, project, myModel, new SdkComboBox.NullSdkComboBoxItem(), null);
    JPanel jdkPanel = new JPanel(new BorderLayout(4, 0));
    jdkPanel.add(mySdkComboBox);
    jdkPanel.add(button, BorderLayout.EAST);
    settingsStep.addSettingsField((project == null ? "Project" : "Module") + " \u001BSDK:", jdkPanel);

  }

  @Override
  public JComponent getComponent() {
    return null;
  }

  @Override
  public void updateDataModel() {
    Project project = myWizardContext.getProject();
    if (project == null) {
      Sdk jdk = mySdkComboBox.getSelectedSdk();
     // myWizardContext.setProjectJdk(jdk);
    }
    else {
    /*  Sdk sdk = ProjectRootManager.getInstance(project).getProjectSdk();
      if (sdk == null || !myModuleBuilder.isSuitableSdkType(sdk.getSdkType())) {
        myModuleBuilder.setModuleJdk(mySdkComboBox.getSelectedSdk());
      } */// else, inherit project jdk
    }
  }

  @Override
  public boolean validate() throws ConfigurationException {
    if (mySdkComboBox.getSelectedSdk() == null) {
      if (Messages.showDialog(getNoSdkMessage(), IdeBundle.message("title.no.jdk.specified"),
                              new String[]{CommonBundle.getYesButtonText(), CommonBundle.getNoButtonText()}, 1,
                              Messages.getWarningIcon()) != Messages.YES) {
        return false;
      }
    }
    try {
      myModel.apply(null, true);
    }
    catch (ConfigurationException e) {
      //IDEA-98382 We should allow Next step if user has wrong SDK
      if (Messages.showDialog(e.getMessage() + "/nDo you want to proceed?", e.getTitle(),
                              new String[]{CommonBundle.getYesButtonText(), CommonBundle.getNoButtonText()}, 1,
                              Messages.getWarningIcon()) != Messages.YES) {
        return false;
      }
    }
    return true;
  }

  protected String getNoSdkMessage() {
    return IdeBundle.message("prompt.confirm.project.no.jdk");
  }
}
