/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.java.compiler.artifact.impl.artifacts;

import com.intellij.java.compiler.artifact.impl.ManifestFileUtil;
import consulo.application.ApplicationManager;
import consulo.application.CommonBundle;
import consulo.application.util.function.CommonProcessors;
import consulo.application.util.function.Processor;
import consulo.application.util.function.ThrowableComputable;
import consulo.compiler.artifact.ArtifactTemplate;
import consulo.compiler.artifact.ArtifactUtil;
import consulo.compiler.artifact.PlainArtifactType;
import consulo.compiler.artifact.element.*;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.library.Library;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.content.ProjectRootManager;
import consulo.module.content.layer.ModulesProvider;
import consulo.module.content.layer.OrderEnumerator;
import consulo.project.Project;
import consulo.ui.ex.awt.Messages;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFilePathUtil;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.util.*;

/**
 * @author nik
 */
public class JarFromModulesTemplate extends ArtifactTemplate {
  private static final Logger LOG = Logger.getInstance(JarFromModulesTemplate.class);

  private PackagingElementResolvingContext myContext;

  public JarFromModulesTemplate(PackagingElementResolvingContext context) {
    myContext = context;
  }

  @Override
  public NewArtifactConfiguration createArtifact() {
    JarArtifactFromModulesDialog dialog = new JarArtifactFromModulesDialog(myContext);
    dialog.show();
    if (!dialog.isOK()) {
      return null;
    }

    return doCreateArtifact(dialog.getSelectedModules(), dialog.getMainClassName(), dialog.getDirectoryForManifest(),
                            dialog.isExtractLibrariesToJar(), dialog.isIncludeTests());
  }

  @Nullable
  public NewArtifactConfiguration doCreateArtifact(final Module[] modules, final String mainClassName,
                                                   final String directoryForManifest,
                                                   final boolean extractLibrariesToJar,
                                                   final boolean includeTests) {
    VirtualFile manifestFile = null;
    final Project project = myContext.getProject();
    if (mainClassName != null && !mainClassName.isEmpty() || !extractLibrariesToJar) {
      final VirtualFile directory;
      try {
        directory = ApplicationManager.getApplication().runWriteAction((ThrowableComputable<VirtualFile, IOException>) () -> VirtualFileUtil.createDirectoryIfMissing(directoryForManifest));
      }
      catch (IOException e) {
        LOG.info(e);
        Messages.showErrorDialog(project, "Cannot create directory '" + directoryForManifest + "': " + e.getMessage(),
                                 CommonBundle.getErrorTitle());
        return null;
      }
      if (directory == null) return null;

      manifestFile = ManifestFileUtil.createManifestFile(directory, project);
      if (manifestFile == null) {
        return null;
      }
      ManifestFileUtil.updateManifest(manifestFile, mainClassName, null, true);
    }

    String name = modules.length == 1 ? modules[0].getName() : project.getName();

    final PackagingElementFactory factory = PackagingElementFactory.getInstance(myContext.getProject());
    final CompositePackagingElement<?> archive = factory.createZipArchive(ArtifactUtil.suggestArtifactFileName(name) + ".jar");

    OrderEnumerator orderEnumerator = ProjectRootManager.getInstance(project).orderEntries(Arrays.asList(modules));

    final Set<Library> libraries = new HashSet<Library>();
    if (!includeTests) {
      orderEnumerator = orderEnumerator.productionOnly();
    }
    final ModulesProvider modulesProvider = myContext.getModulesProvider();
    final OrderEnumerator enumerator = orderEnumerator.using(modulesProvider).withoutSdk().runtimeOnly().recursively();
    enumerator.forEachLibrary(new CommonProcessors.CollectProcessor<Library>(libraries));
    enumerator.forEachModule(new Processor<Module>() {
      @Override
      public boolean process(Module module) {
        if (ProductionModuleOutputElementType.getInstance().isSuitableModule(modulesProvider, module)) {
          archive.addOrFindChild(factory.createModuleOutput(module));
        }
        if (includeTests && TestModuleOutputElementType.getInstance().isSuitableModule(modulesProvider, module)) {
          archive.addOrFindChild(factory.createTestModuleOutput(module));
        }
        return true;
      }
    });

    final JarArtifactType jarArtifactType = JarArtifactType.getInstance();
    if (manifestFile != null && !manifestFile.equals(ManifestFileUtil.findManifestFile(archive, myContext, jarArtifactType))) {
      archive.addFirstChild(factory.createFileCopyWithParentDirectories(manifestFile.getPath(), ManifestFileUtil.MANIFEST_DIR_NAME));
    }

    final String artifactName = name + ":jar";
    if (extractLibrariesToJar) {
      addExtractedLibrariesToJar(archive, factory, libraries);
      return new NewArtifactConfiguration(archive, artifactName, jarArtifactType);
    }
    else {
      final ArtifactRootElement<?> root = factory.createArtifactRootElement();
      List<String> classpath = new ArrayList<String>();
      root.addOrFindChild(archive);
      addLibraries(libraries, root, archive, classpath);
      ManifestFileUtil.updateManifest(manifestFile, mainClassName, classpath, true);
      return new NewArtifactConfiguration(root, artifactName, PlainArtifactType.getInstance());
    }
  }

  private void addLibraries(Set<Library> libraries, ArtifactRootElement<?> root, CompositePackagingElement<?> archive,
                            List<String> classpath) {
    PackagingElementFactory factory = PackagingElementFactory.getInstance(myContext.getProject());
    for (Library library : libraries) {
      if (LibraryPackagingElement.getKindForLibrary(library).containsDirectoriesWithClasses()) {
        for (VirtualFile classesRoot : library.getFiles(BinariesOrderRootType.getInstance())) {
          if (classesRoot.isInLocalFileSystem()) {
            archive.addOrFindChild(factory.createDirectoryCopyWithParentDirectories(classesRoot.getPath(), "/"));
          }
          else {
            final PackagingElement<?> child = factory.createFileCopyWithParentDirectories(VirtualFilePathUtil.getLocalFile(classesRoot).getPath(), "/");
            root.addOrFindChild(child);
            classpath.addAll(ManifestFileUtil.getClasspathForElements(Collections.singletonList(child), myContext, PlainArtifactType.getInstance()));
          }
        }

      }
      else {
        final List<? extends PackagingElement<?>> children = factory.createLibraryElements(library);
        classpath.addAll(ManifestFileUtil.getClasspathForElements(children, myContext, PlainArtifactType.getInstance()));
        root.addOrFindChildren(children);
      }
    }
  }

  private static void addExtractedLibrariesToJar(CompositePackagingElement<?> archive, PackagingElementFactory factory, Set<Library> libraries) {
    for (Library library : libraries) {
      if (LibraryPackagingElement.getKindForLibrary(library).containsJarFiles()) {
        for (VirtualFile classesRoot : library.getFiles(BinariesOrderRootType.getInstance())) {
          if (classesRoot.isInLocalFileSystem()) {
            archive.addOrFindChild(factory.createDirectoryCopyWithParentDirectories(classesRoot.getPath(), "/"));
          }
          else {
            archive.addOrFindChild(factory.createExtractedDirectory(classesRoot));
          }
        }

      }
      else {
        archive.addOrFindChildren(factory.createLibraryElements(library));
      }
    }
  }

  @Override
  public String getPresentableName() {
    return "From modules with dependencies...";
  }
}
