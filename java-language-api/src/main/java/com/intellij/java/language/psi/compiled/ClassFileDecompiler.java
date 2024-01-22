package com.intellij.java.language.psi.compiled;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.language.file.FileViewProvider;
import consulo.language.psi.PsiManager;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

/**
 * Actual implementations should extend either {@link ClassFileDecompiler.Light} or {@link ClassFileDecompiler.Full} classes -
 * those that don't are silently ignored.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ClassFileDecompiler {
  /**
   * <p>"Light" decompilers are intended for augmenting file text constructed by standard IDEA decompiler
   * without changing it's structure - i.e. providing additional information in comments,
   * or replacing standard "compiled code" method body comment with something more meaningful.</p>
   * <p/>
   * <p>Plugins registering extension of this type normally should accept all files and use {@code order="last"}
   * attribute to avoid interfering with other decompilers.</p>
   */
  public abstract static class Light implements ClassFileDecompiler {
    @jakarta.annotation.Nonnull
    public abstract CharSequence getText(@jakarta.annotation.Nonnull VirtualFile file);
  }


  /**
   * <p>"Full" decompilers are designed to provide extended support for languages significantly different from Java.
   * Extensions of this type should take care of building file stubs and properly indexing them -
   * in return they have an ability to represent decompiled file in a way natural for original language.</p>
   */
  public abstract static class Full implements ClassFileDecompiler {
    @jakarta.annotation.Nonnull
    public abstract ClsStubBuilder getStubBuilder();

    /**
     * <h5>Notes for implementers</h5>
     * <p/>
     * <p>1. Return a correct language from {@link FileViewProvider#getBaseLanguage()}.</p>
     * <p/>
     * <p>2. This method is called for both PSI file construction and obtaining document text.
     * In the latter case the PsiManager is based on default project, and the only method called
     * on a resulting view provider is {@link FileViewProvider#getContents()}.</p>
     * <p/>
     * <p>3. A language compiler may produce auxiliary .class files which should be handled as part of their parent classes.
     * A standard practice is to hide such files by returning {@code null} from
     * {@link FileViewProvider#getPsi(Language)}.</p>
     */
    @jakarta.annotation.Nonnull
    public abstract FileViewProvider createFileViewProvider(@jakarta.annotation.Nonnull VirtualFile file, @jakarta.annotation.Nonnull PsiManager manager, boolean physical);
  }

  boolean accepts(@Nonnull VirtualFile file);
}
