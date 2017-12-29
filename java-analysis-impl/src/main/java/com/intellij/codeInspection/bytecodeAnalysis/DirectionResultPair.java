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

import org.jetbrains.annotations.NotNull;

class DirectionResultPair {
  final int directionKey;
  @NotNull
  final HResult hResult;

  DirectionResultPair(int directionKey, @NotNull HResult hResult) {
    this.directionKey = directionKey;
    this.hResult = hResult;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DirectionResultPair that = (DirectionResultPair)o;

    if (directionKey != that.directionKey) return false;
    if (!hResult.equals(that.hResult)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = directionKey;
    result = 31 * result + hResult.hashCode();
    return result;
  }
}