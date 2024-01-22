package com.intellij.java.impl.codeInsight.template;

import com.intellij.java.impl.codeInsight.completion.JavaKeywordCompletion;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;

@ExtensionImpl
public class JavaDeclarationContextType extends JavaCodeContextType {
  public JavaDeclarationContextType() {
    super("JAVA_DECLARATION", "Declaration", JavaGenericContextType.class);
  }

  @Override
  protected boolean isInContext(@Nonnull PsiElement element) {
    if (JavaStatementContextType.isStatementContext(element) || JavaExpressionContextType.isExpressionContext(element)) {
      return false;
    }

    return JavaKeywordCompletion.isSuitableForClass(element) || JavaKeywordCompletion.isInsideParameterList(element);
  }
}
