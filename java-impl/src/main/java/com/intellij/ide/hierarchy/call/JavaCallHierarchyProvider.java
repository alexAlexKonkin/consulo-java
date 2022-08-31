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
package com.intellij.ide.hierarchy.call;

import javax.annotation.Nonnull;

import com.intellij.ide.hierarchy.CallHierarchyBrowserBase;
import com.intellij.ide.hierarchy.HierarchyBrowser;
import com.intellij.ide.hierarchy.HierarchyProvider;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.java.language.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * @author yole
 */
public class JavaCallHierarchyProvider implements HierarchyProvider {
  @Override
  public PsiElement getTarget(@Nonnull final DataContext dataContext) {
    final Project project = dataContext.getData(CommonDataKeys.PROJECT);
    if (project == null) return null;

    final PsiElement element = dataContext.getData(LangDataKeys.PSI_ELEMENT);
    return PsiTreeUtil.getParentOfType(element, PsiMethod.class, false);
  }

  @Override
  @Nonnull
  public HierarchyBrowser createHierarchyBrowser(final PsiElement target) {
    return new CallHierarchyBrowser(target.getProject(), (PsiMethod) target);
  }

  @Override
  public void browserActivated(@Nonnull final HierarchyBrowser hierarchyBrowser) {
    ((CallHierarchyBrowser) hierarchyBrowser).changeView(CallHierarchyBrowserBase.CALLER_TYPE);
  }
}
