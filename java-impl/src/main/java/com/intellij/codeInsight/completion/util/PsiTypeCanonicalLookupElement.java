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
package com.intellij.codeInsight.completion.util;

import javax.annotation.Nonnull;
import javax.swing.Icon;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiFormatUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.PlatformIcons;
import com.intellij.util.ui.EmptyIcon;
import consulo.ide.IconDescriptorUpdaters;

/**
 * @author peter
 */
public class PsiTypeCanonicalLookupElement extends LookupElement {
  private static final Icon EMPTY_ICON = new EmptyIcon(PlatformIcons.CLASS_ICON.getIconWidth() * 2, PlatformIcons.CLASS_ICON.getIconHeight());

  private final PsiType myType;
  private final String myPresentableText;

  public PsiTypeCanonicalLookupElement(@Nonnull final PsiType type) {
    myType = type;
    myPresentableText = myType.getPresentableText();
  }

  @Nonnull
  @Override
  public Object getObject() {
    final PsiClass psiClass = getPsiClass();
    if (psiClass != null) {
      return psiClass;
    }
    return super.getObject();
  }

  @javax.annotation.Nullable
  public PsiClass getPsiClass() {
    return PsiUtil.resolveClassInType(myType);
  }

  @Override
  public boolean isValid() {
    return myType.isValid() && super.isValid();
  }

  public PsiType getPsiType() {
    return myType;
  }

  @Override
  @Nonnull
  public String getLookupString() {
    return myPresentableText;
  }

  @Override
  public void handleInsert(InsertionContext context) {
    context.getEditor().getDocument().replaceString(context.getStartOffset(), context.getStartOffset() + getLookupString().length(), getPsiType().getCanonicalText());
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (!(o instanceof PsiTypeCanonicalLookupElement)) return false;

    final PsiTypeCanonicalLookupElement that = (PsiTypeCanonicalLookupElement)o;

    if (!myType.equals(that.myType)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return myType.hashCode();
  }

  @Override
  public void renderElement(LookupElementPresentation presentation) {
    final PsiClass psiClass = getPsiClass();
    if (psiClass != null) {
      presentation.setIcon(presentation.isReal() ? IconDescriptorUpdaters.getIcon(psiClass, Iconable.ICON_FLAG_VISIBILITY) : EMPTY_ICON);
      presentation.setTailText(" (" + PsiFormatUtil.getPackageDisplayName(psiClass) + ")", true);
    }
    final PsiType type = getPsiType();
    presentation.setItemText(type.getPresentableText());
    presentation.setItemTextBold(type instanceof PsiPrimitiveType);
  }

}
