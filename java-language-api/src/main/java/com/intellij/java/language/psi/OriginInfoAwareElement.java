// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.java.language.psi;

import javax.annotation.Nullable;

/**
 * FIXME [VISTALL] this class useless? drop?
 *
 * @author Max Medvedev
 */
public interface OriginInfoAwareElement {
  @Nullable
  String getOriginInfo();
}