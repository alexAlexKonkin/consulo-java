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
package com.intellij.java.impl.refactoring.util;

import com.intellij.java.language.impl.JavaClassFileType;
import consulo.annotation.component.ExtensionImpl;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.WritingAccessProvider;
import javax.annotation.Nonnull;

import java.util.Collection;
import java.util.Collections;

/**
 * User: ksafonov
 */
@ExtensionImpl
public class ClsElementWritingAccessProvider extends WritingAccessProvider {

  @Nonnull
  @Override
  public Collection<VirtualFile> requestWriting(final VirtualFile... files) {
    return Collections.emptyList();
  }

  @Override
  public boolean isPotentiallyWritable(@Nonnull final VirtualFile file) {
    // TODO make library class files readonly not by their file type but by location in library roots
    return file.getFileType() != JavaClassFileType.INSTANCE;
  }
}