/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.java.impl.codeInspection.nullable;

import com.intellij.java.analysis.impl.codeInspection.nullable.NullableStuffInspectionBase;
import com.intellij.java.analysis.impl.psi.impl.search.JavaNullMethodArgumentUtil;
import com.intellij.java.impl.codeInsight.NullableNotNullDialog;
import com.intellij.java.language.psi.PsiMethod;
import com.intellij.java.language.psi.PsiParameter;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.ReadAction;
import consulo.application.util.function.Processor;
import consulo.ide.impl.find.PsiElement2UsageTargetAdapter;
import consulo.language.editor.inspection.InspectionsBundle;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.LocalQuickFixOnPsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.Project;
import consulo.ui.ex.awt.JBCheckBox;
import consulo.usage.*;
import consulo.util.collection.ArrayUtil;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

@ExtensionImpl
public class NullableStuffInspection extends NullableStuffInspectionBase {
  @Override
  protected LocalQuickFix createNavigateToNullParameterUsagesFix(PsiParameter parameter) {
    return new NavigateToNullLiteralArguments(parameter);
  }

  @Override
  public boolean isEnabledByDefault() {
    return true;
  }

  @Override
  public JComponent createOptionsPanel() {
    return new OptionsPanel();
  }

  private class OptionsPanel extends JPanel {
    private JCheckBox myBreakingOverriding;
    private JCheckBox myNAMethodOverridesNN;
    private JPanel myPanel;
    private JCheckBox myReportNotAnnotatedGetter;
    private JButton myConfigureAnnotationsButton;
    private JCheckBox myIgnoreExternalSuperNotNull;
    private JCheckBox myNNParameterOverridesNA;
    private JBCheckBox myReportNullLiteralsPassedNotNullParameter;

    private OptionsPanel() {
      super(new BorderLayout());
      add(myPanel, BorderLayout.CENTER);

      ActionListener actionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          apply();
        }
      };
      myNAMethodOverridesNN.addActionListener(actionListener);
      myBreakingOverriding.addActionListener(actionListener);
      myNNParameterOverridesNA.addActionListener(actionListener);
      myReportNotAnnotatedGetter.addActionListener(actionListener);
      myIgnoreExternalSuperNotNull.addActionListener(actionListener);
      myReportNullLiteralsPassedNotNullParameter.addActionListener(actionListener);
      myConfigureAnnotationsButton.addActionListener(NullableNotNullDialog.createActionListener(this));
      reset();
    }

    private void reset() {
      myBreakingOverriding.setSelected(REPORT_NOTNULL_PARAMETER_OVERRIDES_NULLABLE);
      myNAMethodOverridesNN.setSelected(REPORT_NOT_ANNOTATED_METHOD_OVERRIDES_NOTNULL);
      myReportNotAnnotatedGetter.setSelected(REPORT_NOT_ANNOTATED_GETTER);
      myIgnoreExternalSuperNotNull.setSelected(IGNORE_EXTERNAL_SUPER_NOTNULL);
      myNNParameterOverridesNA.setSelected(REPORT_NOTNULL_PARAMETERS_OVERRIDES_NOT_ANNOTATED);
      myReportNullLiteralsPassedNotNullParameter.setSelected(REPORT_NULLS_PASSED_TO_NOT_NULL_PARAMETER);

      myIgnoreExternalSuperNotNull.setEnabled(myNAMethodOverridesNN.isSelected());
    }

    private void apply() {
      REPORT_NOT_ANNOTATED_METHOD_OVERRIDES_NOTNULL = myNAMethodOverridesNN.isSelected();
      REPORT_NOTNULL_PARAMETER_OVERRIDES_NULLABLE = myBreakingOverriding.isSelected();
      REPORT_NOT_ANNOTATED_GETTER = myReportNotAnnotatedGetter.isSelected();
      IGNORE_EXTERNAL_SUPER_NOTNULL = myIgnoreExternalSuperNotNull.isSelected();
      REPORT_NOTNULL_PARAMETERS_OVERRIDES_NOT_ANNOTATED = myNNParameterOverridesNA.isSelected();
      REPORT_NULLS_PASSED_TO_NOT_NULL_PARAMETER = myReportNullLiteralsPassedNotNullParameter.isSelected();
      REPORT_ANNOTATION_NOT_PROPAGATED_TO_OVERRIDERS = REPORT_NOT_ANNOTATED_METHOD_OVERRIDES_NOTNULL;

      myIgnoreExternalSuperNotNull.setEnabled(myNAMethodOverridesNN.isSelected());
    }
  }

  public static class NavigateToNullLiteralArguments extends LocalQuickFixOnPsiElement {
    public NavigateToNullLiteralArguments(@Nonnull PsiParameter element) {
      super(element);
    }

    @Nonnull
    @Override
    public String getText() {
      return getFamilyName();
    }

    @Nls
    @Nonnull
    @Override
    public String getFamilyName() {
      return InspectionsBundle.message("nullable.stuff.inspection.navigate.null.argument.usages.fix.family.name");
    }

    @Override
    public void invoke(@Nonnull Project project, @Nonnull PsiFile file, @Nonnull PsiElement startElement, @Nonnull PsiElement endElement) {
      PsiParameter p = (PsiParameter) startElement;
      final PsiMethod method = PsiTreeUtil.getParentOfType(p, PsiMethod.class);
      if (method == null) {
        return;
      }
      final int parameterIdx = ArrayUtil.find(method.getParameterList().getParameters(), p);
      if (parameterIdx < 0) {
        return;
      }

      UsageViewPresentation presentation = new UsageViewPresentation();
      String title = InspectionsBundle.message("nullable.stuff.inspection.navigate.null.argument.usages.view.name", p.getName());
      presentation.setUsagesString(title);
      presentation.setTabName(title);
      presentation.setTabText(title);
      UsageViewManager.getInstance(project).searchAndShowUsages(
          new UsageTarget[]{new PsiElement2UsageTargetAdapter(method.getParameterList().getParameters()[parameterIdx])},
          () -> new UsageSearcher() {
            @Override
            public void generate(@Nonnull final Processor<Usage> processor) {
              ReadAction.run(() -> JavaNullMethodArgumentUtil.searchNullArgument(method, parameterIdx, (arg) -> processor.process(new UsageInfo2UsageAdapter(new UsageInfo(arg)))));
            }
          }, false, false, presentation, null);
    }

    @Override
    public boolean startInWriteAction() {
      return false;
    }
  }
}