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
package com.intellij.core;

import com.intellij.mock.MockFileIndexFacade;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.roots.impl.DirectoryIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleSettingsFacade;
import com.intellij.psi.impl.JavaPsiFacadeImpl;
import com.intellij.psi.impl.JavaPsiImplementationHelper;
import com.intellij.psi.impl.PsiElementFactoryImpl;
import com.intellij.psi.impl.file.impl.JavaFileManager;
import com.intellij.psi.impl.source.resolve.JavaResolveCache;
import com.intellij.psi.impl.source.resolve.PsiResolveHelperImpl;
import org.consulo.psi.PsiPackageManager;
import org.consulo.psi.impl.PsiPackageManagerImpl;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * TODO [VISTALL]
 */
public class JavaCoreProjectEnvironment  extends CoreProjectEnvironment {
  private final JavaFileManager myFileManager;
  //private final PackageIndex myPackageIndex;

  public JavaCoreProjectEnvironment(Disposable parentDisposable, CoreApplicationEnvironment applicationEnvironment) {
    super(parentDisposable, applicationEnvironment);

    myProject.registerService(PsiElementFactory.class, new PsiElementFactoryImpl(myPsiManager));
    myProject.registerService(JavaPsiImplementationHelper.class, createJavaPsiImplementationHelper());
    myProject.registerService(PsiResolveHelper.class, new PsiResolveHelperImpl(myPsiManager));
    myProject.registerService(JavaResolveCache.class, new JavaResolveCache(myMessageBus));
    myProject.registerService(JavaCodeStyleSettingsFacade.class, new CoreJavaCodeStyleSettingsFacade());
    myProject.registerService(JavaCodeStyleManager.class, new CoreJavaCodeStyleManager());
    registerProjectExtensionPoint(PsiElementFinder.EP_NAME, PsiElementFinder.class);

 /*   myPackageIndex = createCorePackageIndex();
    myProject.registerService(PackageIndex.class, myPackageIndex);     */

    myFileManager = createCoreFileManager();
    myProject.registerService(JavaFileManager.class, myFileManager);

    PsiPackageManager manager = new PsiPackageManagerImpl(getProject(), PsiManager.getInstance(getProject()), DirectoryIndex.getInstance(getProject()), myMessageBus);

    myProject.registerService(PsiPackageManager.class, manager);

    JavaPsiFacadeImpl javaPsiFacade = new JavaPsiFacadeImpl(myProject, manager, myFileManager, myMessageBus);
    registerProjectComponent(JavaPsiFacade.class, javaPsiFacade);
    myProject.registerService(JavaPsiFacade.class, javaPsiFacade);
  }

  protected CoreJavaPsiImplementationHelper createJavaPsiImplementationHelper() {
    return new CoreJavaPsiImplementationHelper();
  }

  protected JavaFileManager createCoreFileManager() {
    return new CoreJavaFileManager(myPsiManager);
  }

  public void addJarToClassPath (File path) {
    assert path.isFile();

    final VirtualFile root = getEnvironment().getJarFileSystem().findFileByPath(path + "!/");
    if (root == null) {
      throw new IllegalArgumentException("trying to add non-existing file to classpath: " + path);
    }

    addSourcesToClasspath(root);
  }

  public void addSourcesToClasspath(@NotNull VirtualFile root) {
    assert root.isDirectory();
    ((CoreJavaFileManager)myFileManager).addToClasspath(root);
 //  ((CorePackageIndex)myPackageIndex).addToClasspath(root);
    ((MockFileIndexFacade)myFileIndexFacade).addLibraryRoot(root);
  }
}
