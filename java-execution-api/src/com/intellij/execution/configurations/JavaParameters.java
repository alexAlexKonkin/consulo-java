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
package com.intellij.execution.configurations;

import java.nio.charset.Charset;

import org.consulo.module.extension.JavaModuleExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.execution.CantRunException;
import com.intellij.execution.ExecutionBundle;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.OrderRootsEnumerator;
import com.intellij.openapi.roots.SdkOrderEntry;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.encoding.EncodingProjectManager;
import com.intellij.util.NotNullFunction;

public class JavaParameters extends SimpleJavaParameters {
  public static final DataKey<JavaParameters> JAVA_PARAMETERS = DataKey.create("javaParameters");

  public String getJdkPath() throws CantRunException {
    final Sdk jdk = getJdk();
    if (jdk == null) {
      throw new CantRunException(ExecutionBundle.message("no.jdk.specified..error.message"));
    }

    final String jdkHome = jdk.getHomeDirectory().getPresentableUrl();
    if (jdkHome == null || jdkHome.length() == 0) {
      throw new CantRunException(ExecutionBundle.message("home.directory.not.specified.for.jdk.error.message"));
    }
    return jdkHome;
  }

  public static final int JDK_ONLY = 0x1;
  public static final int CLASSES_ONLY = 0x2;
  private static final int TESTS_ONLY = 0x4;
  public static final int JDK_AND_CLASSES = JDK_ONLY | CLASSES_ONLY;
  public static final int JDK_AND_CLASSES_AND_TESTS = JDK_ONLY | CLASSES_ONLY | TESTS_ONLY;
  public static final int CLASSES_AND_TESTS = CLASSES_ONLY | TESTS_ONLY;

  public void configureByModule(final Module module, final int classPathType, final Sdk jdk) throws CantRunException {
    if ((classPathType & JDK_ONLY) != 0) {
      if (jdk == null) {
        throw CantRunException.noJdkConfigured();
      }
      setJdk(jdk);
    }

    if ((classPathType & CLASSES_ONLY) == 0) {
      return;
    }

    setDefaultCharset(module.getProject());
    configureEnumerator(OrderEnumerator.orderEntries(module).runtimeOnly().recursively(), classPathType, jdk).collectPaths(getClassPath());
  }

  @Nullable
  private static NotNullFunction<OrderEntry, VirtualFile[]> computeRootProvider(int classPathType, final Sdk jdk) {
    return (classPathType & JDK_ONLY) == 0 ? null : new NotNullFunction<OrderEntry, VirtualFile[]>() {
      @NotNull
      @Override
      public VirtualFile[] fun(OrderEntry orderEntry) {
          if (orderEntry instanceof SdkOrderEntry) {
            final Sdk sdk = ((SdkOrderEntry)orderEntry).getSdk();
            if(sdk == null) {
              return VirtualFile.EMPTY_ARRAY;
            }
            if(sdk.getSdkType() == jdk) {
              return jdk.getRootProvider().getFiles(OrderRootType.CLASSES);
            }
            return orderEntry.getFiles(OrderRootType.CLASSES);
          }
          return orderEntry.getFiles(OrderRootType.CLASSES);
        }
      };
  }

  public void setDefaultCharset(final Project project) {
    Charset encoding = EncodingProjectManager.getInstance(project).getDefaultCharset();
    if (encoding != null) {
      setCharset(encoding);
    }
  }

  public void configureByModule(final Module module, final int classPathType) throws CantRunException {
    configureByModule(module, classPathType, getModuleJdk(module));
  }

  public static Sdk getModuleJdk(final Module module) throws CantRunException {
    final Sdk jdk = ModuleUtilCore.getSdk(module, JavaModuleExtension.class);
    if (jdk == null) {
      throw CantRunException.noJdkForModule(module);
    }
    final VirtualFile homeDirectory = jdk.getHomeDirectory();
    if (homeDirectory == null || !homeDirectory.isValid()) {
      throw CantRunException.jdkMisconfigured(jdk, module);
    }
    return jdk;
  }

  public void configureByProject(final Project project, final int classPathType, final Sdk jdk) throws CantRunException {
    if ((classPathType & JDK_ONLY) != 0) {
      if (jdk == null) {
        throw CantRunException.noJdkConfigured();
      }
      setJdk(jdk);
    }

    if ((classPathType & CLASSES_ONLY) == 0) {
      return;
    }
    setDefaultCharset(project);
    configureEnumerator(OrderEnumerator.orderEntries(project).runtimeOnly(), classPathType, jdk).collectPaths(getClassPath());
  }

  private static OrderRootsEnumerator configureEnumerator(OrderEnumerator enumerator, int classPathType, Sdk jdk) {
    if ((classPathType & JDK_ONLY) == 0) {
      enumerator = enumerator.withoutSdk();
    }
    if ((classPathType & TESTS_ONLY) == 0) {
      enumerator = enumerator.productionOnly();
    }
    OrderRootsEnumerator rootsEnumerator = enumerator.classes();
    final NotNullFunction<OrderEntry, VirtualFile[]> provider = computeRootProvider(classPathType, jdk);
    if (provider != null) {
      rootsEnumerator = rootsEnumerator.usingCustomRootProvider(provider);
    }
    return rootsEnumerator;
  }
}