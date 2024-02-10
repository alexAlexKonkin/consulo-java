
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
package com.intellij.java.impl.refactoring.inline;

import com.intellij.java.analysis.codeInsight.intention.QuickFixFactory;
import com.intellij.java.analysis.impl.psi.controlFlow.DefUseUtil;
import com.intellij.java.impl.refactoring.HelpID;
import com.intellij.java.impl.refactoring.util.InlineUtil;
import com.intellij.java.language.impl.codeInsight.ExceptionUtil;
import com.intellij.java.language.psi.*;
import com.intellij.java.language.psi.util.PsiUtil;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.ApplicationManager;
import consulo.application.util.function.Processor;
import consulo.application.util.query.Query;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorColors;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.TextAttributes;
import consulo.language.ast.IElementType;
import consulo.language.editor.TargetElementUtil;
import consulo.language.editor.highlight.HighlightManager;
import consulo.language.editor.refactoring.RefactoringBundle;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.language.editor.refactoring.util.RefactoringMessageDialog;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.search.ReferencesSearch;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.wm.WindowManager;
import consulo.undoRedo.CommandProcessor;
import consulo.util.collection.ArrayUtil;
import jakarta.annotation.Nonnull;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ExtensionImpl
public class InlineLocalHandler extends JavaInlineActionHandler {
  private static final Logger LOG = Logger.getInstance(InlineLocalHandler.class);

  private static final String REFACTORING_NAME = RefactoringBundle.message("inline.variable.title");

  public boolean canInlineElement(PsiElement element) {
    return element instanceof PsiLocalVariable;
  }

  public void inlineElement(Project project, Editor editor, PsiElement element) {
    final PsiReference psiReference = TargetElementUtil.findReference(editor);
    final PsiReferenceExpression refExpr = psiReference instanceof PsiReferenceExpression ? ((PsiReferenceExpression) psiReference) : null;
    invoke(project, editor, (PsiLocalVariable) element, refExpr);
  }

  /**
   * should be called in AtomicAction
   */
  public static void invoke(@Nonnull final Project project, final Editor editor, final PsiLocalVariable local, PsiReferenceExpression refExpr) {
    if (!CommonRefactoringUtil.checkReadOnlyStatus(project, local)) return;

    final HighlightManager highlightManager = HighlightManager.getInstance(project);

    final String localName = local.getName();

    final Query<PsiReference> query = ReferencesSearch.search(local, GlobalSearchScope.allScope(project), false);
    if (query.findFirst() == null) {
      LOG.assertTrue(refExpr == null);
      String message = RefactoringBundle.message("variable.is.never.used", localName);
      CommonRefactoringUtil.showErrorHint(project, editor, message, REFACTORING_NAME, HelpID.INLINE_VARIABLE);
      return;
    }

    final PsiClass containingClass = PsiTreeUtil.getParentOfType(local, PsiClass.class);
    final List<PsiElement> innerClassesWithUsages = Collections.synchronizedList(new ArrayList<PsiElement>());
    final List<PsiElement> innerClassUsages = Collections.synchronizedList(new ArrayList<PsiElement>());
    query.forEach(new Processor<PsiReference>() {
      public boolean process(final PsiReference psiReference) {
        final PsiElement element = psiReference.getElement();
        PsiElement innerClass = PsiTreeUtil.getParentOfType(element, PsiClass.class, PsiLambdaExpression.class);
        while (innerClass != containingClass && innerClass != null) {
          final PsiClass parentPsiClass = PsiTreeUtil.getParentOfType(innerClass, PsiClass.class, true);
          if (parentPsiClass == containingClass) {
            if (innerClass instanceof PsiLambdaExpression) {
              if (PsiTreeUtil.isAncestor(innerClass, local, false)) {
                innerClassesWithUsages.add(element);
              } else {
                innerClassesWithUsages.add(innerClass);
              }
              innerClass = parentPsiClass;
              continue;
            }
            innerClassesWithUsages.add(innerClass);
            innerClassUsages.add(element);
          }
          innerClass = parentPsiClass;
        }
        return true;
      }
    });

    final PsiCodeBlock containerBlock = PsiTreeUtil.getParentOfType(local, PsiCodeBlock.class);
    if (containerBlock == null) {
      final String message = RefactoringBundle.getCannotRefactorMessage("Variable is declared outside a code block");
      CommonRefactoringUtil.showErrorHint(project, editor, message, REFACTORING_NAME, HelpID.INLINE_VARIABLE);
      return;
    }

    final PsiExpression defToInline = innerClassesWithUsages.isEmpty()
        ? getDefToInline(local, refExpr, containerBlock)
        : getDefToInline(local, innerClassesWithUsages.get(0), containerBlock);
    if (defToInline == null) {
      final String key = refExpr == null ? "variable.has.no.initializer" : "variable.has.no.dominating.definition";
      String message = RefactoringBundle.getCannotRefactorMessage(RefactoringBundle.message(key, localName));
      CommonRefactoringUtil.showErrorHint(project, editor, message, REFACTORING_NAME, HelpID.INLINE_VARIABLE);
      return;
    }

    final List<PsiElement> refsToInlineList = new ArrayList<PsiElement>();
    Collections.addAll(refsToInlineList, DefUseUtil.getRefs(containerBlock, local, defToInline));
    for (PsiElement innerClassUsage : innerClassUsages) {
      if (!refsToInlineList.contains(innerClassUsage)) {
        refsToInlineList.add(innerClassUsage);
      }
    }
    if (refsToInlineList.size() == 0) {
      String message = RefactoringBundle.message("variable.is.never.used.before.modification", localName);
      CommonRefactoringUtil.showErrorHint(project, editor, message, REFACTORING_NAME, HelpID.INLINE_VARIABLE);
      return;
    }
    final PsiElement[] refsToInline = PsiUtilBase.toPsiElementArray(refsToInlineList);

    EditorColorsManager manager = EditorColorsManager.getInstance();
    final TextAttributes attributes = manager.getGlobalScheme().getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES);
    final TextAttributes writeAttributes = manager.getGlobalScheme().getAttributes(EditorColors.WRITE_SEARCH_RESULT_ATTRIBUTES);
    if (refExpr != null && PsiUtil.isAccessedForReading(refExpr) && ArrayUtil.find(refsToInline, refExpr) < 0) {
      final PsiElement[] defs = DefUseUtil.getDefs(containerBlock, local, refExpr);
      LOG.assertTrue(defs.length > 0);
      highlightManager.addOccurrenceHighlights(editor, defs, attributes, true, null);
      String message = RefactoringBundle.getCannotRefactorMessage(RefactoringBundle.message("variable.is.accessed.for.writing", localName));
      CommonRefactoringUtil.showErrorHint(project, editor, message, REFACTORING_NAME, HelpID.INLINE_VARIABLE);
      WindowManager.getInstance().getStatusBar(project).setInfo(RefactoringBundle.message("press.escape.to.remove.the.highlighting"));
      return;
    }

    PsiTryStatement tryStatement = PsiTreeUtil.getParentOfType(defToInline, PsiTryStatement.class);
    if (tryStatement != null) {
      if (ExceptionUtil.getThrownExceptions(defToInline).isEmpty()) {
        tryStatement = null;
      }
    }
    PsiFile workingFile = local.getContainingFile();
    for (PsiElement ref : refsToInline) {
      final PsiFile otherFile = ref.getContainingFile();
      if (!otherFile.equals(workingFile)) {
        String message = RefactoringBundle.message("variable.is.referenced.in.multiple.files", localName);
        CommonRefactoringUtil.showErrorHint(project, editor, message, REFACTORING_NAME, HelpID.INLINE_VARIABLE);
        return;
      }
      if (tryStatement != null && !PsiTreeUtil.isAncestor(tryStatement, ref, false)) {
        CommonRefactoringUtil.showErrorHint(project, editor, "Unable to inline outside try/catch statement", REFACTORING_NAME, HelpID.INLINE_VARIABLE);
        return;
      }
    }

    for (final PsiElement ref : refsToInline) {
      final PsiElement[] defs = DefUseUtil.getDefs(containerBlock, local, ref);
      boolean isSameDefinition = true;
      for (PsiElement def : defs) {
        isSameDefinition &= isSameDefinition(def, defToInline);
      }
      if (!isSameDefinition) {
        highlightManager.addOccurrenceHighlights(editor, defs, writeAttributes, true, null);
        highlightManager.addOccurrenceHighlights(editor, new PsiElement[]{ref}, attributes, true, null);
        String message =
            RefactoringBundle.getCannotRefactorMessage(RefactoringBundle.message("variable.is.accessed.for.writing.and.used.with.inlined", localName));
        CommonRefactoringUtil.showErrorHint(project, editor, message, REFACTORING_NAME, HelpID.INLINE_VARIABLE);
        WindowManager.getInstance().getStatusBar(project).setInfo(RefactoringBundle.message("press.escape.to.remove.the.highlighting"));
        return;
      }
    }

    final PsiElement writeAccess = checkRefsInAugmentedAssignmentOrUnaryModified(refsToInline);
    if (writeAccess != null) {
      HighlightManager.getInstance(project).addOccurrenceHighlights(editor, new PsiElement[]{writeAccess}, writeAttributes, true, null);
      String message = RefactoringBundle.getCannotRefactorMessage(RefactoringBundle.message("variable.is.accessed.for.writing", localName));
      CommonRefactoringUtil.showErrorHint(project, editor, message, REFACTORING_NAME, HelpID.INLINE_VARIABLE);
      WindowManager.getInstance().getStatusBar(project).setInfo(RefactoringBundle.message("press.escape.to.remove.the.highlighting"));
      return;
    }

    if (editor != null && !ApplicationManager.getApplication().isUnitTestMode()) {
      // TODO : check if initializer uses fieldNames that possibly will be hidden by other
      // locals with the same names after inlining
      highlightManager.addOccurrenceHighlights(
          editor,
          refsToInline,
          attributes, true, null
      );
      int occurrencesCount = refsToInline.length;
      String occurencesString = RefactoringBundle.message("occurences.string", occurrencesCount);
      final String promptKey = isInliningVariableInitializer(defToInline)
          ? "inline.local.variable.prompt" : "inline.local.variable.definition.prompt";
      final String question = RefactoringBundle.message(promptKey, localName) + " " + occurencesString;
      RefactoringMessageDialog dialog = new RefactoringMessageDialog(
          REFACTORING_NAME,
          question,
          HelpID.INLINE_VARIABLE,
          "OptionPane.questionIcon",
          true,
          project);
      dialog.show();
      if (!dialog.isOK()) {
        WindowManager.getInstance().getStatusBar(project).setInfo(RefactoringBundle.message("press.escape.to.remove.the.highlighting"));
        return;
      }
    }

    final Runnable runnable = new Runnable() {
      public void run() {
        try {
          PsiExpression[] exprs = new PsiExpression[refsToInline.length];
          for (int idx = 0; idx < refsToInline.length; idx++) {
            PsiJavaCodeReferenceElement refElement = (PsiJavaCodeReferenceElement) refsToInline[idx];
            exprs[idx] = InlineUtil.inlineVariable(local, defToInline, refElement);
          }

          if (!isInliningVariableInitializer(defToInline)) {
            defToInline.getParent().delete();
          } else {
            defToInline.delete();
          }

          if (ReferencesSearch.search(local).findFirst() == null) {
            QuickFixFactory.getInstance().createRemoveUnusedVariableFix(local).invoke(project, editor, local.getContainingFile());
          }

          if (editor != null && !ApplicationManager.getApplication().isUnitTestMode()) {
            highlightManager.addOccurrenceHighlights(editor, exprs, attributes, true, null);
            WindowManager.getInstance().getStatusBar(project).setInfo(RefactoringBundle.message("press.escape.to.remove.the.highlighting"));
          }

          for (final PsiExpression expr : exprs) {
            InlineUtil.tryToInlineArrayCreationForVarargs(expr);
          }
        } catch (IncorrectOperationException e) {
          LOG.error(e);
        }
      }
    };

    CommandProcessor.getInstance().executeCommand(project, new Runnable() {
      public void run() {
        ApplicationManager.getApplication().runWriteAction(runnable);
      }
    }, RefactoringBundle.message("inline.command", localName), null);
  }

  @Nullable
  public static PsiElement checkRefsInAugmentedAssignmentOrUnaryModified(final PsiElement[] refsToInline) {
    for (PsiElement element : refsToInline) {

      PsiElement parent = element.getParent();
      if (parent instanceof PsiArrayAccessExpression) {
        if (((PsiArrayAccessExpression) parent).getIndexExpression() == element) continue;
        element = parent;
        parent = parent.getParent();
      }

      if (parent instanceof PsiAssignmentExpression && element == ((PsiAssignmentExpression) parent).getLExpression()
          || isUnaryWriteExpression(parent)) {

        return element;
      }
    }
    return null;
  }

  private static boolean isUnaryWriteExpression(PsiElement parent) {
    IElementType tokenType = null;
    if (parent instanceof PsiPrefixExpression) {
      tokenType = ((PsiPrefixExpression) parent).getOperationTokenType();
    }
    if (parent instanceof PsiPostfixExpression) {
      tokenType = ((PsiPostfixExpression) parent).getOperationTokenType();
    }
    return tokenType == JavaTokenType.PLUSPLUS || tokenType == JavaTokenType.MINUSMINUS;
  }

  private static boolean isSameDefinition(final PsiElement def, final PsiExpression defToInline) {
    if (def instanceof PsiLocalVariable) return defToInline.equals(((PsiLocalVariable) def).getInitializer());
    final PsiElement parent = def.getParent();
    return parent instanceof PsiAssignmentExpression && defToInline.equals(((PsiAssignmentExpression) parent).getRExpression());
  }

  private static boolean isInliningVariableInitializer(final PsiExpression defToInline) {
    return defToInline.getParent() instanceof PsiVariable;
  }

  @Nullable
  private static PsiExpression getDefToInline(final PsiLocalVariable local,
                                              final PsiElement refExpr,
                                              final PsiCodeBlock block) {
    if (refExpr != null) {
      PsiElement def;
      if (refExpr instanceof PsiReferenceExpression && PsiUtil.isAccessedForWriting((PsiExpression) refExpr)) {
        def = refExpr;
      } else {
        final PsiElement[] defs = DefUseUtil.getDefs(block, local, refExpr);
        if (defs.length == 1) {
          def = defs[0];
        } else {
          return null;
        }
      }

      if (def instanceof PsiReferenceExpression && def.getParent() instanceof PsiAssignmentExpression) {
        final PsiAssignmentExpression assignmentExpression = (PsiAssignmentExpression) def.getParent();
        if (assignmentExpression.getOperationTokenType() != JavaTokenType.EQ) return null;
        final PsiExpression rExpr = assignmentExpression.getRExpression();
        if (rExpr != null) return rExpr;
      }
    }
    return local.getInitializer();
  }
}
