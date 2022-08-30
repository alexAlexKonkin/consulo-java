// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.java.analysis.impl.codeInsight.daemon.impl;

import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.java.analysis.JavaAnalysisBundle;
import com.intellij.java.analysis.impl.codeInsight.daemon.impl.analysis.JavaModuleGraphUtil;
import com.intellij.java.analysis.impl.psi.impl.source.resolve.reference.impl.JavaReflectionReferenceUtil;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.psi.*;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.containers.ContainerUtil;
import com.siyeh.ig.callMatcher.CallMatcher;
import consulo.java.psi.impl.icon.JavaPsiImplIconGroup;
import one.util.streamex.StreamEx;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class JavaServiceUtil {
  public static final CallMatcher SERVICE_LOADER_LOAD = CallMatcher.staticCall("java.util.ServiceLoader", "load", "loadInstalled");

  public static boolean isServiceProviderMethod(@Nonnull PsiMethod method) {
    return "provider".equals(method.getName()) &&
        method.getParameterList().isEmpty() &&
        method.hasModifierProperty(PsiModifier.PUBLIC) &&
        method.hasModifierProperty(PsiModifier.STATIC);
  }

  @Nonnull
  public static List<LineMarkerInfo<PsiElement>> collectServiceProviderMethod(@Nonnull PsiMethod method) {
    PsiClass containingClass = method.getContainingClass();
    PsiClass resultClass = PsiUtil.resolveClassInType(method.getReturnType());
    return createJavaServiceLineMarkerInfo(method.getNameIdentifier(), containingClass, resultClass);
  }

  @Nonnull
  public static List<LineMarkerInfo<PsiElement>> collectServiceImplementationClass(@Nonnull PsiClass psiClass) {
    return createJavaServiceLineMarkerInfo(psiClass.getNameIdentifier(), psiClass, psiClass);
  }

  @Nonnull
  private static List<LineMarkerInfo<PsiElement>> createJavaServiceLineMarkerInfo(@Nullable PsiIdentifier identifier,
                                                                                  @Nullable PsiClass implementerClass,
                                                                                  @Nullable PsiClass resultClass) {
    if (identifier != null && implementerClass != null && resultClass != null) {
      String implementerClassName = implementerClass.getQualifiedName();
      if (implementerClassName != null && PsiUtil.isLanguageLevel9OrHigher(identifier)) {
        PsiJavaModule javaModule = JavaModuleGraphUtil.findDescriptorByElement(identifier);
        if (javaModule != null) {
          for (PsiProvidesStatement providesStatement : javaModule.getProvides()) {
            PsiClassType interfaceType = providesStatement.getInterfaceType();
            PsiReferenceList implementationList = providesStatement.getImplementationList();
            if (interfaceType != null && implementationList != null) {
              PsiClassType[] implementationTypes = implementationList.getReferencedTypes();
              for (PsiClassType implementationType : implementationTypes) {
                if (implementerClass.equals(implementationType.resolve())) {
                  PsiClass interfaceClass = interfaceType.resolve();
                  if (InheritanceUtil.isInheritorOrSelf(resultClass, interfaceClass, true)) {
                    String interfaceClassName = interfaceClass.getQualifiedName();
                    if (interfaceClassName != null) {
                      LineMarkerInfo<PsiElement> info =
                          new LineMarkerInfo<>(identifier, identifier.getTextRange(), JavaPsiImplIconGroup.gutterJava9Service(),
                              Pass.LINE_MARKERS,
                              e -> JavaAnalysisBundle.message("service.provides", interfaceClassName),
                              new ServiceProvidesNavigationHandler(interfaceClassName, implementerClassName),
                              GutterIconRenderer.Alignment.LEFT);
                      return Collections.singletonList(info);
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
    return Collections.emptyList();
  }

  public static List<LineMarkerInfo<PsiElement>> collectServiceLoaderLoadCall(@Nonnull PsiIdentifier identifier,
                                                                              @Nonnull PsiMethodCallExpression methodCall) {
    if (PsiUtil.isLanguageLevel9OrHigher(methodCall)) {
      PsiExpression[] arguments = methodCall.getArgumentList().getExpressions();

      JavaReflectionReferenceUtil.ReflectiveType serviceType = null;
      for (int i = 0; i < arguments.length && serviceType == null; i++) {
        serviceType = JavaReflectionReferenceUtil.getReflectiveType(arguments[i]);
      }

      if (serviceType != null && serviceType.isExact()) {
        PsiClass psiClass = serviceType.getPsiClass();
        if (psiClass != null) {
          String qualifiedName = psiClass.getQualifiedName();
          if (qualifiedName != null) {
            PsiJavaModule javaModule = JavaModuleGraphUtil.findDescriptorByElement(methodCall);
            if (javaModule != null) {
              for (PsiUsesStatement statement : javaModule.getUses()) {
                PsiClassType usedClass = statement.getClassType();
                if (usedClass != null && psiClass.equals(usedClass.resolve())) {
                  LineMarkerInfo<PsiElement> info =
                      new LineMarkerInfo<>(identifier, identifier.getTextRange(),
                          JavaPsiImplIconGroup.gutterJava9Service(),
                          Pass.LINE_MARKERS,
                          e -> JavaAnalysisBundle.message("service.uses", qualifiedName),
                          new ServiceUsesNavigationHandler(qualifiedName),
                          GutterIconRenderer.Alignment.LEFT);
                  return Collections.singletonList(info);
                }
              }
            }
          }
        }
      }
    }
    return Collections.emptyList();
  }

  abstract static class ServiceNavigationHandler implements GutterIconNavigationHandler<PsiElement> {
    final String myInterfaceClassName;

    ServiceNavigationHandler(@Nonnull String interfaceClassName) {
      myInterfaceClassName = interfaceClassName;
    }

    @Override
    public void navigate(MouseEvent e, PsiElement element) {
      Optional.ofNullable(JavaModuleGraphUtil.findDescriptorByElement(element))
          .map(this::findTargetReference)
          .filter(NavigationItem.class::isInstance)
          .map(NavigationItem.class::cast)
          .ifPresent(item -> item.navigate(true));
    }

    public abstract PsiJavaCodeReferenceElement findTargetReference(@Nonnull PsiJavaModule module);

    @Nonnull
    protected String getTargetFQN() {
      return myInterfaceClassName;
    }

    boolean isTargetReference(PsiJavaCodeReferenceElement reference) {
      return reference != null && getTargetFQN().equals(reference.getQualifiedName());
    }
  }

  private static class ServiceUsesNavigationHandler extends ServiceNavigationHandler {
    ServiceUsesNavigationHandler(String interfaceClassName) {
      super(interfaceClassName);
    }

    @Override
    public PsiJavaCodeReferenceElement findTargetReference(@Nonnull PsiJavaModule module) {
      return StreamEx.of(module.getUses().iterator())
          .map(PsiUsesStatement::getClassReference)
          .findAny(this::isTargetReference)
          .orElse(null);
    }
  }

  private static class ServiceProvidesNavigationHandler extends ServiceNavigationHandler {
    private final String myImplementerClassName;

    ServiceProvidesNavigationHandler(@Nonnull String interfaceClassName, @Nonnull String implementerClassName) {
      super(interfaceClassName);
      myImplementerClassName = implementerClassName;
    }

    @Override
    public PsiJavaCodeReferenceElement findTargetReference(@Nonnull PsiJavaModule module) {
      PsiProvidesStatement statement = ContainerUtil.find(module.getProvides(), this::isTargetStatement);
      if (statement != null) {
        PsiReferenceList list = statement.getImplementationList();
        if (list != null) {
          return ContainerUtil.find(list.getReferenceElements(), this::isTargetReference);
        }
      }

      return null;
    }

    @Nonnull
    @Override
    protected String getTargetFQN() {
      return myImplementerClassName;
    }

    private boolean isTargetStatement(@Nonnull PsiProvidesStatement statement) {
      PsiJavaCodeReferenceElement reference = statement.getInterfaceReference();
      return reference != null && myInterfaceClassName.equals(reference.getQualifiedName());
    }
  }
}