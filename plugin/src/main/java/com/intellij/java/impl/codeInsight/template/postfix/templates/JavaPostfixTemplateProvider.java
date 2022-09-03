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
package com.intellij.java.impl.codeInsight.template.postfix.templates;

import consulo.language.editor.completion.CompletionInitializationContext;
import consulo.ide.impl.idea.codeInsight.template.postfix.templates.PostfixLiveTemplate;
import consulo.language.editor.postfixTemplate.PostfixTemplate;
import consulo.language.editor.postfixTemplate.PostfixTemplateProvider;
import com.intellij.java.impl.codeInsight.completion.JavaCompletionContributor;
import com.intellij.java.language.psi.JavaTokenType;
import consulo.application.ApplicationManager;
import consulo.undoRedo.CommandProcessor;
import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.ide.impl.idea.openapi.editor.EditorModificationUtil;
import consulo.application.util.function.Computable;
import com.intellij.psi.*;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import java.util.Set;


public class JavaPostfixTemplateProvider implements PostfixTemplateProvider {
  public static final Key<SmartPsiElementPointer<PsiElement>> ADDED_SEMICOLON = Key.create("postfix_added_semicolon");
  private final Set<PostfixTemplate> templates;


  public JavaPostfixTemplateProvider() {
    templates = ContainerUtil.newHashSet(new AssertStatementPostfixTemplate(), new CastExpressionPostfixTemplate(),
        new ElseStatementPostfixTemplate(), new ForAscendingPostfixTemplate(), new ForDescendingPostfixTemplate(),
        new ForeachPostfixTemplate(), new FormatPostfixTemplate(), new IfStatementPostfixTemplate(), new InstanceofExpressionPostfixTemplate
            (), new InstanceofExpressionPostfixTemplate("inst"), new IntroduceFieldPostfixTemplate(), new IntroduceVariablePostfixTemplate(),
        new IsNullCheckPostfixTemplate(), new NotExpressionPostfixTemplate(), new NotExpressionPostfixTemplate("!"),
        new NotNullCheckPostfixTemplate(), new NotNullCheckPostfixTemplate("nn"), new ParenthesizedExpressionPostfixTemplate(),
        new ReturnStatementPostfixTemplate(), new SoutPostfixTemplate(), new SwitchStatementPostfixTemplate(),
        new SynchronizedStatementPostfixTemplate(), new ThrowExceptionPostfixTemplate(), new TryStatementPostfixTemplate(),
        new TryWithResourcesPostfixTemplate(), new WhileStatementPostfixTemplate());
  }

  @Nonnull
  @Override
  public Set<PostfixTemplate> getTemplates() {
    return templates;
  }

  @Override
  public boolean isTerminalSymbol(char currentChar) {
    return currentChar == '.' || currentChar == '!';
  }

  @Override
  public void preExpand(@Nonnull final PsiFile file, @Nonnull final Editor editor) {
    ApplicationManager.getApplication().assertIsDispatchThread();

    file.putUserData(ADDED_SEMICOLON, null);
    if (isSemicolonNeeded(file, editor)) {
      ApplicationManager.getApplication().runWriteAction(new Runnable() {
        @Override
        public void run() {
          CommandProcessor.getInstance().runUndoTransparentAction(new Runnable() {
            public void run() {
              Document document = file.getViewProvider().getDocument();
              assert document != null;
              EditorModificationUtil.insertStringAtCaret(editor, ";", false, false);
              PsiDocumentManager.getInstance(file.getProject()).commitDocument(document);
              PsiElement at = file.findElementAt(editor.getCaretModel().getOffset());
              if (at != null && at.getNode().getElementType() == JavaTokenType.SEMICOLON) {
                file.putUserData(ADDED_SEMICOLON, SmartPointerManager.getInstance(file.getProject()).createSmartPsiElementPointer
                    (at));
              }
            }
          });
        }
      });
    }
  }

  @Override
  public void afterExpand(@Nonnull final PsiFile file, @Nonnull final Editor editor) {
    final SmartPsiElementPointer<PsiElement> pointer = file.getUserData(ADDED_SEMICOLON);
    if (pointer != null) {
      final PsiElement addedSemicolon = pointer.getElement();
      file.putUserData(ADDED_SEMICOLON, null);
      if (addedSemicolon != null && addedSemicolon.isValid()) {
        CommandProcessor.getInstance().runUndoTransparentAction(new Runnable() {
          @Override
          public void run() {
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
              public void run() {
                addedSemicolon.delete();
              }
            });
          }
        });
      }
    }
  }

  @Nonnull
  @Override
  public PsiFile preCheck(final @Nonnull PsiFile copyFile, final @Nonnull Editor realEditor, final int currentOffset) {
    return ApplicationManager.getApplication().runReadAction(new Computable<PsiFile>() {
      @Override
      public PsiFile compute() {
        Document document = copyFile.getViewProvider().getDocument();
        assert document != null;
        CharSequence sequence = document.getCharsSequence();
        StringBuilder fileContentWithSemicolon = new StringBuilder(sequence);
        if (isSemicolonNeeded(copyFile, realEditor)) {
          fileContentWithSemicolon.insert(currentOffset, ';');
          return PostfixLiveTemplate.copyFile(copyFile, fileContentWithSemicolon);
        }

        return copyFile;
      }
    });
  }

  public static void doNotDeleteSemicolon(@Nonnull PsiFile file) {
    file.putUserData(ADDED_SEMICOLON, null);
  }

  private static boolean isSemicolonNeeded(@Nonnull PsiFile file, @Nonnull Editor editor) {
    return JavaCompletionContributor.semicolonNeeded(editor, file, CompletionInitializationContext.calcStartOffset(editor.getCaretModel().getCurrentCaret()));
  }
}
