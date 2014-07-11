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
package com.intellij.core;

import com.intellij.codeInsight.runner.JavaMainMethodProvider;
import com.intellij.ide.highlighter.JarArchiveFileType;
import com.intellij.ide.highlighter.JavaClassFileType;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.lang.java.JavaParserDefinition;
import com.intellij.navigation.ItemPresentationProviders;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.projectRoots.JavaVersionService;
import com.intellij.openapi.util.ClassExtension;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.*;
import com.intellij.psi.augment.PsiAugmentProvider;
import com.intellij.psi.impl.EmptySubstitutorImpl;
import com.intellij.psi.impl.LanguageConstantExpressionEvaluator;
import com.intellij.psi.impl.PsiExpressionEvaluator;
import com.intellij.psi.impl.compiled.ClassFileStubBuilder;
import com.intellij.psi.impl.file.PsiPackageImplementationHelper;
import com.intellij.psi.presentation.java.ClassPresentationProvider;
import com.intellij.psi.presentation.java.FieldPresentationProvider;
import com.intellij.psi.presentation.java.MethodPresentationProvider;
import com.intellij.psi.presentation.java.PackagePresentationProvider;
import com.intellij.psi.presentation.java.VariablePresentationProvider;
import com.intellij.psi.stubs.BinaryFileStubBuilders;

/**
 * @author yole
 */
public class JavaCoreApplicationEnvironment extends CoreApplicationEnvironment {
  public JavaCoreApplicationEnvironment(Disposable parentDisposable) {
    super(parentDisposable);

    registerFileType(JavaClassFileType.INSTANCE, "class");
    registerFileType(JavaFileType.INSTANCE, "java");
    registerFileType(JarArchiveFileType.INSTANCE, "jar");

    addExplicitExtension(FileTypeFileViewProviders.INSTANCE, JavaClassFileType.INSTANCE,  new ClassFileViewProviderFactory());
    addExplicitExtension(BinaryFileStubBuilders.INSTANCE, JavaClassFileType.INSTANCE, new ClassFileStubBuilder());

    addExplicitExtension(LanguageParserDefinitions.INSTANCE, JavaLanguage.INSTANCE, new JavaParserDefinition());
    addExplicitExtension(LanguageConstantExpressionEvaluator.INSTANCE, JavaLanguage.INSTANCE, new PsiExpressionEvaluator());

    registerExtensionPoint(Extensions.getRootArea(), PsiAugmentProvider.EP_NAME, PsiAugmentProvider.class);
    registerExtensionPoint(Extensions.getRootArea(), JavaMainMethodProvider.EP_NAME, JavaMainMethodProvider.class);

    myApplication.registerService(PsiPackageImplementationHelper.class, new CorePsiPackageImplementationHelper());

    myApplication.registerService(EmptySubstitutor.class, new EmptySubstitutorImpl());
    myApplication.registerService(JavaDirectoryService.class, createJavaDirectoryService());
    myApplication.registerService(JavaVersionService.class, new JavaVersionService());

    addExplicitExtension(ItemPresentationProviders.INSTANCE, PsiJavaPackage.class, new PackagePresentationProvider());
    addExplicitExtension(ItemPresentationProviders.INSTANCE, PsiClass.class, new ClassPresentationProvider());
    addExplicitExtension(ItemPresentationProviders.INSTANCE, PsiMethod.class, new MethodPresentationProvider());
    addExplicitExtension(ItemPresentationProviders.INSTANCE, PsiField.class, new FieldPresentationProvider());
    addExplicitExtension(ItemPresentationProviders.INSTANCE, PsiLocalVariable.class, new VariablePresentationProvider());
    addExplicitExtension(ItemPresentationProviders.INSTANCE, PsiParameter.class, new VariablePresentationProvider());
  }

  protected CoreJavaDirectoryService createJavaDirectoryService() {
    return new CoreJavaDirectoryService();
  }

  public <T> void addExplicitExtension(final ClassExtension<T> instance, final Class clazz, final T object) {
    instance.addExplicitExtension(clazz, object);
    Disposer.register(getParentDisposable(), new Disposable() {
      @Override
      public void dispose() {
        instance.removeExplicitExtension(clazz, object);
      }
    });
  }
}
