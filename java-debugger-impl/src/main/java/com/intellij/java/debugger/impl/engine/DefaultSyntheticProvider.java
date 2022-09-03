/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.java.debugger.impl.engine;

import com.intellij.java.debugger.engine.SyntheticTypeComponentProvider;
import com.intellij.java.debugger.impl.DebuggerUtilsEx;
import consulo.annotation.component.ExtensionImpl;
import consulo.internal.com.sun.jdi.TypeComponent;
import consulo.internal.com.sun.jdi.VirtualMachine;

import javax.annotation.Nonnull;

/**
 * @author Nikolay.Tropin
 */
@ExtensionImpl(id = "default")
public class DefaultSyntheticProvider implements SyntheticTypeComponentProvider {
  @Override
  public boolean isSynthetic(@Nonnull TypeComponent typeComponent) {
    return checkIsSynthetic(typeComponent);
  }

  public static boolean checkIsSynthetic(@Nonnull TypeComponent typeComponent) {
    String name = typeComponent.name();
    if (DebuggerUtilsEx.isLambdaName(name)) {
      return false;
    } else {
      if (DebuggerUtilsEx.isLambdaClassName(typeComponent.declaringType().name())) {
        return true;
      }
    }
    VirtualMachine machine = typeComponent.virtualMachine();
    if (machine != null && machine.canGetSyntheticAttribute()) {
      return typeComponent.isSynthetic();
    } else {
      return name.contains("$");
    }
  }
}
