/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.java.impl.codeInsight.completion;

import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.PrefixMatcher;
import com.intellij.codeInsight.lookup.AutoCompletionPolicy;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiMethod;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author peter
 */
public class JavaCompletionSession {
  private final Set<String> myAddedClasses = new HashSet<>();
  private Set<String> myKeywords = new HashSet<>();
  private final MultiMap<CompletionResultSet, LookupElement> myBatchItems = MultiMap.create();
  private final CompletionResultSet myResult;

  public JavaCompletionSession(CompletionResultSet result) {
    myResult = result;
  }

  void registerBatchItems(CompletionResultSet result, Collection<LookupElement> elements) {
    myBatchItems.putValues(result, elements);
  }

  void flushBatchItems() {
    for (Map.Entry<CompletionResultSet, Collection<LookupElement>> entry : myBatchItems.entrySet()) {
      entry.getKey().addAllElements(entry.getValue());
    }
    myBatchItems.clear();
  }

  public void addClassItem(LookupElement lookupElement) {
    PsiClass psiClass = extractClass(lookupElement);
    if (psiClass != null) {
      registerClass(psiClass);
    }
    myResult.addElement(AutoCompletionPolicy.NEVER_AUTOCOMPLETE.applyPolicy(lookupElement));
  }

  @Nonnull
	PrefixMatcher getMatcher() {
    return myResult.getPrefixMatcher();
  }

  @javax.annotation.Nullable
  private static PsiClass extractClass(LookupElement lookupElement) {
    final Object object = lookupElement.getObject();
    if (object instanceof PsiClass) {
      return (PsiClass) object;
    }
    if (object instanceof PsiMethod && ((PsiMethod) object).isConstructor()) {
      return ((PsiMethod) object).getContainingClass();
    }
    return null;
  }

  public void registerClass(@Nonnull PsiClass psiClass) {
    ContainerUtil.addIfNotNull(myAddedClasses, getClassName(psiClass));
  }

  @javax.annotation.Nullable
  private static String getClassName(@Nonnull PsiClass psiClass) {
    String name = psiClass.getQualifiedName();
    return name == null ? psiClass.getName() : name;
  }

  public boolean alreadyProcessed(@Nonnull LookupElement element) {
    final PsiClass psiClass = extractClass(element);
    return psiClass != null && alreadyProcessed(psiClass);
  }

  public boolean alreadyProcessed(@Nonnull PsiClass object) {
    final String name = getClassName(object);
    return name == null || myAddedClasses.contains(name);
  }

  public boolean isKeywordAlreadyProcessed(@Nonnull String keyword) {
    return myKeywords.contains(keyword);
  }

  void registerKeyword(@Nonnull String keyword) {
    myKeywords.add(keyword);
  }
}
