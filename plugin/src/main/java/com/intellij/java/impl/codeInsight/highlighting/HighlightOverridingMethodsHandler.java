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
package com.intellij.java.impl.codeInsight.highlighting;

import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.language.editor.CodeInsightBundle;
import consulo.ide.impl.idea.codeInsight.highlighting.HighlightUsagesHandler;
import consulo.language.editor.highlight.usage.HighlightUsagesHandlerBase;
import com.intellij.java.language.psi.*;
import com.intellij.java.language.psi.util.InheritanceUtil;
import consulo.navigation.ItemPresentation;
import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.ide.impl.idea.util.Consumer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class HighlightOverridingMethodsHandler extends HighlightUsagesHandlerBase<PsiClass> {
  private final PsiElement myTarget;
  private final PsiClass myClass;

  public HighlightOverridingMethodsHandler(final Editor editor, final PsiFile file, final PsiElement target, final PsiClass psiClass) {
    super(editor, file);
    myTarget = target;
    myClass = psiClass;
  }

  @Override
  public List<PsiClass> getTargets() {
    PsiReferenceList list = PsiKeyword.EXTENDS.equals(myTarget.getText()) ? myClass.getExtendsList() : myClass.getImplementsList();
    if (list == null) return Collections.emptyList();
    final PsiClassType[] classTypes = list.getReferencedTypes();
    return ChooseClassAndDoHighlightRunnable.resolveClasses(classTypes);
  }

  @Override
  protected void selectTargets(final List<PsiClass> targets, final Consumer<List<PsiClass>> selectionConsumer) {
    new ChooseClassAndDoHighlightRunnable(targets, myEditor, CodeInsightBundle.message("highlight.overridden.classes.chooser.title")) {
      @Override
      protected void selected(PsiClass... classes) {
        selectionConsumer.consume(Arrays.asList(classes));
      }
    }.run();
  }

  @Override
  public void computeUsages(final List<PsiClass> classes) {
    FeatureUsageTracker.getInstance().triggerFeatureUsed("codeassists.highlight.implements");
    for (PsiMethod method : myClass.getMethods()) {
      List<HierarchicalMethodSignature> superSignatures = method.getHierarchicalMethodSignature().getSuperSignatures();
      for (HierarchicalMethodSignature superSignature : superSignatures) {
        PsiClass containingClass = superSignature.getMethod().getContainingClass();
        if (containingClass == null) continue;
        for (PsiClass classToAnalyze : classes) {
          if (InheritanceUtil.isInheritorOrSelf(classToAnalyze, containingClass, true)) {
            PsiIdentifier identifier = method.getNameIdentifier();
            if (identifier != null) {
              addOccurrence(identifier);
              break;
            }
          }
        }
      }
    }
    if (myReadUsages.isEmpty()) {
      if (ApplicationManager.getApplication().isUnitTestMode()) return;
      String name;
      if (classes.size() == 1) {
        final ItemPresentation presentation = classes.get(0).getPresentation();
        name = presentation != null ? presentation.getPresentableText() : "";
      } else {
        name = "";
      }
      myHintText = CodeInsightBundle.message("no.methods.overriding.0.are.found", classes.size(), name);
    } else {
      addOccurrence(myTarget);
      final int methodCount = myReadUsages.size() - 1;  // exclude 'target' keyword
      myStatusText = CodeInsightBundle.message("status.bar.overridden.methods.highlighted.message", methodCount,
          HighlightUsagesHandler.getShortcutText());
    }
  }
}
