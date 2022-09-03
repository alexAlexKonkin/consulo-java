package com.intellij.java.impl.codeInsight.unwrap;

import consulo.language.editor.CodeInsightBundle;
import com.intellij.java.language.psi.PsiConditionalExpression;
import consulo.language.psi.PsiElement;
import com.intellij.java.language.psi.PsiExpressionList;
import consulo.language.util.IncorrectOperationException;

import java.util.List;

/**
 * @author Sergey Evdokimov
 */
public class JavaConditionalUnwrapper extends JavaUnwrapper {
  public JavaConditionalUnwrapper() {
    super(CodeInsightBundle.message("unwrap.conditional"));
  }

  @Override
  public boolean isApplicableTo(PsiElement e) {
    return e.getParent() instanceof PsiConditionalExpression;
  }

  @Override
  public PsiElement collectAffectedElements(PsiElement e, List<PsiElement> toExtract) {
    super.collectAffectedElements(e, toExtract);
    return e.getParent();
  }
  
  @Override
  protected void doUnwrap(PsiElement element, Context context) throws IncorrectOperationException {
    PsiConditionalExpression cond = (PsiConditionalExpression)element.getParent();

    PsiElement savedBlock;
    
    if (cond.getElseExpression() == element) {
      savedBlock = element;
    }
    else {
      savedBlock = cond.getThenExpression();
    }

    context.extractElement(savedBlock, cond);

    if (cond.getParent() instanceof PsiExpressionList) {
      context.delete(cond);
    }
    else {
      context.deleteExactly(cond);
    }
  }
}
