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
package com.intellij.java.debugger.impl;

import com.intellij.java.debugger.impl.engine.DebugProcessImpl;
import com.intellij.java.debugger.impl.engine.events.DebuggerCommandImpl;
import consulo.util.lang.Pair;

import java.util.ArrayList;
import java.util.List;

public class MultiProcessCommand implements Runnable{
  private final List<Pair<DebugProcessImpl,  DebuggerCommandImpl>> myCommands = new ArrayList<Pair<DebugProcessImpl, DebuggerCommandImpl>>();

  public void run() {
    while(true) {
      Pair<DebugProcessImpl,  DebuggerCommandImpl> pair;
      synchronized(myCommands) {
        if(myCommands.isEmpty()) {
          break;
        }
        pair = myCommands.remove(0);
      }
      pair.getFirst().getManagerThread().invokeAndWait(pair.getSecond());
    }
  }

  public void cancel() {
    synchronized(myCommands) {
      myCommands.clear();
    }
  }

  public boolean isEmpty() {
    return myCommands.isEmpty();
  }

  public void addCommand(DebugProcessImpl debugProcess, DebuggerCommandImpl command) {
    myCommands.add(new Pair<DebugProcessImpl, DebuggerCommandImpl>(debugProcess, command));
  }
}
