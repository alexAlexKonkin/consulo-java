/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.jam;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.ide.ServiceManager;
import consulo.language.psi.PsiElementRef;

import java.util.function.Function;

/**
 * @author peter
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class JamClassGenerator {

  public static JamClassGenerator getInstance() {
    return ServiceManager.getService(JamClassGenerator.class);
  }

  public abstract <T> Function<PsiElementRef, T> generateJamElementFactory(Class<T> aClass);

}
