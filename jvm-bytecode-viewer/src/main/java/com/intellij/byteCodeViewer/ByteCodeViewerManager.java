package com.intellij.byteCodeViewer;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import consulo.internal.org.objectweb.asm.ClassReader;
import consulo.internal.org.objectweb.asm.util.Textifier;
import consulo.internal.org.objectweb.asm.util.TraceClassVisitor;
import com.intellij.codeInsight.documentation.DockablePopupManager;
import com.intellij.debugger.engine.JVMNameUtil;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.presentation.java.SymbolPresentationUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.content.Content;
import consulo.compiler.ModuleCompilerPathsManager;
import consulo.roots.impl.ProductionContentFolderTypeProvider;
import consulo.roots.impl.TestContentFolderTypeProvider;

/**
 * User: anna
 * Date: 5/7/12
 */
@Singleton
public class ByteCodeViewerManager extends DockablePopupManager<ByteCodeViewerComponent> {
  private static final Logger LOG = Logger.getInstance("#" + ByteCodeViewerManager.class.getName());

  public static final String TOOLWINDOW_ID = "Byte Code Viewer";
  private static final String SHOW_BYTECODE_IN_TOOL_WINDOW = "BYTE_CODE_TOOL_WINDOW";
  private static final String BYTECODE_AUTO_UPDATE_ENABLED = "BYTE_CODE_AUTO_UPDATE_ENABLED";
  
  public static ByteCodeViewerManager getInstance(Project project) {
    return ServiceManager.getService(project, ByteCodeViewerManager.class);
  }

  @Inject
  public ByteCodeViewerManager(Project project) {
    super(project);
  }

  @Override
  public String getShowInToolWindowProperty() {
    return SHOW_BYTECODE_IN_TOOL_WINDOW;
  }

  @Override
  public String getAutoUpdateEnabledProperty() {
    return BYTECODE_AUTO_UPDATE_ENABLED;
  }

  @Override
  protected String getToolwindowId() {
    return TOOLWINDOW_ID;
  }

  @Override
  protected String getAutoUpdateTitle() {
    return "Auto Show Byte Code for Selected Element";
  }

  @Override
  protected String getAutoUpdateDescription() {
    return "Show byte code for current element automatically";
  }

  @Override
  protected String getRestorePopupDescription() {
    return "Restore byte code popup behavior";
  }

  @Override
  protected ByteCodeViewerComponent createComponent() {
    return new ByteCodeViewerComponent(myProject, createActions());
  }

  @javax.annotation.Nullable
  protected String getTitle(PsiElement element) {
    PsiClass aClass = getContainingClass(element);
    if (aClass == null) return null;
    return SymbolPresentationUtil.getSymbolPresentableText(aClass);
  }

  private void updateByteCode(PsiElement element, ByteCodeViewerComponent component, Content content) {
    updateByteCode(element, component, content, getByteCode(element));
  }

  public void updateByteCode(PsiElement element,
                             ByteCodeViewerComponent component,
                             Content content,
                             final String byteCode) {
    if (!StringUtil.isEmpty(byteCode)) {
      component.setText(byteCode, element);
    } else {
      PsiClass containingClass = getContainingClass(element);
      PsiFile containingFile = element.getContainingFile();
      component.setText("No bytecode found for " + SymbolPresentationUtil.getSymbolPresentableText(containingClass != null ? containingClass : containingFile));
    }
    content.setDisplayName(getTitle(element));
  }

  @Override
  protected void doUpdateComponent(PsiElement element, PsiElement originalElement, ByteCodeViewerComponent component) {
    final Content content = myToolWindow.getContentManager().getSelectedContent();
    if (content != null && element != null) {
      updateByteCode(element, component, content);
    }
  }

  
  @Override
  protected void doUpdateComponent(Editor editor, PsiFile psiFile) {
    final Content content = myToolWindow.getContentManager().getSelectedContent();
    if (content != null) {
      final ByteCodeViewerComponent component = (ByteCodeViewerComponent)content.getComponent();
      PsiElement element = psiFile.findElementAt(editor.getCaretModel().getOffset());
      if (element != null) {
        updateByteCode(element, component, content);
      }
    }
  }

  @Override
  protected void doUpdateComponent(@Nonnull PsiElement element) {
    doUpdateComponent(element, getByteCode(element));
  }

  protected void doUpdateComponent(@Nonnull PsiElement element, final String newText) {
    final Content content = myToolWindow.getContentManager().getSelectedContent();
    if (content != null) {
      updateByteCode(element, (ByteCodeViewerComponent)content.getComponent(), content, newText);
    }
  }

  @Nullable
  public static String getByteCode(@Nonnull PsiElement psiElement) {
    PsiClass containingClass = getContainingClass(psiElement);
    //todo show popup
    if (containingClass == null) return null;
    final String classVMName = JVMNameUtil.getClassVMName(containingClass);
    if (classVMName == null) return null;

    Module module = ModuleUtilCore.findModuleForPsiElement(psiElement);
    if (module == null){
      final Project project = containingClass.getProject();
      final PsiClass aClass = JavaPsiFacade.getInstance(project).findClass(classVMName, psiElement.getResolveScope());
      if (aClass != null) {
        final VirtualFile virtualFile = PsiUtilCore.getVirtualFile(aClass);
        if (virtualFile != null && ProjectRootManager.getInstance(project).getFileIndex().isInLibraryClasses(virtualFile)) {
          try {
            return processClassFile(virtualFile.contentsToByteArray());
          }
          catch (IOException e) {
            LOG.error(e);
          }
          return null;
        }
      }
      return null;
    }

    try {
      final PsiFile containingFile = containingClass.getContainingFile();
      final VirtualFile virtualFile = containingFile.getVirtualFile();
      if (virtualFile == null) return null;
      final ModuleCompilerPathsManager compilerPathsManager = ModuleCompilerPathsManager.getInstance(module);
      String classPath;
      if (ProjectRootManager.getInstance(module.getProject()).getFileIndex().isInTestSourceContent(virtualFile)) {
        final VirtualFile pathForTests = compilerPathsManager.getCompilerOutput(TestContentFolderTypeProvider.getInstance());
        if (pathForTests == null) return null;
        classPath = pathForTests.getPath();
      } else {
        final VirtualFile compilerOutputPath = compilerPathsManager.getCompilerOutput(ProductionContentFolderTypeProvider.getInstance());
        if (compilerOutputPath == null) return null;
        classPath = compilerOutputPath.getPath();
      }

      classPath += "/" + classVMName.replace('.', '/') + ".class";

      final File classFile = new File(classPath);
      if (!classFile.exists()) {
        LOG.info("search in: " + classPath);
        return null;
      }
      return processClassFile(FileUtil.loadFileBytes(classFile));
    }
    catch (Exception e1) {
      LOG.error(e1);
    }
    return null;
  }

  private static String processClassFile(byte[] bytes) {
    final ClassReader classReader = new ClassReader(bytes);
    final StringWriter writer = new StringWriter();
    final PrintWriter printWriter = new PrintWriter(writer);
    try {
      classReader.accept(new TraceClassVisitor(null, new Textifier(), printWriter), 0);
    }
    finally {
      printWriter.close();
    }
    return writer.toString();
  }

  public static PsiClass getContainingClass(PsiElement psiElement) {
    PsiClass containingClass = PsiTreeUtil.getParentOfType(psiElement, PsiClass.class, false);
    if (containingClass == null) return null;

    return containingClass;
  }
}
