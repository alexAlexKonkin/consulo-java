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
package com.intellij.java.impl.lang.properties;

import com.intellij.java.impl.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceProvider;
import com.intellij.java.language.JavaLanguage;
import com.intellij.java.language.codeInsight.AnnotationUtil;
import com.intellij.java.language.patterns.PsiJavaPatterns;
import com.intellij.lang.properties.ResourceBundleReferenceProvider;
import com.intellij.lang.properties.psi.impl.PropertyValueImpl;
import consulo.language.Language;
import consulo.language.psi.*;
import consulo.language.util.ProcessingContext;

import javax.annotation.Nonnull;

import static com.intellij.java.language.patterns.PsiJavaPatterns.literalExpression;
import static com.intellij.java.language.patterns.PsiJavaPatterns.psiNameValuePair;

/**
 * @author peter
 */
public class PropertiesReferenceContributor extends PsiReferenceContributor {

  private static final JavaClassReferenceProvider CLASS_REFERENCE_PROVIDER = new JavaClassReferenceProvider() {
    public boolean isSoft() {
      return true;
    }
  };

  public void registerReferenceProviders(final PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(literalExpression(), new PropertiesReferenceProvider(true));
    registrar.registerReferenceProvider(literalExpression().withParent(
        psiNameValuePair().withName(AnnotationUtil.PROPERTY_KEY_RESOURCE_BUNDLE_PARAMETER)),
        new ResourceBundleReferenceProvider());

    registrar.registerReferenceProvider(PsiJavaPatterns.psiElement(PropertyValueImpl.class), new PsiReferenceProvider() {
      @Nonnull
      @Override
      public PsiReference[] getReferencesByElement(@Nonnull PsiElement element, @Nonnull ProcessingContext context) {
        String text = element.getText();
        String[] words = text.split("\\s");
        if (words.length != 1) return PsiReference.EMPTY_ARRAY;
        return CLASS_REFERENCE_PROVIDER.getReferencesByString(words[0], element, 0);
      }
    });
  }

  @Nonnull
  @Override
  public Language getLanguage() {
    return JavaLanguage.INSTANCE;
  }
}
