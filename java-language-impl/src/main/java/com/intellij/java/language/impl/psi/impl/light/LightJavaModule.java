// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.java.language.impl.psi.impl.light;

import com.intellij.java.language.JavaLanguage;
import com.intellij.java.language.psi.*;
import com.intellij.java.language.psi.javadoc.PsiDocComment;
import com.intellij.java.language.psi.util.PsiUtil;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.ItemPresentationProviders;
import com.intellij.openapi.util.AtomicNotNullLazyValue;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.light.LightElement;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.psi.PsiPackage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.intellij.util.ObjectUtils.notNull;

public final class LightJavaModule extends LightElement implements PsiJavaModule {
  private final LightJavaModuleReferenceElement myRefElement;
  private final VirtualFile myRoot;
  private final NotNullLazyValue<List<PsiPackageAccessibilityStatement>> myExports = AtomicNotNullLazyValue.createValue(() -> findExports());

  private LightJavaModule(@Nonnull PsiManager manager, @Nonnull VirtualFile root, @Nonnull String name) {
    super(manager, JavaLanguage.INSTANCE);
    myRoot = root;
    myRefElement = new LightJavaModuleReferenceElement(manager, name);
  }

  public
  @Nonnull
  VirtualFile getRootVirtualFile() {
    return myRoot;
  }

  @Override
  public
  @Nullable
  PsiDocComment getDocComment() {
    return null;
  }

  @Override
  public
  @Nonnull
  Iterable<PsiRequiresStatement> getRequires() {
    return Collections.emptyList();
  }

  @Override
  public
  @Nonnull
  Iterable<PsiPackageAccessibilityStatement> getExports() {
    return myExports.getValue();
  }

  private List<PsiPackageAccessibilityStatement> findExports() {
    List<PsiPackageAccessibilityStatement> exports = new ArrayList<>();

    VfsUtilCore.visitChildrenRecursively(myRoot, new VirtualFileVisitor<Void>() {
      private final JavaDirectoryService service = JavaDirectoryService.getInstance();

      @Override
      public boolean visitFile(@Nonnull VirtualFile file) {
        if (file.isDirectory() && !myRoot.equals(file)) {
          PsiDirectory directory = getManager().findDirectory(file);
          if (directory != null) {
            PsiPackage pkg = service.getPackage(directory);
            if (pkg != null) {
              String packageName = pkg.getQualifiedName();
              if (!packageName.isEmpty() && !PsiUtil.isPackageEmpty(new PsiDirectory[]{directory}, packageName)) {
                exports.add(new LightPackageAccessibilityStatement(getManager(), packageName));
              }
            }
          }
        }
        return true;
      }
    });

    return exports;
  }

  @Override
  public
  @Nonnull
  Iterable<PsiPackageAccessibilityStatement> getOpens() {
    return Collections.emptyList();
  }

  @Override
  public
  @Nonnull
  Iterable<PsiUsesStatement> getUses() {
    return Collections.emptyList();
  }

  @Override
  public
  @Nonnull
  Iterable<PsiProvidesStatement> getProvides() {
    return Collections.emptyList();
  }

  @Override
  public
  @Nonnull
  PsiJavaModuleReferenceElement getNameIdentifier() {
    return myRefElement;
  }

  @Override
  public
  @Nonnull
  String getName() {
    return myRefElement.getReferenceText();
  }

  @Override
  public PsiElement setName(@Nonnull String name) throws IncorrectOperationException {
    throw new IncorrectOperationException("Cannot modify an automatic module '" + getName() + "'");
  }

  @Override
  public PsiModifierList getModifierList() {
    return null;
  }

  @Override
  public boolean hasModifierProperty(@Nonnull String name) {
    return false;
  }

  @Override
  public ItemPresentation getPresentation() {
    return ItemPresentationProviders.getItemPresentation(this);
  }

  @Override
  public
  @Nonnull
  PsiElement getNavigationElement() {
    return notNull(myManager.findDirectory(myRoot), super.getNavigationElement());
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof LightJavaModule && myRoot.equals(((LightJavaModule) obj).myRoot) && getManager() == ((LightJavaModule) obj).getManager();
  }

  @Override
  public int hashCode() {
    return getName().hashCode() * 31 + getManager().hashCode();
  }

  @Override
  public String toString() {
    return "PsiJavaModule:" + getName();
  }

  private static final class LightJavaModuleReferenceElement extends LightElement implements PsiJavaModuleReferenceElement {
    private final String myText;

    private LightJavaModuleReferenceElement(@Nonnull PsiManager manager, @Nonnull String text) {
      super(manager, JavaLanguage.INSTANCE);
      myText = text;
    }

    @Override
    public
    @Nonnull
    String getReferenceText() {
      return myText;
    }

    @Override
    public PsiJavaModuleReference getReference() {
      return null;
    }

    @Override
    public String toString() {
      return "PsiJavaModuleReference";
    }
  }

  private static class LightPackageAccessibilityStatement extends LightElement implements PsiPackageAccessibilityStatement {
    private final String myPackageName;

    LightPackageAccessibilityStatement(@Nonnull PsiManager manager, @Nonnull String packageName) {
      super(manager, JavaLanguage.INSTANCE);
      myPackageName = packageName;
    }

    @Override
    public
    @Nonnull
    Role getRole() {
      return Role.EXPORTS;
    }

    @Override
    public
    @Nullable
    PsiJavaCodeReferenceElement getPackageReference() {
      return null;
    }

    @Override
    public
    @Nullable
    String getPackageName() {
      return myPackageName;
    }

    @Override
    public
    @Nonnull
    Iterable<PsiJavaModuleReferenceElement> getModuleReferences() {
      return Collections.emptyList();
    }

    @Override
    public
    @Nonnull
    List<String> getModuleNames() {
      return Collections.emptyList();
    }

    @Override
    public String toString() {
      return "PsiPackageAccessibilityStatement";
    }
  }

  /**
   * @deprecated method scope was extended, use {@link #findModule} instead
   */
  @Deprecated
  @Nonnull
  public static LightJavaModule getModule(@Nonnull PsiManager manager, @Nonnull VirtualFile root) {
    LightJavaModule module = findModule(manager, root);
    assert module != null : root;
    return module;
  }

  /**
   * The method is expected to be called on roots obtained from JavaAutoModuleNameIndex/JavaSourceModuleNameIndex
   */
  @Nullable
  public static LightJavaModule findModule(@Nonnull PsiManager manager, @Nonnull VirtualFile root) {
    PsiElement directory = manager.findDirectory(root);
    if (directory == null) {
      return null;
    }
    if (root.isInLocalFileSystem()) {
      return CachedValuesManager.getCachedValue(directory, () -> {
        VirtualFile manifest = root.findFileByRelativePath(JarFile.MANIFEST_NAME);
        if (manifest != null) {
          PsiElement file = manager.findFile(manifest);
          if (file != null) {
            String name = claimedModuleName(manifest);
            LightJavaModule module = name != null ? new LightJavaModule(manager, root, name) : null;
            return CachedValueProvider.Result.create(module, file);
          }
        }
        return CachedValueProvider.Result.create(null, PsiModificationTracker.MODIFICATION_COUNT);
      });
    } else {
      return CachedValuesManager.getCachedValue(directory, () -> {
        LightJavaModule module = new LightJavaModule(manager, root, moduleName(root));
        return CachedValueProvider.Result.create(module, directory);
      });
    }
  }

  @Nonnull
  public static String moduleName(@Nonnull VirtualFile jarRoot) {
    VirtualFile manifest = jarRoot.findFileByRelativePath(JarFile.MANIFEST_NAME);
    if (manifest != null) {
      String claimed = claimedModuleName(manifest);
      if (claimed != null) {
        return claimed;
      }
    }

    return moduleName(jarRoot.getNameWithoutExtension());
  }

  public static
  @Nullable
  String claimedModuleName(@Nonnull VirtualFile manifest) {
    try (InputStream stream = manifest.getInputStream()) {
      return new Manifest(stream).getMainAttributes().getValue(PsiJavaModule.AUTO_MODULE_NAME);
    } catch (IOException e) {
      Logger.getInstance(LightJavaModule.class).warn(manifest.getPath(), e);
      return null;
    }
  }

  /**
   * <p>Implements a name deriving for automatic modules as described in ModuleFinder.of(Path...) method documentation.</p>
   *
   * <p>Please note that the result may not be a valid module name when the source contains a sequence that starts with a digit
   * (e.g. "org.7gnomes..."). One may validate the result with {@link PsiNameHelper#isValidModuleName}.</p>
   *
   * @param name a .jar file name without extension
   * @see <a href="http://docs.oracle.com/javase/9/docs/api/java/lang/module/ModuleFinder.html#of-java.nio.file.Path...-">ModuleFinder.of(Path...)</a>
   */
  @Nonnull
  public static String moduleName(@Nonnull String name) {
    // If the name matches the regular expression "-(\\d+(\\.|$))" then the module name will be derived from the sub-sequence
    // preceding the hyphen of the first occurrence.
    Matcher m = Patterns.VERSION.matcher(name);
    if (m.find()) {
      name = name.substring(0, m.start());
    }

    // All non-alphanumeric characters ([^A-Za-z0-9]) are replaced with a dot (".") ...
    name = Patterns.NON_NAME.matcher(name).replaceAll(".");
    // ... all repeating dots are replaced with one dot ...
    name = Patterns.DOT_SEQUENCE.matcher(name).replaceAll(".");
    // ... and all leading and trailing dots are removed.
    name = StringUtil.trimLeading(StringUtil.trimTrailing(name, '.'), '.');

    return name;
  }

  private static class Patterns {
    private static final Pattern VERSION = Pattern.compile("-(\\d+(\\.|$))");
    private static final Pattern NON_NAME = Pattern.compile("[^A-Za-z0-9]");
    private static final Pattern DOT_SEQUENCE = Pattern.compile("\\.{2,}");
  }
}
