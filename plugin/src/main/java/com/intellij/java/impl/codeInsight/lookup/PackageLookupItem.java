// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.java.impl.codeInsight.lookup;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.TailType;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.icons.AllIcons;
import com.intellij.java.language.psi.PsiJavaCodeReferenceCodeFragment;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import consulo.psi.PsiPackage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author peter
 */
public class PackageLookupItem extends LookupElement {
  private final PsiPackage myPackage;
  private final String myString;
  private final boolean myAddDot;

  public PackageLookupItem(@Nonnull PsiPackage aPackage) {
    this(aPackage, null);
  }

  public PackageLookupItem(@Nonnull PsiPackage pkg, @Nullable PsiElement context) {
    myPackage = pkg;
    myString = StringUtil.notNullize(myPackage.getName());

    PsiFile file = context == null ? null : context.getContainingFile();
    myAddDot = !(file instanceof PsiJavaCodeReferenceCodeFragment) || ((PsiJavaCodeReferenceCodeFragment) file).isClassesAccepted();
  }

  @Nonnull
  @Override
  public Object getObject() {
    return myPackage;
  }

  @Nonnull
  @Override
  public String getLookupString() {
    return myString;
  }

  @Override
  public void renderElement(LookupElementPresentation presentation) {
    super.renderElement(presentation);
    if (myAddDot) {
      presentation.setItemText(myString + ".");
    }
    presentation.setIcon(AllIcons.Nodes.Package);
  }

  @Override
  public void handleInsert(@Nonnull InsertionContext context) {
    if (myAddDot) {
      context.setAddCompletionChar(false);
      TailType.DOT.processTail(context.getEditor(), context.getTailOffset());
    }
    if (myAddDot || context.getCompletionChar() == '.') {
      AutoPopupController.getInstance(context.getProject()).scheduleAutoPopup(context.getEditor());
    }
  }
}
