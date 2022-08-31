/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.java.impl.openapi.roots.impl;

import com.intellij.java.impl.codeInsight.daemon.impl.quickfix.LocateLibraryDialog;
import com.intellij.java.impl.codeInsight.daemon.impl.quickfix.OrderEntryFix;
import com.intellij.java.impl.openapi.roots.JavaProjectModelModifier;
import com.intellij.java.language.LanguageLevel;
import com.intellij.java.language.impl.projectRoots.ex.JavaSdkUtil;
import com.intellij.java.language.projectRoots.roots.ExternalLibraryDescriptor;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.impl.OrderEntryUtil;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.roots.libraries.LibraryUtil;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.util.containers.ContainerUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.java.language.module.extension.JavaMutableModuleExtension;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public class IdeaProjectModelModifier extends JavaProjectModelModifier {
  private static final Logger LOG = Logger.getInstance(IdeaProjectModelModifier.class);
  private final Project myProject;

  public IdeaProjectModelModifier(Project project) {
    myProject = project;
  }

  @Override
  public AsyncResult<Void> addModuleDependency(@Nonnull Module from, @Nonnull Module to, @Nonnull DependencyScope scope) {
    ModuleRootModificationUtil.addDependency(from, to, scope, false);
    return AsyncResult.done(null);
  }

  @Override
  public AsyncResult<Void> addExternalLibraryDependency(@Nonnull final Collection<Module> modules, @Nonnull final ExternalLibraryDescriptor descriptor, @Nonnull final DependencyScope scope) {
    List<String> defaultRoots = descriptor.getLibraryClassesRoots();
    Module firstModule = ContainerUtil.getFirstItem(modules);
    LOG.assertTrue(firstModule != null);
    LocateLibraryDialog dialog = new LocateLibraryDialog(firstModule, defaultRoots, descriptor.getPresentableName());
    List<String> classesRoots = dialog.showAndGetResult();
    if (!classesRoots.isEmpty()) {
      String libraryName = classesRoots.size() > 1 ? descriptor.getPresentableName() : null;
      final List<String> urls = OrderEntryFix.refreshAndConvertToUrls(classesRoots);
      if (modules.size() == 1) {
        ModuleRootModificationUtil.addModuleLibrary(firstModule, libraryName, urls, Collections.emptyList(), scope);
      } else {
        WriteAction.run(() ->
        {
          Library library = LibraryUtil.createLibrary(LibraryTablesRegistrar.getInstance().getLibraryTable(myProject), descriptor.getPresentableName());
          Library.ModifiableModel model = library.getModifiableModel();
          for (String url : urls) {
            model.addRoot(url, OrderRootType.CLASSES);
          }
          model.commit();
          for (Module module : modules) {
            ModuleRootModificationUtil.addDependency(module, library, scope, false);
          }
        });
      }
    }
    return AsyncResult.done(null);
  }

  @Override
  public AsyncResult<Void> addLibraryDependency(@Nonnull Module from, @Nonnull Library library, @Nonnull DependencyScope scope) {
    OrderEntryUtil.addLibraryToRoots(from, library);
    return AsyncResult.done(null);
  }

  @Override
  @RequiredReadAction
  public AsyncResult<Void> changeLanguageLevel(@Nonnull Module module, @Nonnull LanguageLevel level) {
    if (JavaSdkUtil.isLanguageLevelAcceptable(myProject, module, level)) {
      final ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
      rootModel.getExtension(JavaMutableModuleExtension.class).getInheritableLanguageLevel().set(null, level);
      rootModel.commit();
    }
    return AsyncResult.done(null);
  }
}
