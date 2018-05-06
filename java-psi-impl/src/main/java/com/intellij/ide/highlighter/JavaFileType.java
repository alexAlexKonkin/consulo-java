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
package com.intellij.ide.highlighter;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NonNls;
import com.intellij.ide.IdeBundle;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.fileTypes.LanguageFileType;
import consulo.java.JavaIcons;
import consulo.ui.image.Image;

public class JavaFileType extends LanguageFileType {
  @NonNls public static final String DEFAULT_EXTENSION = "java";
  @NonNls public static final String DOT_DEFAULT_EXTENSION = ".java";
  public static final JavaFileType INSTANCE = new JavaFileType();

  private JavaFileType() {
    super(JavaLanguage.INSTANCE);
  }

  @Override
  @Nonnull
  public String getId() {
    return "JAVA";
  }

  @Override
  @Nonnull
  public String getDescription() {
    return IdeBundle.message("filetype.description.java");
  }

  @Override
  @Nonnull
  public String getDefaultExtension() {
    return DEFAULT_EXTENSION;
  }

  @Override
  public Image getIcon() {
    return JavaIcons.FileTypes.Java;
  }
}
