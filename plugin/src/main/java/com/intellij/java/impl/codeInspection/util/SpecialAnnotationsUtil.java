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

package com.intellij.java.impl.codeInspection.util;

import com.intellij.java.analysis.impl.codeInspection.util.SpecialAnnotationsUtilBase;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.util.ClassFilter;
import com.intellij.java.language.util.TreeClassChooser;
import com.intellij.java.language.util.TreeClassChooserFactory;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataManager;
import consulo.ide.impl.idea.util.IconUtil;
import consulo.language.editor.CommonDataKeys;
import consulo.language.editor.inspection.InspectionsBundle;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.SyntheticIntentionAction;
import consulo.language.psi.PsiFile;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.util.IncorrectOperationException;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.*;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.util.Comparator;
import java.util.List;

/**
 * @author Gregory.Shrago
 */
public class SpecialAnnotationsUtil {
  public static JPanel createSpecialAnnotationsListControl(final List<String> list, final String borderTitle) {
    return createSpecialAnnotationsListControl(list, borderTitle, false);
  }

  public static JPanel createSpecialAnnotationsListControl(final List<String> list,
                                                           final String borderTitle,
                                                           final boolean acceptPatterns) {
    final SortedListModel<String> listModel = new SortedListModel<String>(new Comparator<String>() {
      @Override
      public int compare(final String o1, final String o2) {
        return o1.compareTo(o2);
      }
    });
    final JList injectionList = new JBList(listModel);
    for (String s : list) {
      listModel.add(s);
    }
    injectionList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
    injectionList.getModel().addListDataListener(new ListDataListener() {
      @Override
      public void intervalAdded(ListDataEvent e) {
        listChanged();
      }

      private void listChanged() {
        list.clear();
        for (int i = 0; i < listModel.getSize(); i++) {
          list.add((String) listModel.getElementAt(i));
        }
      }

      @Override
      public void intervalRemoved(ListDataEvent e) {
        listChanged();
      }

      @Override
      public void contentsChanged(ListDataEvent e) {
        listChanged();
      }
    });

    ToolbarDecorator toolbarDecorator = ToolbarDecorator.createDecorator(injectionList)
        .setAddAction(new AnActionButtonRunnable() {
          @Override
          public void run(AnActionButton button) {
            Project project = DataManager.getInstance().getDataContext(injectionList).getData(CommonDataKeys.PROJECT);
            if (project == null) project = ProjectManager.getInstance().getDefaultProject();
            TreeClassChooser chooser = TreeClassChooserFactory.getInstance(project)
                .createWithInnerClassesScopeChooser(InspectionsBundle.message("special.annotations.list.annotation.class"),
                    GlobalSearchScope.allScope(project), new ClassFilter() {
                      @Override
                      public boolean isAccepted(PsiClass aClass) {
                        return aClass.isAnnotationType();
                      }
                    }, null);
            chooser.showDialog();
            final PsiClass selected = chooser.getSelected();
            if (selected != null) {
              listModel.add(selected.getQualifiedName());
            }
          }
        }).setAddActionName(InspectionsBundle.message("special.annotations.list.add.annotation.class")).disableUpDownActions();

    if (acceptPatterns) {
      toolbarDecorator
          .setAddIcon(IconUtil.getAddClassIcon())
          .addExtraAction(
              new AnActionButton(InspectionsBundle.message("special.annotations.list.annotation.pattern"), IconUtil.getAddPatternIcon()) {
                @Override
                public void actionPerformed(AnActionEvent e) {
                  String selectedPattern = Messages.showInputDialog(InspectionsBundle.message("special.annotations.list.annotation.pattern"),
                      InspectionsBundle.message("special.annotations.list.annotation.pattern"),
                      Messages.getQuestionIcon());
                  if (selectedPattern != null) {
                    listModel.add(selectedPattern);
                  }
                }
              }).setButtonComparator(InspectionsBundle.message("special.annotations.list.add.annotation.class"),
          InspectionsBundle.message("special.annotations.list.annotation.pattern"), "Remove");
    }

    JPanel panel = new JPanel(new BorderLayout());
    panel.add(SeparatorFactory.createSeparator(borderTitle, null), BorderLayout.NORTH);
    panel.add(toolbarDecorator.createPanel(), BorderLayout.CENTER);
    return panel;
  }

  public static IntentionAction createAddToSpecialAnnotationsListIntentionAction(final String text,
                                                                                 final String family,
                                                                                 final List<String> targetList,
                                                                                 final String qualifiedName) {
    return new SyntheticIntentionAction() {
      @Override
      @Nonnull
      public String getText() {
        return text;
      }

      public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
        return true;
      }

      @Override
      public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        SpecialAnnotationsUtilBase.doQuickFixInternal(project, targetList, qualifiedName);
      }

      @Override
      public boolean startInWriteAction() {
        return true;
      }
    };
  }
}