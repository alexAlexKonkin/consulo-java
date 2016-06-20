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

interface Direction {
  final class In implements Direction {
    final int paramIndex;

    static final int NOT_NULL_MASK = 0;
    static final int NULLABLE_MASK = 1;
    /**
     * @see #NOT_NULL_MASK
     * @see #NULLABLE_MASK
     */
    final int nullityMask;

    In(int paramIndex, int nullityMask) {
      this.paramIndex = paramIndex;
      this.nullityMask = nullityMask;
    }

    @Override
    public String toString() {
      return "In " + paramIndex;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      In in = (In)o;
      if (paramIndex != in.paramIndex) return false;
      if (nullityMask != in.nullityMask) return false;
      return true;
    }

    @Override
    public int hashCode() {
      return 31 * paramIndex + nullityMask;
    }

    public int paramId() {
      return paramIndex;
    }
  }

  final class InOut implements Direction {
    final int paramIndex;
    final Value inValue;

    InOut(int paramIndex, Value inValue) {
      this.paramIndex = paramIndex;
      this.inValue = inValue;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      InOut inOut = (InOut)o;

      if (paramIndex != inOut.paramIndex) return false;
      if (inValue != inOut.inValue) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = paramIndex;
      result = 31 * result + inValue.ordinal();
      return result;
    }

    @Override
    public String toString() {
      return "InOut " + paramIndex + " " + inValue.toString();
    }

    public int paramId() {
      return paramIndex;
    }

    public int valueId() {
      return inValue.ordinal();
    }
  }

  Direction Out = new Direction() {
    @Override
    public String toString() {
      return "Out";
    }

    @Override
    public int hashCode() {
      return -1;
    }
  };

  Direction NullableOut = new Direction() {
    @Override
    public String toString() {
      return "NullableOut";
    }

    @Override
    public int hashCode() {
      return -2;
    }
  };

  Direction Pure = new Direction() {
    @Override
    public int hashCode() {
      return -3;
    }

    @Override
    public String toString() {
      return "Pure";
    }
  };
}
