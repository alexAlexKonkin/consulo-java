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

package com.intellij.ide.actions;

import com.intellij.core.JavaCoreBundle;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.JavaCreateFromTemplateHandler;
import com.intellij.ide.fileTemplates.JavaTemplateUtil;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.ui.InputValidatorEx;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.*;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.PlatformIcons;
import org.consulo.java.platform.module.extension.JavaModuleExtensionImpl;
import org.consulo.module.extension.ModuleExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * The standard "New Class" action.
 *
 * @since 5.1
 */
public class CreateClassAction extends JavaCreateTemplateInPackageAction<PsiClass> {
  public CreateClassAction() {
    super(JavaCoreBundle.message("action.NewClass.text"), JavaCoreBundle.message("action.create.new.class.description"), PlatformIcons.CLASS_ICON, true);
  }

  @Override
  protected void buildDialog(final Project project, PsiDirectory directory, CreateFileFromTemplateDialog.Builder builder) {
    builder
      .setTitle(JavaCoreBundle.message("action.create.new.class"))
      .addKind("Class", PlatformIcons.CLASS_ICON, JavaTemplateUtil.INTERNAL_CLASS_TEMPLATE_NAME)
      .addKind("Interface", PlatformIcons.INTERFACE_ICON, JavaTemplateUtil.INTERNAL_INTERFACE_TEMPLATE_NAME);

    Module module = ModuleUtilCore.findModuleForPsiElement(directory);
    assert module != null;
    JavaModuleExtensionImpl moduleExtension = ModuleRootManager.getInstance(module).getExtension(JavaModuleExtensionImpl.class);

    assert moduleExtension != null;
    if (moduleExtension.getLanguageLevel().isAtLeast(LanguageLevel.JDK_1_5)) {
      builder.addKind("Enum", PlatformIcons.ENUM_ICON, JavaTemplateUtil.INTERNAL_ENUM_TEMPLATE_NAME);
      builder.addKind("Annotation", PlatformIcons.ANNOTATION_TYPE_ICON, JavaTemplateUtil.INTERNAL_ANNOTATION_TYPE_TEMPLATE_NAME);
    }
    
    for (FileTemplate template : FileTemplateManager.getInstance().getAllTemplates()) {
      final JavaCreateFromTemplateHandler handler = new JavaCreateFromTemplateHandler();
      if (handler.handlesTemplate(template) && JavaCreateFromTemplateHandler.canCreate(directory)) {
        builder.addKind(template.getName(), JavaFileType.INSTANCE.getIcon(), template.getName());
      }
    }
    
    builder.setValidator(new InputValidatorEx() {
      @Override
      public String getErrorText(String inputString) {
        if (inputString.length() > 0 && !JavaPsiFacade.getInstance(project).getNameHelper().isQualifiedName(inputString)) {
          return "This is not a valid Java qualified name";
        }
        return null;
      }

      @Override
      public boolean checkInput(String inputString) {
        return true;
      }

      @Override
      public boolean canClose(String inputString) {
        return !StringUtil.isEmptyOrSpaces(inputString) && getErrorText(inputString) == null;
      }
    });
  }

  @Nullable
  @Override
  protected Class<? extends ModuleExtension> getModuleExtensionClass() {
    return JavaModuleExtensionImpl.class;
  }

  @Override
  protected String getErrorTitle() {
    return JavaCoreBundle.message("title.cannot.create.class");
  }

  @Override
  protected String getActionName(PsiDirectory directory, String newName, String templateName) {
    return JavaCoreBundle.message("progress.creating.class", StringUtil.getQualifiedName(JavaDirectoryService.getInstance().getPackage(directory).getQualifiedName(), newName));
  }

  protected final PsiClass doCreate(PsiDirectory dir, String className, String templateName) throws IncorrectOperationException {
    return JavaDirectoryService.getInstance().createClass(dir, className, templateName, true);
  }

  @Override
  protected PsiElement getNavigationElement(@NotNull PsiClass createdElement) {
    return createdElement.getLBrace();
  }

  @Override
  protected void postProcess(PsiClass createdElement, String templateName, Map<String, String> customProperties) {
    super.postProcess(createdElement, templateName, customProperties);

    moveCaretAfterNameIdentifier(createdElement);
  }
}
