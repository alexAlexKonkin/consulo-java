/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.java.impl.ig.serialization;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import consulo.annotation.component.ExtensionImpl;
import org.jetbrains.annotations.Nls;
import com.intellij.java.impl.codeInsight.daemon.impl.quickfix.AddDefaultConstructorFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.project.Project;
import com.intellij.java.language.psi.PsiAnonymousClass;
import com.intellij.java.language.psi.PsiClass;
import consulo.language.psi.PsiElement;
import com.intellij.java.language.psi.PsiMethod;
import com.intellij.java.language.psi.PsiModifier;
import com.intellij.java.language.psi.PsiParameterList;
import com.intellij.java.language.psi.PsiTypeParameter;
import consulo.language.util.IncorrectOperationException;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.intellij.java.impl.ig.DelegatingFix;
import com.siyeh.ig.InspectionGadgetsFix;
import com.siyeh.ig.psiutils.ClassUtils;

/**
 * @author Bas Leijdekkers
 */
@ExtensionImpl
public class ExternalizableWithoutPublicNoArgConstructorInspection extends BaseInspection {

  @Nls
  @Nonnull
  @Override
  public String getDisplayName() {
    return InspectionGadgetsBundle.message("externalizable.without.public.no.arg.constructor.display.name");
  }

  @Nonnull
  @Override
  protected String buildErrorString(Object... infos) {
    return InspectionGadgetsBundle.message("externalizable.without.public.no.arg.constructor.problem.descriptor");
  }

  @Override
  protected InspectionGadgetsFix buildFix(Object... infos) {
    final PsiMethod constructor = (PsiMethod)infos[1];
    if (constructor == null) {
      final PsiClass aClass = (PsiClass)infos[0];
      if (aClass instanceof PsiAnonymousClass) {
        // can't create constructor for anonymous class
        return null;
      }
      return new DelegatingFix(new AddDefaultConstructorFix(aClass, PsiModifier.PUBLIC));
    }
    else {
      return new MakeConstructorPublicFix();
    }
  }

  @Nullable
  private static PsiMethod getNoArgConstructor(PsiClass aClass) {
    final PsiMethod[] constructors = aClass.getConstructors();
    for (PsiMethod constructor : constructors) {
      final PsiParameterList parameterList = constructor.getParameterList();
      if (parameterList.getParametersCount() == 0) {
        return constructor;
      }
    }
    return null;
  }

  private static class MakeConstructorPublicFix extends InspectionGadgetsFix {

    @Override
    @Nonnull
    public String getName() {
      return InspectionGadgetsBundle.message("make.constructor.public");
    }

    @Override
    public void doFix(Project project, ProblemDescriptor descriptor)
      throws IncorrectOperationException {
      final PsiElement classNameIdentifier = descriptor.getPsiElement();
      final PsiClass aClass = (PsiClass)classNameIdentifier.getParent();
      if (aClass == null) {
        return;
      }
      final PsiMethod constructor = getNoArgConstructor(aClass);
      if (constructor == null) {
        return;
      }
      constructor.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);
    }
  }

  @Override
  public BaseInspectionVisitor buildVisitor() {
    return new ExternalizableWithoutPublicNoArgConstructorVisitor();
  }

  private static class ExternalizableWithoutPublicNoArgConstructorVisitor extends BaseInspectionVisitor {

    @Override
    public void visitClass(@Nonnull PsiClass aClass) {
      if (aClass.isInterface() || aClass.isEnum() || aClass.isAnnotationType() || aClass instanceof PsiTypeParameter) {
        return;
      }
      if (aClass.hasModifierProperty(PsiModifier.ABSTRACT)) {
        return;
      }
      if (!isExternalizable(aClass)) {
        return;
      }
      final PsiMethod constructor = getNoArgConstructor(aClass);
      if (constructor == null) {
        if (aClass.hasModifierProperty(PsiModifier.PUBLIC)) {
          return;
        }
      } else {
        if (constructor.hasModifierProperty(PsiModifier.PUBLIC)) {
          return;
        }
      }
      registerClassError(aClass, aClass, constructor);
    }

    private static boolean isExternalizable(PsiClass aClass) {
      final PsiClass externalizableClass = ClassUtils.findClass("java.io.Externalizable", aClass);
      return externalizableClass != null && aClass.isInheritor(externalizableClass, true);
    }
  }
}