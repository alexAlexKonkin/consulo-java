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

package com.intellij.java.impl.lang.java;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.intellij.java.language.JavaLanguage;
import com.intellij.java.language.psi.PsiImportList;
import com.intellij.java.language.psi.PsiImportStatement;
import com.intellij.java.language.psi.PsiImportStaticStatement;
import com.intellij.java.language.psi.PsiJavaFile;
import com.intellij.java.language.psi.codeStyle.JavaCodeStyleManager;
import consulo.annotation.component.ExtensionImpl;
import consulo.document.Document;
import consulo.language.Language;
import consulo.language.editor.refactoring.ImportOptimizer;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.lang.EmptyRunnable;

import jakarta.annotation.Nonnull;

/**
 * @author max
 */
@ExtensionImpl
public class JavaImportOptimizer implements ImportOptimizer {
  private static final Logger LOG = Logger.getInstance(JavaImportOptimizer.class);

  @Override
  @Nonnull
  public Runnable processFile(final PsiFile file) {
    if (!(file instanceof PsiJavaFile)) {
      return EmptyRunnable.getInstance();
    }
    Project project = file.getProject();
    final PsiImportList newImportList = JavaCodeStyleManager.getInstance(project).prepareOptimizeImportsResult((PsiJavaFile) file);
    if (newImportList == null) {
      return EmptyRunnable.getInstance();
    }

    return new CollectingInfoRunnable() {
      private int myImportsAdded;
      private int myImportsRemoved;

      @Override
      public void run() {
        try {
          final PsiDocumentManager manager = PsiDocumentManager.getInstance(file.getProject());
          final Document document = manager.getDocument(file);
          if (document != null) {
            manager.commitDocument(document);
          }
          final PsiImportList oldImportList = ((PsiJavaFile) file).getImportList();
          assert oldImportList != null;
          final Multiset<PsiElement> oldImports = HashMultiset.create();
          for (PsiImportStatement statement : oldImportList.getImportStatements()) {
            oldImports.add(statement.resolve());
          }

          final Multiset<PsiElement> oldStaticImports = HashMultiset.create();
          for (PsiImportStaticStatement statement : oldImportList.getImportStaticStatements()) {
            oldStaticImports.add(statement.resolve());
          }

          oldImportList.replace(newImportList);
          for (PsiImportStatement statement : newImportList.getImportStatements()) {
            if (!oldImports.remove(statement.resolve())) {
              myImportsAdded++;
            }
          }
          myImportsRemoved += oldImports.size();

          for (PsiImportStaticStatement statement : newImportList.getImportStaticStatements()) {
            if (!oldStaticImports.remove(statement.resolve())) {
              myImportsAdded++;
            }
          }
          myImportsRemoved += oldStaticImports.size();
        } catch (IncorrectOperationException e) {
          LOG.error(e);
        }
      }

      @Override
      public String getUserNotificationInfo() {
        if (myImportsRemoved == 0) {
          return "rearranged imports";
        }
        final StringBuilder notification = new StringBuilder("removed ").append(myImportsRemoved).append(" import");
        if (myImportsRemoved > 1) {
          notification.append('s');
        }
        if (myImportsAdded > 0) {
          notification.append(", added ").append(myImportsAdded).append(" import");
          if (myImportsAdded > 1) {
            notification.append('s');
          }
        }
        return notification.toString();
      }
    };
  }

  @Override
  public boolean supports(PsiFile file) {
    return file instanceof PsiJavaFile;
  }

  @Nonnull
  @Override
  public Language getLanguage() {
    return JavaLanguage.INSTANCE;
  }
}
