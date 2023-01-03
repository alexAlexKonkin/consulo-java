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
package com.intellij.java.indexing.search.searches;

import consulo.component.extension.ExtensionPointName;
import com.intellij.java.language.psi.PsiClass;
import consulo.content.scope.SearchScope;
import consulo.application.util.query.ExtensibleQueryFactory;
import consulo.application.util.query.Query;
import consulo.application.util.query.QueryExecutor;

import javax.annotation.Nonnull;

/**
 * Searcher that searches for classes which have members annotated with the specified annotation.
 *
 * @author yole
 */
public class ClassesWithAnnotatedMembersSearch extends ExtensibleQueryFactory<PsiClass, ClassesWithAnnotatedMembersSearch.Parameters> {
  public static final ClassesWithAnnotatedMembersSearch INSTANCE = new ClassesWithAnnotatedMembersSearch();

  public static class Parameters {
    private final PsiClass myAnnotationClass;
    private final SearchScope myScope;

    public Parameters(final PsiClass annotationClass, final SearchScope scope) {
      myAnnotationClass = annotationClass;
      myScope = scope;
    }

    public PsiClass getAnnotationClass() {
      return myAnnotationClass;
    }

    public SearchScope getScope() {
      return myScope;
    }
  }

  public ClassesWithAnnotatedMembersSearch() {
    super(ClassesWithAnnotatedMembersSearchExecutor.class);
  }

  public static Query<PsiClass> search(@Nonnull PsiClass annotationClass, @Nonnull SearchScope scope) {
    return INSTANCE.createQuery(new Parameters(annotationClass, scope));
  }
}