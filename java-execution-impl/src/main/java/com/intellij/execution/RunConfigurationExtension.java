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
 * Created by IntelliJ IDEA.
 * User: yole
 * Date: 31.01.2007
 * Time: 13:56:12
 */
package com.intellij.execution;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.intellij.execution.configuration.RunConfigurationExtensionBase;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.listeners.RefactoringElementListener;
import consulo.java.execution.configurations.OwnJavaParameters;

public abstract class RunConfigurationExtension extends RunConfigurationExtensionBase<RunConfigurationBase>{
  public static final ExtensionPointName<RunConfigurationExtension> EP_NAME = new ExtensionPointName<>("consulo.java.runConfigurationExtension");

  public abstract <T extends RunConfigurationBase > void updateJavaParameters(final T configuration, final OwnJavaParameters params, RunnerSettings runnerSettings) throws ExecutionException;


  @Override
  protected void patchCommandLine(@Nonnull RunConfigurationBase configuration,
                                  RunnerSettings runnerSettings,
                                  @Nonnull GeneralCommandLine cmdLine,
                                  @Nonnull String runnerId)  throws ExecutionException {}

  @Override
  protected boolean isEnabledFor(@Nonnull RunConfigurationBase applicableConfiguration, @Nullable RunnerSettings runnerSettings) {
    return true;
  }

  @Override
  protected void extendTemplateConfiguration(@Nonnull RunConfigurationBase configuration) {
  }

  public void cleanUserData(RunConfigurationBase runConfigurationBase) {}

  public static void cleanExtensionsUserData(RunConfigurationBase runConfigurationBase) {
    for (RunConfigurationExtension extension : Extensions.getExtensions(EP_NAME)) {
      extension.cleanUserData(runConfigurationBase);
    }
  }

  public RefactoringElementListener wrapElementListener(PsiElement element,
                                                        RunConfigurationBase runJavaConfiguration,
                                                        RefactoringElementListener listener) {
    return listener;
  }

  public static RefactoringElementListener wrapRefactoringElementListener(PsiElement element,
                                                                          RunConfigurationBase runConfigurationBase,
                                                                          RefactoringElementListener listener) {
    for (RunConfigurationExtension extension : Extensions.getExtensions(EP_NAME)) {
      listener = extension.wrapElementListener(element, runConfigurationBase, listener);
    }
    return listener;
  }

  public  boolean isListenerDisabled(RunConfigurationBase configuration, Object listener, RunnerSettings runnerSettings) {
    return false;
  }
}