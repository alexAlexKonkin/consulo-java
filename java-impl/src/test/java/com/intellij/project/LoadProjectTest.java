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
package com.intellij.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.testFramework.LeakHunter;
import com.intellij.testFramework.PlatformTestCase;
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl;
import com.intellij.util.Processor;

public abstract class LoadProjectTest extends PlatformTestCase {
  @Override
  protected void setUpProject() throws Exception {
    String projectPath = "/model/model.ipr";
    myProject = ProjectManager.getInstance().loadAndOpenProject(projectPath);
  }

  @Override
  protected void tearDown() throws Exception {
    myProject = null;
    super.tearDown();
  }

  public void testLoadProject() throws Exception {
    VirtualFile src = ProjectRootManager.getInstance(getProject()).getContentSourceRoots()[0];

    VirtualFile a = src.findFileByRelativePath("/x/AClass.java");
    assertNotNull(a);
    PsiFile fileA = getPsiManager().findFile(a);
    assertNotNull(fileA);
    fileA.navigate(true);
    Editor editorA = FileEditorManager.getInstance(getProject()).openTextEditor(new OpenFileDescriptor(getProject(), a), true);
    assertNotNull(editorA);
    CodeInsightTestFixtureImpl.instantiateAndRun(fileA, editorA, new int[0], false);

    VirtualFile b = src.findFileByRelativePath("/x/BClass.java");
    assertNotNull(b);
    PsiFile fileB = getPsiManager().findFile(b);
    assertNotNull(fileB);
    fileB.navigate(true);
    Editor editorB = FileEditorManager.getInstance(getProject()).openTextEditor(new OpenFileDescriptor(getProject(), b), true);
    assertNotNull(editorB);
    CodeInsightTestFixtureImpl.instantiateAndRun(fileB, editorB, new int[0], false);

    FileEditor[] allEditors = FileEditorManager.getInstance(getProject()).getAllEditors();
    assertEquals(2, allEditors.length);

    FileEditorManager.getInstance(getProject()).closeFile(a);
    FileEditorManager.getInstance(getProject()).closeFile(b);
    ProjectManagerEx.getInstanceEx().closeAndDispose(getProject());

    LeakHunter.checkLeak(ApplicationManager.getApplication(), PsiFileImpl.class, new Processor<PsiFileImpl>() {
      @Override
      public boolean process(PsiFileImpl psiFile) {
        return  psiFile.getViewProvider().getVirtualFile().getFileSystem() instanceof LocalFileSystem;
      }
    });
  }

  @Override
  protected boolean isRunInWriteAction() {
    return false;
  }
}
