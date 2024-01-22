// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.java.language.psi;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * <a href="https://openjdk.org/jeps/405">JEP</a>
 * <p>
 * Represents record pattern, for example: {@code Point(int x, int y) p }
 */
public interface PsiDeconstructionPattern extends PsiPrimaryPattern {
  /**
   * The empty array of PSI deconstruction patterns which can be reused to avoid unnecessary allocations.
   */
  PsiDeconstructionPattern[] EMPTY_ARRAY = new PsiDeconstructionPattern[0];

  /**
   * @return element representing code inside '(...)' inclusive parenthesis
   */
  @Nonnull
  PsiDeconstructionList getDeconstructionList();

  /**
   * @return type of the pattern, for example in {@code Point(int x, int y) p } it is {@code Point }
   */
  @Nonnull
  PsiTypeElement getTypeElement();

  /**
   * @return pattern variable if the pattern has a name, {@code null} otherwise
   */
  @Nullable
  PsiPatternVariable getPatternVariable();
}
