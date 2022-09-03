/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.java.debugger.impl;

import com.intellij.java.debugger.engine.evaluation.TextWithImports;
import consulo.language.extension.LanguageExtension;
import consulo.util.lang.Pair;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;

import javax.annotation.Nullable;

/**
 * Provides text in the editor for Evaluate expression action
 * @author Maxim.Medvedev
 */
public interface EditorTextProvider {
  LanguageExtension<EditorTextProvider> EP = new LanguageExtension<EditorTextProvider>("consulo.java.debuggerEditorTextProvider");

  @Nullable
  TextWithImports getEditorText(PsiElement elementAtCaret);

  @Nullable
  Pair<PsiElement, TextRange> findExpression(PsiElement elementAtCaret, boolean allowMethodCalls);
}
