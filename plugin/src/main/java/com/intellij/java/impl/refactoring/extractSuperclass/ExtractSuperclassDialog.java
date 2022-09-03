/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.java.impl.refactoring.extractSuperclass;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;

import consulo.project.Project;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiMember;
import com.intellij.java.language.psi.PsiMethod;
import com.intellij.java.impl.refactoring.HelpID;
import com.intellij.java.impl.refactoring.JavaRefactoringSettings;
import consulo.language.editor.refactoring.RefactoringBundle;
import consulo.language.editor.refactoring.classMember.MemberInfoChange;
import consulo.language.editor.refactoring.classMember.MemberInfoModel;
import com.intellij.java.impl.refactoring.memberPullUp.PullUpProcessor;
import com.intellij.java.impl.refactoring.ui.MemberSelectionPanel;
import consulo.ide.impl.idea.refactoring.util.DocCommentPolicy;
import com.intellij.java.impl.refactoring.util.classMembers.InterfaceContainmentVerifier;
import com.intellij.java.impl.refactoring.util.classMembers.MemberInfo;
import com.intellij.java.impl.refactoring.util.classMembers.UsesAndInterfacesDependencyMemberInfoModel;
import consulo.util.collection.ArrayUtil;

class ExtractSuperclassDialog extends JavaExtractSuperBaseDialog {
  private final InterfaceContainmentVerifier myContainmentVerifier = new InterfaceContainmentVerifier() {
    public boolean checkedInterfacesContain(PsiMethod psiMethod) {
      return PullUpProcessor.checkedInterfacesContain(myMemberInfos, psiMethod);
    }
  };

  public interface Callback {
    boolean checkConflicts(ExtractSuperclassDialog dialog);
  }

  private final Callback myCallback;

  public ExtractSuperclassDialog(Project project, PsiClass sourceClass, List<MemberInfo> selectedMembers, Callback callback) {
    super(project, sourceClass, selectedMembers, ExtractSuperclassHandler.REFACTORING_NAME);
    myCallback = callback;
    init();
  }

  InterfaceContainmentVerifier getContainmentVerifier() {
    return myContainmentVerifier;
  }

  protected String getClassNameLabelText() {
    return isExtractSuperclass()
           ? RefactoringBundle.message("superclass.name")
           : RefactoringBundle.message("extractSuper.rename.original.class.to");
  }

  @Override
  protected String getPackageNameLabelText() {
    return isExtractSuperclass()
           ? RefactoringBundle.message("package.for.new.superclass")
           : RefactoringBundle.message("package.for.original.class");
  }

  protected String getEntityName() {
    return RefactoringBundle.message("ExtractSuperClass.superclass");
  }

  @Override
  protected String getTopLabelText() {
    return RefactoringBundle.message("extract.superclass.from");
  }

  protected JComponent createCenterPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    final MemberSelectionPanel memberSelectionPanel = new MemberSelectionPanel(RefactoringBundle.message("members.to.form.superclass"),
                                                                               myMemberInfos, RefactoringBundle.message("make.abstract"));
    panel.add(memberSelectionPanel, BorderLayout.CENTER);
    final MemberInfoModel<PsiMember, MemberInfo> memberInfoModel =
      new UsesAndInterfacesDependencyMemberInfoModel(mySourceClass, null, false, myContainmentVerifier) {
        public Boolean isFixedAbstract(MemberInfo member) {
          return Boolean.TRUE;
        }
      };
    memberInfoModel.memberInfoChanged(new MemberInfoChange<PsiMember, MemberInfo>(myMemberInfos));
    memberSelectionPanel.getTable().setMemberInfoModel(memberInfoModel);
    memberSelectionPanel.getTable().addMemberInfoChangeListener(memberInfoModel);

    panel.add(myDocCommentPanel, BorderLayout.EAST);

    return panel;
  }

  @Override
  protected String getDocCommentPanelName() {
    return RefactoringBundle.message("javadoc.for.abstracts");
  }

  @Override
  protected String getExtractedSuperNameNotSpecifiedMessage() {
    return RefactoringBundle.message("no.superclass.name.specified");
  }

  @Override
  protected boolean checkConflicts() {
    return myCallback.checkConflicts(this);
  }

  @Override
  protected int getDocCommentPolicySetting() {
    return JavaRefactoringSettings.getInstance().EXTRACT_SUPERCLASS_JAVADOC;
  }

  @Override
  protected void setDocCommentPolicySetting(int policy) {
    JavaRefactoringSettings.getInstance().EXTRACT_SUPERCLASS_JAVADOC = policy;
  }


  @Override
  protected ExtractSuperBaseProcessor createProcessor() {
    return new ExtractSuperClassProcessor(myProject, getTargetDirectory(), getExtractedSuperName(),
                                          mySourceClass, ArrayUtil.toObjectArray(getSelectedMemberInfos(), MemberInfo.class), false,
                                          new DocCommentPolicy(getDocCommentPolicy()));
  }

  @Override
  protected String getHelpId() {
    return HelpID.EXTRACT_SUPERCLASS;
  }
}
