/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.java.impl.ig.fixes;

import com.siyeh.ig.InspectionGadgetsFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.project.Project;
import consulo.undoRedo.BasicUndoableAction;
import consulo.undoRedo.ProjectUndoManager;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.annotations.Nls;

import jakarta.annotation.Nonnull;
import java.util.Set;

/**
 * @author Bas Leijdekkers
 */
public class IgnoreClassFix extends InspectionGadgetsFix {
  final Set<String> myIgnoredClasses;
  final String myQualifiedName;
  private final String myFixName;

  public IgnoreClassFix(String qualifiedName, Set<String> ignoredClasses, String fixName) {
    myIgnoredClasses = ignoredClasses;
    myQualifiedName = qualifiedName;
    myFixName = fixName;
  }

  @Nls
  @Nonnull
  @Override
  public String getName() {
    return myFixName;
  }

  @Nls
  @Nonnull
  @Override
  public String getFamilyName() {
    return "Ignore for these types";
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  @Override
  protected void doFix(Project project, ProblemDescriptor descriptor) {
    if (!myIgnoredClasses.add(myQualifiedName)) {
      return;
    }
    //InspectionProjectProfileManager.getInstance(project).fireProfileChanged();
    final VirtualFile vFile = descriptor.getPsiElement().getContainingFile().getVirtualFile();
    ProjectUndoManager.getInstance(project).undoableActionPerformed(new BasicUndoableAction(vFile) {
      @Override
      public void undo() {
        myIgnoredClasses.remove(myQualifiedName);
        //ProjectInspectionProfileManager.getInstance(project).fireProfileChanged();
      }

      @Override
      public void redo() {
        myIgnoredClasses.add(myQualifiedName);
        //ProjectInspectionProfileManager.getInstance(project).fireProfileChanged();
      }

      @Override
      public boolean isGlobal() {
        return true;
      }
    });
  }
}
