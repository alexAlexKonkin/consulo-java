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
 * Date: 03.08.2006
 * Time: 14:01:20
 */
package com.intellij.execution.util;

import consulo.java.module.extension.JavaModuleExtension;
import com.intellij.execution.CommonJavaRunConfigurationParameters;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.JavaSdkVersion;
import com.intellij.openapi.projectRoots.Sdk;

public class JreVersionDetector {
  private String myLastAlternativeJrePath = null; //awful hack
  private boolean myLastIsJre50;


  public boolean isModuleJre50Configured(final ModuleBasedConfiguration configuration) {
    final Module module = configuration.getConfigurationModule().getModule();
    if (module != null && !module.isDisposed()) {
      final Sdk sdk = ModuleUtil.getSdk(module, JavaModuleExtension.class);
      return isJre50(sdk);
    }

    return false;
  }

  public boolean isJre50Configured(final CommonJavaRunConfigurationParameters configuration) {
    if (configuration.isAlternativeJrePathEnabled()) {
      if (configuration.getAlternativeJrePath().equals(myLastAlternativeJrePath)) return myLastIsJre50;
      myLastAlternativeJrePath = configuration.getAlternativeJrePath();
      final String versionString = JavaSdk.getJdkVersion(myLastAlternativeJrePath);
      myLastIsJre50 = versionString != null && isJre50(versionString);
      return myLastIsJre50;
    }
    return false;
  }

  private static boolean isJre50(final Sdk jdk) {
    if (jdk == null) return false;
    return JavaSdk.getInstance().isOfVersionOrHigher(jdk, JavaSdkVersion.JDK_1_5);
  }

  private static boolean isJre50(final @javax.annotation.Nullable String versionString) {
    if (versionString == null) return false;
    JavaSdkVersion version = JavaSdk.getInstance().getVersion(versionString);
    return version != null && version.isAtLeast(JavaSdkVersion.JDK_1_5);
  }
}