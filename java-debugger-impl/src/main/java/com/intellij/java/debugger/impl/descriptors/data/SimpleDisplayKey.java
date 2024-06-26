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
package com.intellij.java.debugger.impl.descriptors.data;

import com.intellij.java.debugger.ui.tree.NodeDescriptor;

public class SimpleDisplayKey<T extends NodeDescriptor> implements DisplayKey<T>{
  private final Object myKey;

  public SimpleDisplayKey(Object key) {
    myKey = key;
  }

  public boolean equals(Object o) {
    if(!(o instanceof SimpleDisplayKey)) return false;
    return ((SimpleDisplayKey)o).myKey.equals(myKey);
  }

  public int hashCode() {
    return myKey.hashCode();
  }
}
