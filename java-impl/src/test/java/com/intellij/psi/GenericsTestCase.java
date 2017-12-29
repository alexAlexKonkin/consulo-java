package com.intellij.psi;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PsiTestCase;
import com.intellij.testFramework.PsiTestUtil;

/**
 * @author dsl
 */
public abstract class GenericsTestCase extends PsiTestCase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    //LanguageLevelProjectExtension.getInstance(myJavaFacade.getProject()).setLanguageLevel(LanguageLevel.JDK_1_5);
  }

  protected void setupGenericSampleClasses() {
    final String commonPath = "/psi/types/src";
    final VirtualFile[] commonRoot = new VirtualFile[] { null };
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        commonRoot[0] = LocalFileSystem.getInstance().refreshAndFindFileByPath(commonPath);
      }
    });

    PsiTestUtil.addSourceRoot(myModule, commonRoot[0]);
  }
}
