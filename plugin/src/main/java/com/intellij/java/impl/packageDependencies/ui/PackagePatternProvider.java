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

/*
 * User: anna
 * Date: 16-Jan-2008
 */
package com.intellij.java.impl.packageDependencies.ui;

import consulo.ide.IdeBundle;
import com.intellij.java.impl.ide.util.scopeChooser.GroupByScopeTypeAction;
import com.intellij.java.impl.psi.search.scope.packageSet.PatternPackageSet;
import com.intellij.java.language.psi.PsiClassOwner;
import com.intellij.java.language.psi.PsiNameHelper;
import consulo.ui.ex.action.AnAction;
import consulo.project.Project;
import consulo.module.content.ProjectRootManager;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import com.intellij.packageDependencies.ui.*;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.content.scope.PackageSet;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

public class PackagePatternProvider extends PatternDialectProvider {
  @NonNls
  public static final String PACKAGES = "package";
  private static final Logger LOG = Logger.getInstance(PackagePatternProvider.class);

  @Nullable
  private static GeneralGroupNode getGroupParent(PackageDependenciesNode node) {
    if (node instanceof GeneralGroupNode) return (GeneralGroupNode) node;
    if (node == null || node instanceof RootNode) return null;
    return getGroupParent((PackageDependenciesNode) node.getParent());
  }

  public PackageSet createPackageSet(final PackageDependenciesNode node, final boolean recursively) {
    GeneralGroupNode groupParent = getGroupParent(node);
    String scope1 = PatternPackageSet.SCOPE_ANY;
    if (groupParent != null) {
      String name = groupParent.toString();
      if (TreeModelBuilder.PRODUCTION_NAME.equals(name)) {
        scope1 = PatternPackageSet.SCOPE_SOURCE;
      } else if (TreeModelBuilder.TEST_NAME.equals(name)) {
        scope1 = PatternPackageSet.SCOPE_TEST;
      } else if (TreeModelBuilder.LIBRARY_NAME.equals(name)) {
        scope1 = PatternPackageSet.SCOPE_LIBRARY;
      }
    }
    final String scope = scope1;
    if (node instanceof ModuleGroupNode) {
      if (!recursively) return null;
      @NonNls final String modulePattern = "group:" + ((ModuleGroupNode) node).getModuleGroup().toString();
      return new PatternPackageSet("*..*", scope, modulePattern);
    } else if (node instanceof ModuleNode) {
      if (!recursively) return null;
      final String modulePattern = ((ModuleNode) node).getModuleName();
      return new PatternPackageSet("*..*", scope, modulePattern);
    } else if (node instanceof PackageNode) {
      String pattern = ((PackageNode) node).getPackageQName();
      if (pattern != null) {
        pattern += recursively ? "..*" : ".*";
      } else {
        pattern = recursively ? "*..*" : "*";
      }

      return new PatternPackageSet(pattern, scope, getModulePattern(node));
    } else if (node instanceof FileNode) {
      if (recursively) return null;
      FileNode fNode = (FileNode) node;
      final PsiElement element = fNode.getPsiElement();
      String qName = null;
      if (element instanceof PsiClassOwner) {
        final PsiClassOwner javaFile = (PsiClassOwner) element;
        final VirtualFile virtualFile = javaFile.getVirtualFile();
        LOG.assertTrue(virtualFile != null);
        final String packageName =
            ProjectRootManager.getInstance(element.getProject()).getFileIndex().getPackageNameByDirectory(virtualFile.getParent());
        final String name = virtualFile.getNameWithoutExtension();
        if (!PsiNameHelper.getInstance(element.getProject()).isIdentifier(name)) return null;
        qName = StringUtil.getQualifiedName(packageName, name);
      }
      if (qName != null) {
        return new PatternPackageSet(qName, scope, getModulePattern(node));
      }
    } else if (node instanceof GeneralGroupNode) {
      return new PatternPackageSet("*..*", scope, null);
    }

    return null;
  }

  public Image getIcon() {
    return PlatformIconGroup.nodesCopyOfFolder();
  }

  public TreeModel createTreeModel(final Project project, final Marker marker) {
    return TreeModelBuilder.createTreeModel(project, false, marker);
  }

  public TreeModel createTreeModel(final Project project, final Set<PsiFile> deps, final Marker marker,
                                   final DependenciesPanel.DependencyPanelSettings settings) {
    return TreeModelBuilder.createTreeModel(project, false, deps, marker, settings);
  }

  public String getDisplayName() {
    return IdeBundle.message("title.packages");
  }

  @Nonnull
  public String getShortName() {
    return PACKAGES;
  }

  public AnAction[] createActions(Project project, final Runnable update) {
    return new AnAction[]{new GroupByScopeTypeAction(update)};
  }
}
