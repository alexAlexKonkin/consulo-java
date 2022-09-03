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
package com.intellij.java.execution.impl.filters;

import com.intellij.execution.filters.Filter;
import consulo.execution.ui.console.HyperlinkInfo;
import consulo.execution.ui.console.OpenFileHyperlinkInfo;
import consulo.dataContext.DataManager;
import consulo.language.psi.util.EditSourceUtil;
import consulo.language.editor.ui.PsiElementListCellRenderer;
import com.intellij.java.language.psi.JavaDirectoryService;
import com.intellij.java.language.psi.PsiJavaPackage;
import consulo.language.editor.PlatformDataKeys;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.ide.impl.ui.impl.PopupChooserBuilder;
import consulo.navigation.Navigatable;
import com.intellij.psi.*;
import com.intellij.java.language.psi.search.PsiShortNamesCache;
import consulo.ui.ex.awt.JBList;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YourkitFilter implements Filter {
  private static final Logger LOG = Logger.getInstance(YourkitFilter.class);

  private final Project myProject;


  private static final Pattern PATTERN = Pattern.compile("\\s*(\\w*)\\(\\):(-?\\d*), (\\w*\\.java)\\n");

  public YourkitFilter(@Nonnull final Project project) {
    myProject = project;
  }

  public Result applyFilter(final String line, final int entireLength) {
    if (!line.endsWith(".java\n")) {
      return null;
    }

    try {
      final Matcher matcher = PATTERN.matcher(line);
      if (matcher.matches()) {
        final String method = matcher.group(1);
        final int lineNumber = Integer.parseInt(matcher.group(2));
        final String fileName = matcher.group(3);

        final int textStartOffset = entireLength - line.length();

        final PsiShortNamesCache cache = PsiShortNamesCache.getInstance(myProject);
        final PsiFile[] psiFiles = cache.getFilesByName(fileName);

        if (psiFiles.length == 0) return null;


        final HyperlinkInfo info = psiFiles.length == 1 ?
            new OpenFileHyperlinkInfo(myProject, psiFiles[0].getVirtualFile(), lineNumber - 1) :
            new MyHyperlinkInfo(psiFiles);

        return new Result(textStartOffset + matcher.start(2), textStartOffset + matcher.end(3), info);
      }
    } catch (NumberFormatException e) {
      LOG.debug(e);
    }

    return null;
  }

  private static class MyHyperlinkInfo implements HyperlinkInfo {
    private final PsiFile[] myPsiFiles;

    public MyHyperlinkInfo(final PsiFile[] psiFiles) {
      myPsiFiles = psiFiles;
    }

    public void navigate(final Project project) {
      DefaultPsiElementListCellRenderer renderer = new DefaultPsiElementListCellRenderer();

      final JList list = new JBList(myPsiFiles);
      list.setCellRenderer(renderer);

      final PopupChooserBuilder builder = new PopupChooserBuilder(list);
      renderer.installSpeedSearch(builder);

      final Runnable runnable = new Runnable() {
        public void run() {
          int[] ids = list.getSelectedIndices();
          if (ids == null || ids.length == 0) return;
          Object[] selectedElements = list.getSelectedValues();
          for (Object element : selectedElements) {
            Navigatable descriptor = EditSourceUtil.getDescriptor((PsiElement) element);
            if (descriptor != null && descriptor.canNavigate()) {
              descriptor.navigate(true);
            }
          }
        }
      };

      final Editor editor = DataManager.getInstance().getDataContext().getData(PlatformDataKeys.EDITOR);

      builder.
          setTitle("Choose file").
          setItemChoosenCallback(runnable).
          createPopup().showInBestPositionFor(editor);
    }
  }


  private static class DefaultPsiElementListCellRenderer extends PsiElementListCellRenderer {
    public String getElementText(final PsiElement element) {
      return element.getContainingFile().getName();
    }

    @Nullable
    protected String getContainerText(final PsiElement element, final String name) {
      final PsiDirectory parent = ((PsiFile) element).getParent();
      if (parent == null) return null;
      final PsiJavaPackage psiPackage = JavaDirectoryService.getInstance().getPackage(parent);
      if (psiPackage == null) return null;
      return "(" + psiPackage.getQualifiedName() + ")";
    }

    protected int getIconFlags() {
      return 0;
    }
  }
}
