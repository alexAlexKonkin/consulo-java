/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.java.language.impl.core;

import com.intellij.java.language.impl.psi.impl.file.impl.JavaFileManager;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiClassOwner;
import com.intellij.java.language.psi.PsiJavaModule;
import com.intellij.java.language.psi.PsiJavaPackage;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.logging.Logger;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author yole
 * TODO [VISTALL] create CorePackageManager
 */
public class CoreJavaFileManager implements JavaFileManager {
  private static final Logger LOG = Logger.getInstance(CoreJavaFileManager.class);

  private final List<VirtualFile> myClasspath = new ArrayList<VirtualFile>();

  private final PsiManager myPsiManager;

  public CoreJavaFileManager(PsiManager psiManager) {
    myPsiManager = psiManager;
  }

  private List<VirtualFile> roots() {
    return myClasspath;
  }

  @Override
  public PsiJavaPackage findPackage(@Nonnull String packageName) {
    throw new UnsupportedOperationException();
    /*final List<VirtualFile> files = findDirectoriesByPackageName(packageName);
    if (files.size() > 0) {
      return new PsiPackageImpl(myPsiManager, packageName);
    }    */
    //   return null;
  }

  private List<VirtualFile> findDirectoriesByPackageName(String packageName) {
    List<VirtualFile> result = new ArrayList<VirtualFile>();
    String dirName = packageName.replace(".", "/");
    for (VirtualFile root : roots()) {
      VirtualFile classDir = root.findFileByRelativePath(dirName);
      if (classDir != null) {
        result.add(classDir);
      }
    }
    return result;
  }

  /*@Nullable
  public PsiJavaPackage getPackage(PsiDirectory dir) {
    final VirtualFile file = dir.getVirtualFile();
    for (VirtualFile root : myClasspath) {
      if (VfsUtilCore.isAncestor(root, file, false)) {
        String relativePath = FileUtil.getRelativePath(root.getPath(), file.getPath(), '/');
        if (relativePath == null) continue;
        return new PsiPackageImpl(myPsiManager, relativePath.replace('/', '.'));
      }
    }
    return null;
  }   */

  @Override
  public PsiClass findClass(@Nonnull String qName, @Nonnull GlobalSearchScope scope) {
    for (VirtualFile root : roots()) {
      final PsiClass psiClass = findClassInClasspathRoot(qName, root, myPsiManager);
      if (psiClass != null) {
        return psiClass;
      }
    }
    return null;
  }

  @Nullable
  public static PsiClass findClassInClasspathRoot(String qName, VirtualFile root, PsiManager psiManager) {
    String pathRest = qName;
    VirtualFile cur = root;

    while (true) {
      int dot = pathRest.indexOf('.');
      if (dot < 0) {
        break;
      }

      String pathComponent = pathRest.substring(0, dot);
      VirtualFile child = cur.findChild(pathComponent);

      if (child == null) {
        break;
      }
      pathRest = pathRest.substring(dot + 1);
      cur = child;
    }

    String className = pathRest.replace('.', '$');
    int bucks = className.indexOf('$');

    String rootClassName;
    if (bucks < 0) {
      rootClassName = className;
    }
    else {
      rootClassName = className.substring(0, bucks);
      className = className.substring(bucks + 1);
    }

    VirtualFile vFile = cur.findChild(rootClassName + ".class");
    if (vFile == null) {
      vFile = cur.findChild(rootClassName + ".java");
    }

    if (vFile != null) {
      if (!vFile.isValid()) {
        LOG.error("Invalid child of valid parent: " + vFile.getPath() + "; " + root.isValid() + " path=" + root.getPath());
        return null;
      }

      final PsiFile file = psiManager.findFile(vFile);
      if (file instanceof PsiClassOwner) {
        final PsiClass[] classes = ((PsiClassOwner)file).getClasses();
        if (classes.length == 1) {
          PsiClass curClass = classes[0];

          if (bucks > 0) {
            int newComponentStart = 0;
            int lookupStart = 0;

            while (lookupStart <= className.length()) {
              int b = className.indexOf("$", lookupStart);
              b = b < 0 ? className.length() : b;

              String component = className.substring(newComponentStart, b);
              PsiClass inner = curClass.findInnerClassByName(component, false);

              lookupStart = b + 1;
              if (inner == null) {
                continue;
              }

              newComponentStart = lookupStart;
              curClass = inner;
            }

            if (lookupStart != newComponentStart) {
              return null;
            }
          }


          return curClass;
        }
      }
    }

    return null;
  }

  @Override
  public PsiClass[] findClasses(@Nonnull String qName, @Nonnull GlobalSearchScope scope) {
    List<PsiClass> result = new ArrayList<PsiClass>();
    for (VirtualFile file : roots()) {
      final PsiClass psiClass = findClassInClasspathRoot(qName, file, myPsiManager);
      if (psiClass != null) {
        result.add(psiClass);
      }
    }
    return result.toArray(new PsiClass[result.size()]);
  }

  @Override
  public Collection<String> getNonTrivialPackagePrefixes() {
    return Collections.emptyList();
  }

  @Nonnull
  @Override
  public Collection<PsiJavaModule> findModules(@Nonnull String moduleName, @Nonnull GlobalSearchScope scope) {
    return Collections.emptySet();
  }

  public void addToClasspath(VirtualFile root) {
    myClasspath.add(root);
  }
}
