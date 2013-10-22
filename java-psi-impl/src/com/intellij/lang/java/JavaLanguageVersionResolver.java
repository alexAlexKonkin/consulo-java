package com.intellij.lang.java;

import com.intellij.lang.Language;
import com.intellij.lang.LanguageVersion;
import com.intellij.lang.LanguageVersionResolver;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.PsiElement;
import org.consulo.java.module.extension.JavaModuleExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 19:43/30.05.13
 */
public class JavaLanguageVersionResolver implements LanguageVersionResolver {
  @NotNull
  @Override
  public LanguageVersion getLanguageVersion(@NotNull Language language, @Nullable PsiElement element) {
    if (element == null) {
      return LanguageLevel.HIGHEST;
    }
    else {
      final Module moduleForPsiElement = ModuleUtilCore.findModuleForPsiElement(element);
      if (moduleForPsiElement == null) {
        return LanguageLevel.HIGHEST;
      }
      final JavaModuleExtension extension = ModuleUtilCore.getExtension(moduleForPsiElement, JavaModuleExtension.class);
      if (extension == null) {
        return LanguageLevel.HIGHEST;
      }
      return extension.getLanguageLevel();
    }
  }

  @Override
  public LanguageVersion getLanguageVersion(@NotNull Language language, @Nullable Project project, @Nullable VirtualFile virtualFile) {
    if (project == null || virtualFile == null) {
      return LanguageLevel.HIGHEST;
    }
    final Module moduleForPsiElement = ModuleUtilCore.findModuleForFile(virtualFile, project);
    if (moduleForPsiElement == null) {
      return LanguageLevel.HIGHEST;
    }
    final JavaModuleExtension extension = ModuleUtilCore.getExtension(moduleForPsiElement, JavaModuleExtension.class);
    if (extension == null) {
      return LanguageLevel.HIGHEST;
    }
    return extension.getLanguageLevel();
  }
}
