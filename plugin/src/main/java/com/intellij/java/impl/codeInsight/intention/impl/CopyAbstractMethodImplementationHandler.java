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
package com.intellij.java.impl.codeInsight.intention.impl;

import com.intellij.java.language.impl.codeInsight.ChangeContextUtil;
import consulo.language.editor.CodeInsightBundle;
import com.intellij.java.impl.codeInsight.generation.GenerateMembersUtil;
import com.intellij.java.impl.codeInsight.generation.OverrideImplementUtil;
import com.intellij.java.impl.ide.util.MethodCellRenderer;
import com.intellij.java.language.psi.*;
import consulo.application.Result;
import consulo.language.editor.WriteCommandAction;
import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.fileEditor.FileEditorManager;
import consulo.navigation.OpenFileDescriptor;
import consulo.application.progress.ProgressManager;
import consulo.project.Project;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.awt.Messages;
import consulo.ide.impl.ui.impl.PopupChooserBuilder;
import consulo.util.lang.Comparing;
import consulo.language.psi.*;
import com.intellij.java.indexing.search.searches.ClassInheritorsSearch;
import consulo.language.psi.PsiUtilCore;
import com.intellij.java.language.psi.util.TypeConversionUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;

import javax.swing.*;
import java.util.*;

/**
 * @author yole
 */
public class CopyAbstractMethodImplementationHandler {
  private static final Logger LOG = Logger.getInstance(CopyAbstractMethodImplementationHandler.class);

  private final Project myProject;
  private final Editor myEditor;
  private final PsiMethod myMethod;
  private PsiClass mySourceClass;
  private final List<PsiClass> myTargetClasses = new ArrayList<PsiClass>();
  private final List<PsiEnumConstant> myTargetEnumConstants = new ArrayList<PsiEnumConstant>();
  private final List<PsiMethod> mySourceMethods = new ArrayList<PsiMethod>();

  public CopyAbstractMethodImplementationHandler(final Project project, final Editor editor, final PsiMethod method) {
    myProject = project;
    myEditor = editor;
    myMethod = method;
  }

  public void invoke() {
    ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
      @Override
      public void run() {
        searchExistingImplementations();
      }
    }, CodeInsightBundle.message("searching.for.implementations"), false, myProject);
    if (mySourceMethods.isEmpty()) {
      Messages.showErrorDialog(myProject, CodeInsightBundle.message("copy.abstract.method.no.existing.implementations.found"),
                               CodeInsightBundle.message("copy.abstract.method.title"));
      return;
    }
    if (mySourceMethods.size() == 1) {
      copyImplementation(mySourceMethods.get(0));
    }
    else {
      Collections.sort(mySourceMethods, new Comparator<PsiMethod>() {
        @Override
        public int compare(final PsiMethod o1, final PsiMethod o2) {
          PsiClass c1 = o1.getContainingClass();
          PsiClass c2 = o2.getContainingClass();
          return Comparing.compare(c1.getName(), c2.getName());
        }
      });
      final PsiMethod[] methodArray = mySourceMethods.toArray(new PsiMethod[mySourceMethods.size()]);
      final JList list = new JBList(methodArray);
      list.setCellRenderer(new MethodCellRenderer(true));
      final Runnable runnable = new Runnable() {
        @Override
        public void run() {
          int index = list.getSelectedIndex();
          if (index < 0) return;
          PsiMethod element = (PsiMethod)list.getSelectedValue();
          copyImplementation(element);
        }
      };
      new consulo.ide.impl.ui.impl.PopupChooserBuilder(list)
        .setTitle(CodeInsightBundle.message("copy.abstract.method.popup.title"))
        .setItemChoosenCallback(runnable)
        .createPopup()
        .showInBestPositionFor(myEditor);
    }
  }

  private void searchExistingImplementations() {
    mySourceClass = myMethod.getContainingClass();
    if (!mySourceClass.isValid()) return;
    for (PsiClass inheritor : ClassInheritorsSearch.search(mySourceClass, true)) {
      if (!inheritor.isInterface()) {
        PsiMethod method = ImplementAbstractMethodAction.findExistingImplementation(inheritor, myMethod);
        if (method != null && !method.hasModifierProperty(PsiModifier.ABSTRACT)) {
          mySourceMethods.add(method);
        }
        else if (method == null) {
          myTargetClasses.add(inheritor);
        }
      }
    }
    for (Iterator<PsiClass> targetClassIterator = myTargetClasses.iterator(); targetClassIterator.hasNext();) {
      PsiClass targetClass = targetClassIterator.next();
      if (containsAnySuperClass(targetClass)) {
        targetClassIterator.remove();
      }
    }
    if (mySourceClass.isEnum()) {
      for (PsiField field : mySourceClass.getFields()) {
        if (field instanceof PsiEnumConstant){
          final PsiEnumConstant enumConstant = (PsiEnumConstant)field;
          final PsiEnumConstantInitializer initializingClass = enumConstant.getInitializingClass();
          if (initializingClass == null) {
            myTargetEnumConstants.add(enumConstant);
          }
        }
      }
    }
  }

  private boolean containsAnySuperClass(final PsiClass targetClass) {
    PsiClass superClass = targetClass.getSuperClass();
    while(superClass != null) {
      if (myTargetClasses.contains(superClass)) return true;
      superClass = superClass.getSuperClass();
    }
    return false;
  }

  private void copyImplementation(final PsiMethod sourceMethod) {
    final List<PsiMethod> generatedMethods = new ArrayList<PsiMethod>();
    new WriteCommandAction(myProject, getTargetFiles()) {
      @Override
      protected void run(final Result result) throws Throwable {
        for (PsiEnumConstant enumConstant : myTargetEnumConstants) {
          PsiClass initializingClass = enumConstant.getOrCreateInitializingClass();
          myTargetClasses.add(initializingClass);
        }
        for(PsiClass psiClass: myTargetClasses) {
          final Collection<PsiMethod> methods = OverrideImplementUtil.overrideOrImplementMethod(psiClass, myMethod, true);
          final Iterator<PsiMethod> iterator = methods.iterator();
          if (!iterator.hasNext()) continue;
          PsiMethod overriddenMethod = iterator.next();
          final PsiCodeBlock body = overriddenMethod.getBody();
          final PsiCodeBlock sourceBody = sourceMethod.getBody();
          assert body != null && sourceBody != null;
          ChangeContextUtil.encodeContextInfo(sourceBody, true);
          final PsiElement newBody = body.replace(sourceBody.copy());
          ChangeContextUtil.decodeContextInfo(newBody, psiClass, null);

          PsiSubstitutor substitutor = TypeConversionUtil.getSuperClassSubstitutor(mySourceClass, psiClass, PsiSubstitutor.EMPTY);
          PsiElement anchor = OverrideImplementUtil.getDefaultAnchorToOverrideOrImplement(psiClass, sourceMethod, substitutor);
          try {
            if (anchor != null) {
              overriddenMethod = (PsiMethod) anchor.getParent().addBefore(overriddenMethod, anchor);
            }
            else {
              overriddenMethod = (PsiMethod) psiClass.addBefore(overriddenMethod, psiClass.getRBrace());
            }
            generatedMethods.add(overriddenMethod);
          }
          catch (IncorrectOperationException e) {
            LOG.error(e);
          }
        }
      }
    }.execute();
    if (generatedMethods.size() > 0) {
      PsiMethod target = generatedMethods.get(0);
      PsiFile psiFile = target.getContainingFile();
      FileEditorManager fileEditorManager = FileEditorManager.getInstance(psiFile.getProject());
      Editor editor = fileEditorManager.openTextEditor(new OpenFileDescriptor(psiFile.getProject(), psiFile.getVirtualFile()), false);
      if (editor != null) {
        GenerateMembersUtil.positionCaret(editor, target, true);
        editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
      }
    }
  }

  private PsiFile[] getTargetFiles() {
    Collection<PsiFile> fileList = new HashSet<PsiFile>();
    for(PsiClass psiClass: myTargetClasses) {
      fileList.add(psiClass.getContainingFile());
    }
    return PsiUtilCore.toPsiFileArray(fileList);
  }

}
