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
package com.intellij.java.impl.codeInspection;

import com.intellij.java.analysis.codeInspection.GroupNames;
import com.intellij.java.analysis.codeInspection.reference.RefClass;
import com.intellij.java.analysis.codeInspection.reference.RefJavaVisitor;
import com.intellij.java.analysis.impl.codeInspection.JavaSuppressionUtil;
import com.intellij.java.impl.codeInsight.daemon.impl.RemoveSuppressWarningAction;
import com.intellij.java.language.JavaLanguage;
import com.intellij.java.language.psi.*;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.impl.inspection.GlobalInspectionContextBase;
import consulo.language.editor.impl.inspection.reference.RefManagerImpl;
import consulo.language.editor.impl.inspection.scheme.GlobalInspectionToolWrapper;
import consulo.language.editor.inspection.*;
import consulo.language.editor.inspection.reference.RefElement;
import consulo.language.editor.inspection.reference.RefEntity;
import consulo.language.editor.inspection.reference.RefVisitor;
import consulo.language.editor.inspection.scheme.*;
import consulo.language.editor.inspection.ui.SingleCheckboxOptionsPanel;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.editor.scope.AnalysisScope;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.collection.BidirectionalMap;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.WriteExternalException;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.*;

/**
 * @author cdr
 */
@ExtensionImpl
public class RedundantSuppressInspection extends GlobalInspectionTool {
  private BidirectionalMap<String, QuickFix> myQuickFixes = null;
  private static final Logger LOG = Logger.getInstance(RedundantSuppressInspection.class);

  public boolean IGNORE_ALL = false;

  @Nonnull
  @Override
  public HighlightDisplayLevel getDefaultLevel() {
    return HighlightDisplayLevel.WARNING;
  }

  @Override
  public boolean isEnabledByDefault() {
    return false;
  }

  @Nullable
  @Override
  public Language getLanguage() {
    return JavaLanguage.INSTANCE;
  }

  @Override
  @Nonnull
  public String getGroupDisplayName() {
    return GroupNames.DECLARATION_REDUNDANCY;
  }

  @Override
  @Nonnull
  public String getDisplayName() {
    return InspectionsBundle.message("inspection.redundant.suppression.name");
  }

  @Override
  @Nonnull
  @NonNls
  public String getShortName() {
    return "RedundantSuppression";
  }

  @Override
  public JComponent createOptionsPanel() {
    return new SingleCheckboxOptionsPanel("Ignore @SuppressWarning(\"ALL\")", this, "IGNORE_ALL");
  }

  @Override
  public void writeSettings(@Nonnull Element node) throws WriteExternalException {
    if (IGNORE_ALL) {
      super.writeSettings(node);
    }
  }

  @Override
  public void runInspection(@Nonnull final AnalysisScope scope,
                            @Nonnull final InspectionManager manager,
                            @Nonnull final GlobalInspectionContext globalContext,
                            @Nonnull final ProblemDescriptionsProcessor problemDescriptionsProcessor) {
    globalContext.getRefManager().iterate(new RefJavaVisitor() {
      @Override
      public void visitClass(@Nonnull RefClass refClass) {
        if (!globalContext.shouldCheck(refClass, RedundantSuppressInspection.this)) return;
        CommonProblemDescriptor[] descriptors = checkElement(refClass, manager, globalContext.getProject());
        if (descriptors != null) {
          for (CommonProblemDescriptor descriptor : descriptors) {
            if (descriptor instanceof ProblemDescriptor) {
              final PsiElement psiElement = ((ProblemDescriptor) descriptor).getPsiElement();
              final PsiMember member = PsiTreeUtil.getParentOfType(psiElement, PsiMember.class);
              final RefElement refElement = globalContext.getRefManager().getReference(member);
              if (refElement != null) {
                problemDescriptionsProcessor.addProblemElement(refElement, descriptor);
                continue;
              }
            }
            problemDescriptionsProcessor.addProblemElement(refClass, descriptor);
          }
        }
      }
    });
  }

  @Nullable
  private CommonProblemDescriptor[] checkElement(@Nonnull RefClass refEntity, @Nonnull InspectionManager manager, @Nonnull Project project) {
    final PsiClass psiClass = refEntity.getElement();
    if (psiClass == null) return null;
    return checkElement(psiClass, manager, project);
  }

  public CommonProblemDescriptor[] checkElement(@Nonnull final PsiElement psiElement, @Nonnull final InspectionManager manager, @Nonnull Project project) {
    final Map<PsiElement, Collection<String>> suppressedScopes = new HashMap<PsiElement, Collection<String>>();
    psiElement.accept(new JavaRecursiveElementWalkingVisitor() {
      @Override
      public void visitModifierList(PsiModifierList list) {
        super.visitModifierList(list);
        final PsiElement parent = list.getParent();
        if (parent instanceof PsiModifierListOwner && !(parent instanceof PsiClass)) {
          checkElement(parent);
        }
      }

      @Override
      public void visitComment(PsiComment comment) {
        checkElement(comment);
      }

      @Override
      public void visitClass(PsiClass aClass) {
        if (aClass == psiElement) {
          super.visitClass(aClass);
          checkElement(aClass);
        }
      }


      private void checkElement(final PsiElement owner) {
        String idsString = JavaSuppressionUtil.getSuppressedInspectionIdsIn(owner);
        if (idsString != null && !idsString.isEmpty()) {
          List<String> ids = StringUtil.split(idsString, ",");
          if (IGNORE_ALL && (ids.contains(SuppressionUtil.ALL) || ids.contains(SuppressionUtil.ALL.toLowerCase())))
            return;
          Collection<String> suppressed = suppressedScopes.get(owner);
          if (suppressed == null) {
            suppressed = ids;
          } else {
            for (String id : ids) {
              if (!suppressed.contains(id)) {
                suppressed.add(id);
              }
            }
          }
          suppressedScopes.put(owner, suppressed);
        }
      }
    });

    if (suppressedScopes.values().isEmpty()) return null;
    // have to visit all file from scratch since inspections can be written in any perversive way including checkFile() overriding
    Collection<InspectionToolWrapper> suppressedTools = new HashSet<InspectionToolWrapper>();
    InspectionToolWrapper[] toolWrappers = getInspectionTools(psiElement, manager);
    for (Collection<String> ids : suppressedScopes.values()) {
      for (Iterator<String> iterator = ids.iterator(); iterator.hasNext(); ) {
        final String shortName = iterator.next().trim();
        for (InspectionToolWrapper toolWrapper : toolWrappers) {
          if (toolWrapper instanceof LocalInspectionToolWrapper && ((LocalInspectionToolWrapper) toolWrapper).getTool().getID().equals(shortName)) {
            if (((LocalInspectionToolWrapper) toolWrapper).isUnfair()) {
              iterator.remove();
              break;
            } else {
              suppressedTools.add(toolWrapper);
            }
          } else if (toolWrapper.getShortName().equals(shortName)) {
            //ignore global unused as it won't be checked anyway
            if (toolWrapper instanceof LocalInspectionToolWrapper || toolWrapper instanceof GlobalInspectionToolWrapper) {
              suppressedTools.add(toolWrapper);
            } else {
              iterator.remove();
              break;
            }
          }
        }
      }
    }

    PsiFile file = psiElement.getContainingFile();
    final AnalysisScope scope = new AnalysisScope(file);

    final GlobalInspectionContextBase globalContext = new GlobalInspectionContextBase(file.getProject());
    globalContext.setCurrentScope(scope);
    final RefManagerImpl refManager = (RefManagerImpl) globalContext.getRefManager();
    refManager.inspectionReadActionStarted();
    final List<ProblemDescriptor> result;
    try {
      result = new ArrayList<ProblemDescriptor>();
      for (InspectionToolWrapper toolWrapper : suppressedTools) {
        String toolId = toolWrapper instanceof LocalInspectionToolWrapper ? ((LocalInspectionToolWrapper) toolWrapper).getTool().getID() : toolWrapper.getShortName();
        toolWrapper.initialize(globalContext);
        final Collection<CommonProblemDescriptor> descriptors;
        if (toolWrapper instanceof LocalInspectionToolWrapper) {
          LocalInspectionToolWrapper local = (LocalInspectionToolWrapper) toolWrapper;
          if (local.isUnfair()) continue; //cant't work with passes other than LocalInspectionPass
          List<ProblemDescriptor> results = local.getTool().processFile(file, manager);
          descriptors = new ArrayList<CommonProblemDescriptor>(results);
        } else if (toolWrapper instanceof GlobalInspectionToolWrapper) {
          final GlobalInspectionToolWrapper global = (GlobalInspectionToolWrapper) toolWrapper;
          GlobalInspectionTool globalTool = global.getTool();
          if (globalTool.isGraphNeeded()) {
            refManager.findAllDeclarations();
          }
          descriptors = new ArrayList<CommonProblemDescriptor>();
          globalContext.getRefManager().iterate(new RefVisitor() {
            @Override
            public void visitElement(@Nonnull RefEntity refEntity) {
              CommonProblemDescriptor[]
                  descriptors1 = global.getTool().checkElement(refEntity, scope, manager, globalContext, new ProblemDescriptionsProcessor() {
                @Nullable
                @Override
                public CommonProblemDescriptor[] getDescriptions(@Nonnull RefEntity refEntity) {
                  return new CommonProblemDescriptor[0];
                }

                @Override
                public void ignoreElement(@Nonnull RefEntity refEntity) {

                }

                @Override
                public void addProblemElement(@Nullable RefEntity refEntity, @Nonnull CommonProblemDescriptor... commonProblemDescriptors) {
                  int i = 0;
                }

                @Override
                public RefEntity getElement(@Nonnull CommonProblemDescriptor descriptor) {
                  return null;
                }
              });
              if (descriptors1 != null) {
                ContainerUtil.addAll(descriptors, descriptors1);
              }
            }
          });
        } else {
          continue;
        }
        for (PsiElement suppressedScope : suppressedScopes.keySet()) {
          Collection<String> suppressedIds = suppressedScopes.get(suppressedScope);
          if (!suppressedIds.contains(toolId)) continue;
          for (CommonProblemDescriptor descriptor : descriptors) {
            if (!(descriptor instanceof ProblemDescriptor)) continue;
            PsiElement element = ((ProblemDescriptor) descriptor).getPsiElement();
            if (element == null) continue;
            PsiElement annotation = JavaSuppressionUtil.getElementToolSuppressedIn(element, toolId);
            if (annotation != null && PsiTreeUtil.isAncestor(suppressedScope, annotation, false) || annotation == null && !PsiTreeUtil.isAncestor(suppressedScope, element, false)) {
              suppressedIds.remove(toolId);
              break;
            }
          }
        }
      }
      for (PsiElement suppressedScope : suppressedScopes.keySet()) {
        Collection<String> suppressedIds = suppressedScopes.get(suppressedScope);
        for (String toolId : suppressedIds) {
          PsiMember psiMember;
          String problemLine = null;
          if (suppressedScope instanceof PsiMember) {
            psiMember = (PsiMember) suppressedScope;
          } else {
            psiMember = PsiTreeUtil.getParentOfType(suppressedScope, PsiDocCommentOwner.class);
            final PsiStatement statement = PsiTreeUtil.getNextSiblingOfType(suppressedScope, PsiStatement.class);
            problemLine = statement != null ? statement.getText() : null;
          }
          if (psiMember != null && psiMember.isValid()) {
            String description = InspectionsBundle.message("inspection.redundant.suppression.description");
            if (myQuickFixes == null) myQuickFixes = new BidirectionalMap<String, QuickFix>();
            final String key = toolId + (problemLine != null ? ";" + problemLine : "");
            QuickFix fix = myQuickFixes.get(key);
            if (fix == null) {
              fix = new RemoveSuppressWarningAction(toolId, problemLine);
              myQuickFixes.put(key, fix);
            }
            PsiElement identifier = null;
            if (psiMember instanceof PsiMethod) {
              identifier = ((PsiMethod) psiMember).getNameIdentifier();
            } else if (psiMember instanceof PsiField) {
              identifier = ((PsiField) psiMember).getNameIdentifier();
            } else if (psiMember instanceof PsiClass) {
              identifier = ((PsiClass) psiMember).getNameIdentifier();
            }
            if (identifier == null) {
              identifier = psiMember;
            }
            result.add(
                manager.createProblemDescriptor(identifier, description, (LocalQuickFix) fix, ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    false));
          }
        }
      }
    } finally {
      refManager.inspectionReadActionFinished();
      globalContext.close(true);
    }
    return result.toArray(new ProblemDescriptor[result.size()]);
  }

  protected InspectionToolWrapper[] getInspectionTools(PsiElement psiElement, @Nonnull InspectionManager manager) {
    ModifiableModel model = InspectionProjectProfileManager.getInstance(manager.getProject()).getInspectionProfile().getModifiableModel();
    InspectionProfileWrapper profile = new InspectionProfileWrapper((InspectionProfile) model);
    profile.init(manager.getProject());

    return profile.getInspectionTools(psiElement);
  }


  @Override
  @Nullable
  public QuickFix getQuickFix(final String hint) {
    return myQuickFixes != null ? myQuickFixes.get(hint) : new RemoveSuppressWarningAction(hint);
  }


  @Override
  @Nullable
  public String getHint(@Nonnull final QuickFix fix) {
    if (myQuickFixes != null) {
      final List<String> list = myQuickFixes.getKeysByValue(fix);
      if (list != null) {
        LOG.assertTrue(list.size() == 1);
        return list.get(0);
      }
    }
    return null;
  }
}