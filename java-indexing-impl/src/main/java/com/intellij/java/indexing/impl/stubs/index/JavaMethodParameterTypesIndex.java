/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

/*
 * @author max
 */
package com.intellij.java.indexing.impl.stubs.index;

import com.intellij.java.indexing.impl.search.JavaSourceFilterScope;
import com.intellij.java.language.impl.psi.impl.java.stubs.index.JavaStubIndexKeys;
import com.intellij.java.language.psi.PsiMethod;
import consulo.language.psi.stub.StringStubIndexExtension;
import consulo.language.psi.stub.StubIndex;
import consulo.language.psi.stub.StubIndexKey;
import consulo.project.Project;
import consulo.project.content.scope.ProjectAwareSearchScope;

import javax.annotation.Nonnull;
import java.util.Collection;

public class JavaMethodParameterTypesIndex extends StringStubIndexExtension<PsiMethod> {

  private static final JavaMethodParameterTypesIndex ourInstance = new JavaMethodParameterTypesIndex();

  public static JavaMethodParameterTypesIndex getInstance() {
    return ourInstance;
  }

  @Nonnull
  @Override
  public StubIndexKey<String, PsiMethod> getKey() {
    return JavaStubIndexKeys.METHOD_TYPES;
  }

  @Override
  public Collection<PsiMethod> get(@Nonnull final String s,
                                   @Nonnull final Project project,
                                   @Nonnull final ProjectAwareSearchScope scope) {
    return StubIndex.getElements(getKey(), s, project, new JavaSourceFilterScope(scope), PsiMethod.class);
  }
}