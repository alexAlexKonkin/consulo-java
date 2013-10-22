/*
 * Copyright 2013 Consulo.org
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
package org.consulo.java.platform.module.extension;

import com.intellij.openapi.module.Module;
import org.consulo.java.platform.JavaPlatformIcons;
import org.consulo.module.extension.ModuleExtensionProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 12:39/19.05.13
 */
public class JavaModuleExtensionProvider implements
                                         ModuleExtensionProvider<JavaModuleExtensionImpl, JavaMutableModuleExtensionImpl> {
  @Nullable
  @Override
  public Icon getIcon() {
    return JavaPlatformIcons.Java;
  }

  @NotNull
  @Override
  public String getName() {
    return "Java";
  }

  @NotNull
  @Override
  public Class<JavaModuleExtensionImpl> getImmutableClass() {
    return JavaModuleExtensionImpl.class;
  }

  @NotNull
  @Override
  public JavaModuleExtensionImpl createImmutable(@NotNull String id, @NotNull Module module) {
    return new JavaModuleExtensionImpl(id, module);
  }

  @NotNull
  @Override
  public JavaMutableModuleExtensionImpl createMutable(@NotNull String id, @NotNull Module module, @NotNull JavaModuleExtensionImpl javaModuleExtension) {
    return new JavaMutableModuleExtensionImpl(id, module, javaModuleExtension);
  }
}