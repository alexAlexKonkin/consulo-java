/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package com.intellij.java.impl.openapi.roots.ui.configuration.libraryEditor;

import com.intellij.java.language.projectRoots.roots.NativeLibraryOrderRootType;
import consulo.annotation.component.ExtensionImpl;
import consulo.content.bundle.Sdk;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.ide.ui.OrderRootTypeUIFactory;
import consulo.ide.ui.SdkPathEditor;
import consulo.java.impl.JavaBundle;
import consulo.java.language.impl.JavaIcons;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;

@ExtensionImpl
public class NativeLibraryOrderRootTypeUIFactory implements OrderRootTypeUIFactory {
  @Nonnull
  @Override
  public Image getIcon() {
    return JavaIcons.Nodes.NativeLibrariesFolder;
  }

  @Nonnull
  @Override
  public String getNodeText() {
    return JavaBundle.message("project.roots.native.library.node.text");
  }

  @Nonnull
  @Override
  public String getOrderRootTypeId() {
    return "javaNative";
  }

  @Nonnull
  @Override
  public SdkPathEditor createPathEditor(Sdk sdk) {
    return new SdkPathEditor(getNodeText(),
                             NativeLibraryOrderRootType.getInstance(),
                             new FileChooserDescriptor(true, false, true, false, true, false),
                             sdk);
  }
}
