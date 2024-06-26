/*
 * Copyright 2003-2011 Dave Griffith, Bas Leijdekkers
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
package com.intellij.java.impl.ipp.switchtoif;

import com.intellij.java.impl.codeInsight.daemon.impl.quickfix.ConvertSwitchToIfIntention;
import com.intellij.java.impl.ipp.base.Intention;
import com.intellij.java.impl.ipp.base.PsiElementPredicate;
import com.intellij.java.language.psi.PsiJavaToken;
import com.intellij.java.language.psi.PsiSwitchStatement;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.intention.IntentionMetaData;
import consulo.language.psi.PsiElement;
import consulo.language.util.IncorrectOperationException;
import jakarta.annotation.Nonnull;

@ExtensionImpl
@IntentionMetaData(ignoreId = "java.ReplaceSwitchWithIfIntention", fileExtensions = "java", categories = {"Java", "Control Flow"})
public class ReplaceSwitchWithIfIntention extends Intention {
  @Override
  @Nonnull
  public PsiElementPredicate getElementPredicate() {
    return new SwitchPredicate();
  }

  @Override
  public void processIntention(@Nonnull PsiElement element)
    throws IncorrectOperationException {
    final PsiJavaToken switchToken = (PsiJavaToken)element;
    final PsiSwitchStatement switchStatement =
      (PsiSwitchStatement)switchToken.getParent();
    if (switchStatement == null) {
      return;
    }
    ConvertSwitchToIfIntention.doProcessIntention(switchStatement);
  }

  public static boolean canProcess(@Nonnull PsiSwitchStatement switchLabelStatement) {
    return SwitchPredicate.checkSwitchStatement(switchLabelStatement);
  }
}