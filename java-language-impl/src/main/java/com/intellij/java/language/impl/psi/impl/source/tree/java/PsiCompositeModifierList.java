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

/*
 * @author max
 */
package com.intellij.java.language.impl.psi.impl.source.tree.java;

import com.intellij.java.language.psi.PsiAnnotation;
import consulo.language.psi.PsiManager;
import com.intellij.java.language.psi.PsiModifierList;
import com.intellij.java.language.impl.psi.impl.light.LightModifierList;
import consulo.util.collection.ContainerUtil;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

public class PsiCompositeModifierList extends LightModifierList {
  private final List<PsiModifierList> mySublists;

  public PsiCompositeModifierList(final PsiManager manager, List<PsiModifierList> sublists) {
    super(manager);
    mySublists = sublists;
  }

  @Override
  @Nonnull
  public PsiAnnotation[] getAnnotations() {
    List<PsiAnnotation> annotations = new ArrayList<PsiAnnotation>();
    for (PsiModifierList list : mySublists) {
      ContainerUtil.addAll(annotations, list.getAnnotations());
    }
    return annotations.toArray(new PsiAnnotation[annotations.size()]);
  }

  @Override
  public PsiAnnotation findAnnotation(@Nonnull final String qualifiedName) {
    for (PsiModifierList sublist : mySublists) {
      final PsiAnnotation annotation = sublist.findAnnotation(qualifiedName);
      if (annotation != null) return annotation;
    }

    return null;
  }

  @Override
  public boolean hasModifierProperty(@Nonnull final String name) {
    for (PsiModifierList sublist : mySublists) {
      if (sublist.hasModifierProperty(name)) return true;
    }
    return false;
  }

  @Override
  public boolean hasExplicitModifier(@Nonnull final String name) {
    for (PsiModifierList sublist : mySublists) {
      if (sublist.hasExplicitModifier(name)) return true;
    }
    return false;
  }
}