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
package com.intellij.java.impl.ide.structureView.impl.java;

import java.util.ArrayList;
import java.util.Collection;

import jakarta.annotation.Nullable;

import consulo.application.AllIcons;
import consulo.ide.IdeBundle;
import consulo.fileEditor.structureView.tree.Group;
import consulo.fileEditor.structureView.tree.TreeElement;
import consulo.navigation.ItemPresentation;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiModifierList;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import com.intellij.java.language.psi.util.PsiUtil;
import consulo.ui.image.Image;

public class SuperTypeGroup implements Group, ItemPresentation, AccessLevelProvider{
  private final SmartPsiElementPointer mySuperClassPointer;
  private final OwnershipType myOverrides;
  private final Collection<TreeElement> myChildren = new ArrayList<TreeElement>();

  public static enum OwnershipType {
    IMPLEMENTS,
    OVERRIDES,
    INHERITS
  }

  public SuperTypeGroup(PsiClass superClass, OwnershipType type) {
    myOverrides = type;
    mySuperClassPointer = SmartPointerManager.getInstance(superClass.getProject()).createSmartPsiElementPointer(superClass);
  }

  public Collection<TreeElement> getChildren() {
    return myChildren;
  }

  @Nullable
  private PsiClass getSuperClass() {
    return (PsiClass)mySuperClassPointer.getElement();
  }

  public ItemPresentation getPresentation() {
    return this;
  }

  public Image getIcon() {
    switch (myOverrides) {
      case IMPLEMENTS:
        return AllIcons.General.ImplementingMethod;
      case INHERITS:
        return AllIcons.General.InheritedMethod;
      case OVERRIDES:
        return AllIcons.General.OverridingMethod;
    }

    return null; // Can't be
  }

  public String getLocationString() {
    return null;
  }

  public String getPresentableText() {
    return toString();
  }

  public String toString() {
    final PsiClass superClass = getSuperClass();
    return superClass != null ? superClass.getName() : IdeBundle.message("node.structureview.invalid");
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SuperTypeGroup)) return false;

    final SuperTypeGroup superTypeGroup = (SuperTypeGroup)o;

    if (myOverrides != superTypeGroup.myOverrides) return false;
    final PsiClass superClass = getSuperClass();
    if (superClass != null ? !superClass .equals(superTypeGroup.getSuperClass() ) : superTypeGroup.getSuperClass()  != null) return false;

    return true;
  }

  public int hashCode() {
    final PsiClass superClass = getSuperClass();
    return superClass  != null ? superClass .hashCode() : 0;
  }

  public Object getValue() {
    return this;
  }

  public int getAccessLevel() {
    final PsiClass superClass = getSuperClass();
    PsiModifierList modifierList = superClass == null ? null : superClass.getModifierList();
    return modifierList == null ? PsiUtil.ACCESS_LEVEL_PUBLIC : PsiUtil.getAccessLevel(modifierList);
  }

  public int getSubLevel() {
    return 1;
  }

  public void addMethod(final TreeElement superMethod) {
     myChildren.add(superMethod);
  }
}
