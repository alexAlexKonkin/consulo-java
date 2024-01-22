// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.java.analysis.impl.codeInspection.dataFlow.types;

import com.intellij.java.language.psi.PsiPrimitiveType;
import jakarta.annotation.Nonnull;

/**
 * A type that represents concrete JVM primitive type or subset of values of given type
 */
public interface DfPrimitiveType extends DfType {
  @Nonnull
  PsiPrimitiveType getPsiType();
}
