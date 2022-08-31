/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.codeInsight.template.postfix.templates;

import com.intellij.codeInsight.template.postfix.util.JavaPostfixTemplatesUtils;
import com.intellij.codeInsight.unwrap.ScopeHighlighter;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Pass;
import com.intellij.psi.PsiElement;
import com.intellij.java.language.psi.PsiExpression;
import com.intellij.java.language.psi.PsiType;
import com.intellij.java.language.psi.util.PsiExpressionTrimRenderer;
import com.intellij.refactoring.IntroduceTargetChooser;
import com.intellij.refactoring.introduceVariable.IntroduceVariableBase;
import com.intellij.util.containers.ContainerUtil;
import javax.annotation.Nonnull;

import java.util.List;

/**
 * @author ignatov
 */
public abstract class ExpressionPostfixTemplateWithChooser extends PostfixTemplate {
  protected ExpressionPostfixTemplateWithChooser(@Nonnull String name, @Nonnull String example) {
    super(name, example);
  }

  protected ExpressionPostfixTemplateWithChooser(@Nonnull String name,
                                                 @Nonnull String key,
                                                 @Nonnull String example) {
    super(name, key, example);
  }

  @Override
  public boolean isApplicable(@Nonnull PsiElement context, @Nonnull Document copyDocument, int newOffset) {
    return !getExpressions(context, copyDocument, newOffset).isEmpty();
  }

  @Override
  public void expand(@Nonnull PsiElement context, @Nonnull final Editor editor) {
    List<PsiExpression> expressions = getExpressions(context, editor.getDocument(), editor.getCaretModel().getOffset());

    if (expressions.isEmpty()) {
      PostfixTemplatesUtils.showErrorHint(context.getProject(), editor);
    }
    else if (expressions.size() == 1) {
      doIt(editor, expressions.get(0));
    }
    else {
      IntroduceTargetChooser.showChooser(
        editor, expressions,
        new Pass<PsiExpression>() {
          public void pass(@Nonnull final PsiExpression e) {
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
              @Override
              public void run() {
                CommandProcessor.getInstance().executeCommand(e.getProject(), new Runnable() {
                  public void run() {
                    doIt(editor, e);
                  }
                }, "Expand postfix template", PostfixLiveTemplate.POSTFIX_TEMPLATE_ID);
              }
            });
          }
        },
        new PsiExpressionTrimRenderer.RenderFunction(),
        "Expressions", 0, ScopeHighlighter.NATURAL_RANGER
      );
    }
  }

  @Nonnull
  protected List<PsiExpression> getExpressions(@Nonnull PsiElement context, @Nonnull Document document, final int offset) {
    List<PsiExpression> expressions = ContainerUtil.filter(IntroduceVariableBase.collectExpressions(context.getContainingFile(), document,
                                                                                                    Math.max(offset - 1, 0), false),
                                                           new Condition<PsiExpression>() {
                                                             @Override
                                                             public boolean value(PsiExpression expression) {
                                                               return expression.getTextRange().getEndOffset() == offset;
                                                             }
                                                           }
    );
    return ContainerUtil.filter(expressions.isEmpty() ? maybeTopmostExpression(context) : expressions, getTypeCondition());
  }

  @Nonnull
  @SuppressWarnings("unchecked")
  protected Condition<PsiExpression> getTypeCondition() {
    return Condition.TRUE;
  }

  @Nonnull
  private static List<PsiExpression> maybeTopmostExpression(@Nonnull PsiElement context) {
    PsiExpression expression = JavaPostfixTemplatesUtils.getTopmostExpression(context);
    PsiType type = expression != null ? expression.getType() : null;
    if (type == null || PsiType.VOID.equals(type)) return ContainerUtil.emptyList();
    return ContainerUtil.createMaybeSingletonList(expression);
  }

  protected abstract void doIt(@Nonnull Editor editor, @Nonnull PsiExpression expression);
}
