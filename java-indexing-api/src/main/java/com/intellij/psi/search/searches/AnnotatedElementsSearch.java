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
package com.intellij.psi.search.searches;

import javax.annotation.Nonnull;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.search.SearchScope;
import com.intellij.util.InstanceofQuery;
import com.intellij.util.Query;

public class AnnotatedElementsSearch extends ExtensibleQueryFactory<PsiModifierListOwner, AnnotatedElementsSearch.Parameters> {
  public static final AnnotatedElementsSearch INSTANCE = new AnnotatedElementsSearch();

  public static class Parameters {
    private final PsiClass myAnnotationClass;
    private final SearchScope myScope;
    private final Class<? extends PsiModifierListOwner>[] myTypes;

    public Parameters(final PsiClass annotationClass, final SearchScope scope, Class<? extends PsiModifierListOwner>... types) {
      myAnnotationClass = annotationClass;
      myScope = scope;
      myTypes = types;
    }

    public PsiClass getAnnotationClass() {
      return myAnnotationClass;
    }

    public SearchScope getScope() {
      return myScope;
    }

    public Class<? extends PsiModifierListOwner>[] getTypes() {
      return myTypes;
    }
  }

  private AnnotatedElementsSearch() {
    super("consulo.java");
  }

  private static Query<PsiModifierListOwner> createDelegateQuery(PsiClass annotationClass,
                                                                 SearchScope scope,
                                                                 Class<? extends PsiModifierListOwner>... types) {
    return INSTANCE.createQuery(new Parameters(annotationClass, scope, types));
  }

  public static <T extends PsiModifierListOwner> Query<T> searchElements(@Nonnull PsiClass annotationClass,
                                                                         @Nonnull SearchScope scope,
                                                                         Class<? extends T>... types) {
    return new InstanceofQuery<T>(createDelegateQuery(annotationClass, scope, types), types);
  }

  public static Query<PsiClass> searchPsiClasses(@Nonnull PsiClass annotationClass, @Nonnull SearchScope scope) {
    return searchElements(annotationClass, scope, PsiClass.class);
  }

  public static Query<PsiMethod> searchPsiMethods(@Nonnull PsiClass annotationClass, @Nonnull SearchScope scope) {
    return searchElements(annotationClass, scope, PsiMethod.class);
  }

  public static Query<PsiMember> searchPsiMembers(@Nonnull PsiClass annotationClass, @Nonnull SearchScope scope) {
    return searchElements(annotationClass, scope, PsiMember.class);
  }

  public static Query<PsiField> searchPsiFields(@Nonnull PsiClass annotationClass, @Nonnull SearchScope scope) {
    return searchElements(annotationClass, scope, PsiField.class);
  }

  public static Query<PsiParameter> searchPsiParameters(@Nonnull PsiClass annotationClass, @Nonnull SearchScope scope) {
    return searchElements(annotationClass, scope, PsiParameter.class);
  }
}
