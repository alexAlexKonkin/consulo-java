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
package com.intellij.java.language.impl.psi;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.ide.ServiceManager;
import consulo.language.psi.ReferenceProviderType;
import consulo.project.Project;

import javax.annotation.Nonnull;

/**
 * @author peter
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class CommonReferenceProviderTypes {

  /**
   * @see #getInstance()
   * @deprecated
   */
  public static CommonReferenceProviderTypes getInstance(final Project project) {
    return getInstance();
  }

  public static CommonReferenceProviderTypes getInstance() {
    return ServiceManager.getService(CommonReferenceProviderTypes.class);
  }

  public static final ReferenceProviderType PROPERTIES_FILE_KEY_PROVIDER = new ReferenceProviderType("Properties File Key Provider");

  @Nonnull
  public abstract JavaClassPsiReferenceProvider getClassReferenceProvider();

  @Nonnull
  public abstract JavaClassPsiReferenceProvider getSoftClassReferenceProvider();
}