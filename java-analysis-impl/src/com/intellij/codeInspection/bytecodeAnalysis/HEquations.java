/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package com.intellij.codeInspection.bytecodeAnalysis;

import java.util.List;

import org.jetbrains.annotations.NotNull;

class HEquations {
  @NotNull
  final List<DirectionResultPair> results;
  final boolean stable;

  HEquations(@NotNull List<DirectionResultPair> results, boolean stable) {
    this.results = results;
    this.stable = stable;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HEquations that = (HEquations)o;

    if (stable != that.stable) return false;
    if (!results.equals(that.results)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = results.hashCode();
    result = 31 * result + (stable ? 1 : 0);
    return result;
  }
}
