/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.spi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.intellij.icons.AllIcons;
import com.intellij.lang.spi.SPILanguage;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.ui.image.Image;

/**
 * User: anna
 */
public class SPIFileType extends LanguageFileType {
  public static final SPIFileType INSTANCE = new SPIFileType();

  private SPIFileType() {
    super(SPILanguage.INSTANCE);
  }

  @Nonnull
  @Override
  public String getName() {
    return "JAVA-SPI";
  }

  @Nonnull
  @Override
  public String getDescription() {
    return "Service Provider Interface";
  }

  @Nonnull
  @Override
  public String getDefaultExtension() {
    return "";
  }

  @Nullable
  @Override
  public Image getIcon() {
    return AllIcons.FileTypes.Text;
  }

  @Nullable
  @Override
  public String getCharset(@Nonnull VirtualFile file, byte[] content) {
    return CharsetToolkit.UTF8;
  }
}
