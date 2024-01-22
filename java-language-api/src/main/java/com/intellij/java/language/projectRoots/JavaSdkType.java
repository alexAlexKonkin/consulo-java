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


package com.intellij.java.language.projectRoots;

import consulo.annotation.DeprecationInfo;
import consulo.content.bundle.Sdk;
import consulo.process.cmd.GeneralCommandLine;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;

/**
 * User: anna
 * Date: 14-Jan-2008
 */
@Deprecated
@DeprecationInfo("Use JavaSdk")
public interface JavaSdkType {
  @NonNls
  String getBinPath(Sdk sdk);

  @NonNls
  String getToolsPath(Sdk sdk);

  void setupCommandLine(@jakarta.annotation.Nonnull GeneralCommandLine commandLine, @Nonnull Sdk sdk);
}