package com.intellij.java.coverage;

import com.intellij.java.language.psi.PsiClass;
import consulo.component.extension.ExtensionPointName;
import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.coverage.CoverageSuitesBundle;
import consulo.language.psi.PsiFile;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.Set;

/**
 * User: anna
 * Date: 2/14/11
 */
public abstract class JavaCoverageEngineExtension {
  public static final ExtensionPointName<JavaCoverageEngineExtension> EP_NAME =
      ExtensionPointName.create("consulo.java.coverageEngineExtension");

  public abstract boolean isApplicableTo(@Nullable RunConfigurationBase conf);

  public boolean suggestQualifiedName(@Nonnull PsiFile sourceFile, PsiClass[] classes, Set<String> names) {
    return false;
  }

  public boolean collectOutputFiles(@Nonnull final PsiFile srcFile, @Nullable final VirtualFile output, @Nullable final VirtualFile testoutput,
                                    @Nonnull final CoverageSuitesBundle suite, @Nonnull final Set<File> classFiles) {
    return false;
  }
}
