// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.java.language.psi.util;

import com.intellij.java.language.JavaPsiBundle;
import com.intellij.java.language.psi.*;
import consulo.language.psi.PsiElement;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.PropertyKey;

import javax.annotation.Nonnull;

/**
 * Represents a kind of element that appears in Java source code.
 * The main purpose of this enum is to be able to display localized element name in UI
 */
public enum JavaElementKind {
  ABSTRACT_METHOD("element.abstract_method"),
  ANNOTATION("element.annotation"),
  ANONYMOUS_CLASS("element.anonymous_class"),
  CLASS("element.class"),
  CONSTANT("element.constant"),
  CONSTRUCTOR("element.constructor"),
  ENUM("element.enum"),
  ENUM_CONSTANT("element.enum_constant"),
  EXPRESSION("element.expression"),
  FIELD("element.field"),
  INITIALIZER("element.initializer"),
  INTERFACE("element.interface"),
  LABEL("element.label"),
  LOCAL_VARIABLE("element.local_variable"),
  METHOD("element.method"),
  MODULE("element.module"),
  PACKAGE("element.package"),
  PARAMETER("element.parameter"),
  PATTERN_VARIABLE("element.pattern_variable"),
  RECORD("element.record"),
  RECORD_COMPONENT("element.record_component"),
  STATEMENT("element.statement"),
  UNKNOWN("element.unknown"),
  VARIABLE("element.variable");

  private final
  @PropertyKey(resourceBundle = JavaPsiBundle.BUNDLE)
  String propertyKey;

  JavaElementKind(@PropertyKey(resourceBundle = JavaPsiBundle.BUNDLE) String key) {
    propertyKey = key;
  }

  /**
   * @return human-readable name of the item having the subject role in the sentence (nominative case)
   */
  @Nls
  @Nonnull
  public String subject() {
    return JavaPsiBundle.message(propertyKey, 0);
  }

  /**
   * @return human-readable name of the item having the object role in the sentence (accusative case)
   */
  @Nls
  @Nonnull
  public String object() {
    return JavaPsiBundle.message(propertyKey, 1);
  }

  /**
   * @return less descriptive type for this type; usually result can be described in a single word
   * (e.g. LOCAL_VARIABLE is replaced with VARIABLE).
   */
  public
  @Nonnull
  JavaElementKind lessDescriptive() {
    switch (this) {
      case ABSTRACT_METHOD:
        return METHOD;
      case LOCAL_VARIABLE:
      case PATTERN_VARIABLE:
        return VARIABLE;
      case CONSTANT:
        return FIELD;
      case ANONYMOUS_CLASS:
        return CLASS;
      default:
        return this;
    }
  }

  /**
   * @param element element to get the kind from
   * @return resulting kind
   */
  public static JavaElementKind fromElement(@Nonnull PsiElement element) {
    if (element instanceof PsiClass) {
      PsiClass psiClass = (PsiClass) element;
      if (psiClass instanceof PsiAnonymousClass) {
        return ANONYMOUS_CLASS;
      }
      if (psiClass.isEnum()) {
        return ENUM;
      }
      if (psiClass.isRecord()) {
        return RECORD;
      }
      if (psiClass.isAnnotationType()) {
        return ANNOTATION;
      }
      if (psiClass.isInterface()) {
        return INTERFACE;
      }
      return CLASS;
    }
    if (element instanceof PsiMethod) {
      if (((PsiMethod) element).isConstructor()) {
        return CONSTRUCTOR;
      }
      if (((PsiMethod) element).hasModifierProperty(PsiModifier.ABSTRACT)) {
        return ABSTRACT_METHOD;
      }
      return METHOD;
    }
    if (element instanceof PsiField) {
      PsiField field = (PsiField) element;
      if (field instanceof PsiEnumConstant) {
        return ENUM_CONSTANT;
      }
      if (field.hasModifierProperty(PsiModifier.STATIC) && field.hasModifierProperty(PsiModifier.FINAL)) {
        return CONSTANT;
      }
      return FIELD;
    }
    if (element instanceof PsiRecordComponent) {
      return RECORD_COMPONENT;
    }
    if (element instanceof PsiLocalVariable) {
      return LOCAL_VARIABLE;
    }
    if (element instanceof PsiPatternVariable) {
      return PATTERN_VARIABLE;
    }
    if (element instanceof PsiParameter) {
      return PARAMETER;
    }
    if (element instanceof PsiVariable) {
      return VARIABLE;
    }
    if (element instanceof PsiJavaPackage) {
      return PACKAGE;
    }
    if (element instanceof PsiJavaModule) {
      return MODULE;
    }
    if (element instanceof PsiClassInitializer) {
      return INITIALIZER;
    }
    if (element instanceof PsiLabeledStatement) {
      return LABEL;
    }
    if (element instanceof PsiStatement) {
      return STATEMENT;
    }
    if (element instanceof PsiExpression) {
      return EXPRESSION;
    }
    return UNKNOWN;
  }
}
