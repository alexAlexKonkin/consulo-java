package com.intellij.java.impl.codeInsight.hints;

import com.intellij.java.language.psi.PsiCallExpression;
import com.intellij.java.language.psi.PsiMethod;
import consulo.language.editor.inlay.InlayInfo;
import consulo.language.editor.inlay.InlayParameterHintsProvider;
import consulo.language.editor.inlay.MethodInfo;
import consulo.language.psi.PsiElement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * from kotlin
 */
public abstract class JavaInlayParameterHintsProvider implements InlayParameterHintsProvider {
  private static String[] ourDefaultBlackList = {
      "(begin*, end*)",
      "(start*, end*)",
      "(first*, last*)",
      "(first*, second*)",
      "(from*, to*)",
      "(min*, max*)",
      "(key, value)",
      "(format, arg*)",
      "(message)",
      "(message, error)",
      "*Exception",
      "*.add(*)",
      "*.set(*,*)",
      "*.get(*)",
      "*.create(*)",
      "*.getProperty(*)",
      "*.setProperty(*,*)",
      "*.print(*)",
      "*.println(*)",
      "*.append(*)",
      "*.charAt(*)",
      "*.indexOf(*)",
      "*.contains(*)",
      "*.startsWith(*)",
      "*.endsWith(*)",
      "*.equals(*)",
      "*.equal(*)",
      "java.lang.Math.*",
      "org.slf4j.Logger.*"
  };

  @Nonnull
  @Override
  public List<InlayInfo> getParameterHints(@Nonnull PsiElement element) {
    if (element instanceof PsiCallExpression) {
      return List.copyOf(JavaInlayHintsProvider.createHints((PsiCallExpression) element));
    }
    return Collections.emptyList();
  }

  @Nullable
  @Override
  public MethodInfo getMethodInfo(@Nonnull PsiElement element) {
    if (element instanceof PsiCallExpression) {
      PsiElement resolvedElement = ((PsiCallExpression) element).resolveMethodGenerics().getElement();
      if (resolvedElement instanceof PsiMethod) {
        return getMethodInfo(resolvedElement);
      }
    }
    return null;
  }

  @Nonnull
  @Override
  public Set<String> getDefaultBlackList() {
    return Set.of(ourDefaultBlackList);
  }
}
