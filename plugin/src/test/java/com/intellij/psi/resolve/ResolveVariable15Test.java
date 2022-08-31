/*
 * Copyright (c) 2006 JetBrains s.r.o. All Rights Reserved.
 */
package com.intellij.psi.resolve;

import com.intellij.psi.PsiReference;
import com.intellij.java.language.psi.JavaResolveResult;
import com.intellij.java.language.psi.PsiJavaReference;
import com.intellij.psi.PsiElement;

/**
 * @author ven
 */
public abstract class ResolveVariable15Test extends Resolve15TestCase {

  public void testDuplicateStaticImport() throws Exception {
    PsiReference ref = configure();
    final JavaResolveResult result = ((PsiJavaReference)ref).advancedResolve(true);
    PsiElement target = result.getElement();
    assertNotNull(target);
    assertTrue(result.isValidResult());
  }

  public void testRhombExtending() throws Exception {
    PsiReference ref = configure();
    final JavaResolveResult result = ((PsiJavaReference)ref).advancedResolve(true);
    PsiElement target = result.getElement();
    assertNotNull(target);
    assertTrue(result.isValidResult());
  }


  private PsiReference configure() throws Exception {
    return configureByFile("var15/" + getTestName(false) + ".java");
  }
}
