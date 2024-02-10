/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.java.impl.refactoring.move.moveClassesOrPackages;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.CommonBundle;
import com.intellij.java.impl.psi.impl.file.JavaDirectoryServiceImpl;
import com.intellij.java.impl.refactoring.JavaRefactoringSettings;
import com.intellij.java.impl.refactoring.rename.JavaVetoRenameCondition;
import com.intellij.java.impl.refactoring.util.RefactoringUtil;
import com.intellij.java.language.psi.*;
import consulo.dataContext.DataContext;
import consulo.language.editor.LangDataKeys;
import consulo.codeEditor.Editor;
import consulo.module.Module;
import consulo.language.util.ModuleUtilCore;
import consulo.project.Project;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.Messages;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.editor.refactoring.RefactoringBundle;
import consulo.language.editor.refactoring.move.MoveCallback;
import consulo.language.editor.refactoring.move.MoveHandlerDelegate;
import consulo.language.editor.refactoring.move.fileOrDirectory.MoveFilesOrDirectoriesUtil;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.language.editor.refactoring.ui.RadioUpDownListener;
import consulo.language.util.IncorrectOperationException;
import consulo.java.impl.util.JavaProjectRootsUtil;
import consulo.logging.Logger;

import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;

@ExtensionImpl(order = "before moveJavaFileOrDir")
public class JavaMoveClassesOrPackagesHandler extends MoveHandlerDelegate {
  private static final Logger LOG = Logger.getInstance(JavaMoveClassesOrPackagesHandler.class);
  private static final JavaVetoRenameCondition VETO_RENAME_CONDITION = new JavaVetoRenameCondition();

  public static boolean isPackageOrDirectory(final PsiElement element) {
    if (element instanceof PsiJavaPackage) {
      return true;
    }
    return element instanceof PsiDirectory && JavaDirectoryService.getInstance().getPackage((PsiDirectory) element) != null;
  }

  public static boolean isReferenceInAnonymousClass(@Nullable final PsiReference reference) {
    if (reference instanceof PsiJavaCodeReferenceElement && ((PsiJavaCodeReferenceElement) reference).getParent() instanceof PsiAnonymousClass) {
      return true;
    }
    return false;
  }

  @Override
  public boolean canMove(PsiElement[] elements, @Nullable PsiElement targetContainer) {
    for (PsiElement element : elements) {
      if (!isPackageOrDirectory(element) && invalid4Move(element)) {
        return false;
      }
    }
    return super.canMove(elements, targetContainer);
  }

  public static boolean invalid4Move(PsiElement element) {
    PsiFile parentFile;
    if (element instanceof PsiClassOwner) {
      final PsiClass[] classes = ((PsiClassOwner) element).getClasses();
      if (classes.length == 0) {
        return true;
      }
      for (PsiClass aClass : classes) {
        // if (aClass instanceof JspClass) return true;
      }
      parentFile = (PsiFile) element;
    } else {
      //if (element instanceof JspClass) return true;
      if (!(element instanceof PsiClass)) {
        return true;
      }
      if (element instanceof PsiAnonymousClass) {
        return true;
      }
      if (((PsiClass) element).getContainingClass() != null) {
        return true;
      }
      parentFile = element.getContainingFile();
    }
    return parentFile instanceof PsiJavaFile && JavaProjectRootsUtil.isOutsideSourceRoot(parentFile);
  }

  @Override
  public boolean isValidTarget(PsiElement psiElement, PsiElement[] sources) {
    if (isPackageOrDirectory(psiElement)) {
      return true;
    }
    boolean areAllClasses = true;
    for (PsiElement source : sources) {
      areAllClasses &= !isPackageOrDirectory(source) && !invalid4Move(source);
    }
    return areAllClasses && psiElement instanceof PsiClass;
  }

  public PsiElement[] adjustForMove(final Project project, final PsiElement[] sourceElements, final PsiElement targetElement) {
    return MoveClassesOrPackagesImpl.adjustForMove(project, sourceElements, targetElement);
  }

  public void doMove(final Project project, final PsiElement[] elements, final PsiElement targetContainer, final MoveCallback callback) {
    final PsiDirectory[] directories = new PsiDirectory[elements.length];
    final String prompt = getPromptToMoveDirectoryLibrariesSafe(elements);
    if (prompt != null) {
      System.arraycopy(elements, 0, directories, 0, directories.length);
      moveDirectoriesLibrariesSafe(project, targetContainer, callback, directories, prompt);
      return;
    }
    if (canMoveOrRearrangePackages(elements)) {
      System.arraycopy(elements, 0, directories, 0, directories.length);
      SelectMoveOrRearrangePackageDialog dialog = new SelectMoveOrRearrangePackageDialog(project, directories, targetContainer == null);
      dialog.show();
      if (!dialog.isOK()) {
        return;
      }

      if (dialog.isPackageRearrageSelected()) {
        MoveClassesOrPackagesImpl.doRearrangePackage(project, directories);
        return;
      }

      if (dialog.isMoveDirectory()) {
        moveAsDirectory(project, targetContainer, callback, directories);
        return;
      }
    }
    final PsiElement[] adjustedElements = MoveClassesOrPackagesImpl.adjustForMove(project, elements, targetContainer);
    if (adjustedElements == null) {
      return;
    }

    if (targetContainer instanceof PsiDirectory) {
      if (CommonRefactoringUtil.checkReadOnlyStatusRecursively(project, Arrays.asList(adjustedElements), true)) {
        if (!packageHasMultipleDirectoriesInModule(project, (PsiDirectory) targetContainer)) {
          new MoveClassesOrPackagesToNewDirectoryDialog((PsiDirectory) targetContainer, adjustedElements, callback).show();
          return;
        }
      }
    }
    MoveClassesOrPackagesImpl.doMove(project, adjustedElements, targetContainer, callback);
  }

  private static void moveDirectoriesLibrariesSafe(Project project, PsiElement targetContainer, MoveCallback callback, PsiDirectory[] directories, String prompt) {
    final PsiJavaPackage aPackage = JavaDirectoryService.getInstance().getPackage(directories[0]);
    LOG.assertTrue(aPackage != null);
    final PsiDirectory[] projectDirectories = aPackage.getDirectories(GlobalSearchScope.projectScope(project));
    if (projectDirectories.length > 1) {
      int ret = Messages.showYesNoCancelDialog(project, prompt + " or all directories in project?", RefactoringBundle.message("warning.title"), RefactoringBundle.message("move.current" +
          ".directory"), RefactoringBundle.message("move.directories"), CommonBundle.getCancelButtonText(), Messages.getWarningIcon());
      if (ret == 0) {
        moveAsDirectory(project, targetContainer, callback, directories);
      } else if (ret == 1) {
        moveAsDirectory(project, targetContainer, callback, projectDirectories);
      }
    } else if (Messages.showOkCancelDialog(project, prompt + "?", RefactoringBundle.message("warning.title"), Messages.getWarningIcon()) == DialogWrapper.OK_EXIT_CODE) {
      moveAsDirectory(project, targetContainer, callback, directories);
    }
  }

  private static void moveAsDirectory(Project project, PsiElement targetContainer, final MoveCallback callback, final PsiDirectory[] directories) {
    if (targetContainer instanceof PsiDirectory) {
      final JavaRefactoringSettings refactoringSettings = JavaRefactoringSettings.getInstance();
      final MoveDirectoryWithClassesProcessor processor = new MoveDirectoryWithClassesProcessor(project, directories, (PsiDirectory) targetContainer, refactoringSettings
          .RENAME_SEARCH_IN_COMMENTS_FOR_PACKAGE, refactoringSettings.RENAME_SEARCH_IN_COMMENTS_FOR_PACKAGE, true, callback);
      processor.setPrepareSuccessfulSwingThreadCallback(new Runnable() {
        @Override
        public void run() {
        }
      });
      processor.run();
    } else {
      final boolean containsJava = hasJavaFiles(directories[0]);
      if (!containsJava) {
        MoveFilesOrDirectoriesUtil.doMove(project, new PsiElement[]{directories[0]}, new PsiElement[]{targetContainer}, callback);
        return;
      }
      final MoveClassesOrPackagesToNewDirectoryDialog dlg = new MoveClassesOrPackagesToNewDirectoryDialog(directories[0], new PsiElement[0], false, callback) {
        @Override
        protected void performRefactoring(Project project, final PsiDirectory targetDirectory, PsiJavaPackage aPackage, boolean searchInComments, boolean searchForTextOccurences) {
          try {
            for (PsiDirectory dir : directories) {
              MoveFilesOrDirectoriesUtil.checkIfMoveIntoSelf(dir, targetDirectory);
            }
          } catch (IncorrectOperationException e) {
            Messages.showErrorDialog(project, e.getMessage(), RefactoringBundle.message("cannot.move"));
            return;
          }
          final MoveDirectoryWithClassesProcessor processor = new MoveDirectoryWithClassesProcessor(project, directories, targetDirectory, searchInComments, searchForTextOccurences, true,
              callback);
          processor.setPrepareSuccessfulSwingThreadCallback(new Runnable() {
            @Override
            public void run() {
            }
          });
          processor.run();
        }
      };
      dlg.show();
    }
  }

  public static boolean hasJavaFiles(PsiDirectory directory) {
    final boolean[] containsJava = new boolean[]{false};
    directory.accept(new JavaRecursiveElementWalkingVisitor() {
      @Override
      public void visitElement(PsiElement element) {
        if (containsJava[0]) {
          return;
        }
        if (element instanceof PsiDirectory) {
          super.visitElement(element);
        }
      }

      @Override
      public void visitFile(PsiFile file) {
        containsJava[0] = file instanceof PsiJavaFile;
      }
    });
    return containsJava[0];
  }

  @Override
  public PsiElement adjustTargetForMove(DataContext dataContext, PsiElement targetContainer) {
    if (targetContainer instanceof PsiJavaPackage) {
      final Module module = dataContext.getData(LangDataKeys.TARGET_MODULE);
      if (module != null) {
        final PsiDirectory[] directories = ((PsiJavaPackage) targetContainer).getDirectories(GlobalSearchScope.moduleScope(module));
        if (directories.length == 1) {
          return directories[0];
        }
      }
    }
    return super.adjustTargetForMove(dataContext, targetContainer);
  }

  public static boolean packageHasMultipleDirectoriesInModule(Project project, PsiDirectory targetElement) {
    final PsiJavaPackage psiPackage = JavaDirectoryService.getInstance().getPackage(targetElement);
    if (psiPackage != null) {
      final Module module = ModuleUtilCore.findModuleForFile(targetElement.getVirtualFile(), project);
      if (module != null) {
        if (psiPackage.getDirectories(GlobalSearchScope.moduleScope(module)).length > 1) {
          return true;
        }
      }
    }
    return false;
  }

  @Nullable
  private static String getPromptToMoveDirectoryLibrariesSafe(PsiElement[] elements) {
    if (elements.length == 0 || elements.length > 1) {
      return null;
    }
    final Project project = elements[0].getProject();
    final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    if (!(elements[0] instanceof PsiDirectory)) {
      return null;
    }
    final PsiDirectory directory = ((PsiDirectory) elements[0]);
    if (RefactoringUtil.isSourceRoot(directory)) {
      return null;
    }
    final PsiJavaPackage aPackage = JavaDirectoryService.getInstance().getPackage(directory);
    if (aPackage == null) {
      return null;
    }
    if ("".equals(aPackage.getQualifiedName())) {
      return null;
    }
    final PsiDirectory[] directories = aPackage.getDirectories();

    boolean inLib = false;
    for (PsiDirectory psiDirectory : directories) {
      inLib |= !fileIndex.isInContent(psiDirectory.getVirtualFile());
    }

    return inLib ? "Package \'" + aPackage.getName() + "\' contains directories in libraries which cannot be moved. Do you want to move current directory" : null;
  }

  private static boolean canMoveOrRearrangePackages(PsiElement[] elements) {
    if (elements.length == 0) {
      return false;
    }
    final Project project = elements[0].getProject();
    if (ProjectRootManager.getInstance(project).getContentSourceRoots().length == 1) {
      return false;
    }
    for (PsiElement element : elements) {
      if (!(element instanceof PsiDirectory)) {
        return false;
      }
      final PsiDirectory directory = ((PsiDirectory) element);
      if (RefactoringUtil.isSourceRoot(directory)) {
        return false;
      }
      final PsiJavaPackage aPackage = JavaDirectoryService.getInstance().getPackage(directory);
      if (aPackage == null) {
        return false;
      }
      if (aPackage.getQualifiedName().isEmpty()) {
        return false;
      }
      final VirtualFile sourceRootForFile = ProjectRootManager.getInstance(element.getProject()).getFileIndex().getSourceRootForFile(directory.getVirtualFile());
      if (sourceRootForFile == null) {
        return false;
      }
    }
    return true;
  }

  public static boolean hasPackages(PsiDirectory directory) {
    if (JavaDirectoryService.getInstance().getPackage(directory) != null) {
      return true;
    }
    return false;
  }

  private static class SelectMoveOrRearrangePackageDialog extends DialogWrapper {
    private JRadioButton myRbMovePackage;
    private JRadioButton myRbRearrangePackage;
    private JRadioButton myRbMoveDirectory;

    private final PsiDirectory[] myDirectories;
    private final boolean myRearrangePackagesEnabled;

    public SelectMoveOrRearrangePackageDialog(Project project, PsiDirectory[] directories) {
      this(project, directories, true);
    }

    public SelectMoveOrRearrangePackageDialog(Project project, PsiDirectory[] directories, boolean rearrangePackagesEnabled) {
      super(project, true);
      myDirectories = directories;
      myRearrangePackagesEnabled = rearrangePackagesEnabled;
      setTitle(RefactoringBundle.message("select.refactoring.title"));
      init();
    }

    protected JComponent createNorthPanel() {
      return new JLabel(RefactoringBundle.message("what.would.you.like.to.do"));
    }

    public JComponent getPreferredFocusedComponent() {
      return myRbMovePackage;
    }

    protected String getDimensionServiceKey() {
      return "#com.intellij.refactoring.move.MoveHandler.SelectRefactoringDialog";
    }


    protected JComponent createCenterPanel() {
      JPanel panel = new JPanel(new BorderLayout());


      final HashSet<String> packages = new HashSet<String>();
      for (PsiDirectory directory : myDirectories) {
        packages.add(JavaDirectoryService.getInstance().getPackage(directory).getQualifiedName());
      }
      final String moveDescription;
      LOG.assertTrue(myDirectories.length > 0);
      LOG.assertTrue(packages.size() > 0);
      if (packages.size() > 1) {
        moveDescription = RefactoringBundle.message("move.packages.to.another.package", packages.size());
      } else {
        final String qName = packages.iterator().next();
        moveDescription = RefactoringBundle.message("move.package.to.another.package", qName);
      }

      myRbMovePackage = new JRadioButton();
      myRbMovePackage.setText(moveDescription);
      myRbMovePackage.setSelected(true);

      final String rearrangeDescription;
      if (myDirectories.length > 1) {
        rearrangeDescription = RefactoringBundle.message("move.directories.to.another.source.root", myDirectories.length);
      } else {
        rearrangeDescription = RefactoringBundle.message("move.directory.to.another.source.root", myDirectories[0].getVirtualFile().getPresentableUrl());
      }
      myRbRearrangePackage = new JRadioButton();
      myRbRearrangePackage.setText(rearrangeDescription);
      myRbRearrangePackage.setVisible(myRearrangePackagesEnabled);

      final String moveDirectoryDescription;
      if (myDirectories.length > 1) {
        moveDirectoryDescription = "Move everything from " + myDirectories.length + " directories to another directory";
      } else {
        moveDirectoryDescription = "Move everything from " + myDirectories[0].getVirtualFile().getPresentableUrl() + " to another directory";
      }
      myRbMoveDirectory = new JRadioButton();
      myRbMoveDirectory.setMnemonic('e');
      myRbMoveDirectory.setText(moveDirectoryDescription);

      ButtonGroup gr = new ButtonGroup();
      gr.add(myRbMovePackage);
      gr.add(myRbRearrangePackage);
      gr.add(myRbMoveDirectory);

      if (myRearrangePackagesEnabled) {
        new RadioUpDownListener(myRbMovePackage, myRbRearrangePackage, myRbMoveDirectory);
      } else {
        new RadioUpDownListener(myRbMovePackage, myRbMoveDirectory);
      }

      Box box = Box.createVerticalBox();
      box.add(Box.createVerticalStrut(5));
      box.add(myRbMovePackage);
      box.add(myRbRearrangePackage);
      box.add(myRbMoveDirectory);
      panel.add(box, BorderLayout.CENTER);
      return panel;
    }

    public boolean isPackageRearrageSelected() {
      return myRbRearrangePackage.isSelected();
    }

    public boolean isMoveDirectory() {
      return myRbMoveDirectory.isSelected();
    }
  }


  public boolean tryToMove(final PsiElement element, final Project project, final DataContext dataContext, final PsiReference reference, final Editor editor) {
    if (isPackageOrDirectory(element)) {
      return false;
    }
    if (isReferenceInAnonymousClass(reference)) {
      return false;
    }

    if (!invalid4Move(element)) {
      final PsiElement initialTargetElement = dataContext.getData(LangDataKeys.TARGET_PSI_ELEMENT);
      PsiElement[] adjustedElements = adjustForMove(project, new PsiElement[]{element}, initialTargetElement);
      if (adjustedElements == null) {
        return true;
      }
      MoveClassesOrPackagesImpl.doMove(project, adjustedElements, initialTargetElement, null);
      return true;
    }
    return false;
  }

  @Override
  public boolean isMoveRedundant(PsiElement source, PsiElement target) {
    if (target instanceof PsiDirectory && source instanceof PsiClass) {
      try {
        JavaDirectoryServiceImpl.checkCreateClassOrInterface((PsiDirectory) target, ((PsiClass) source).getName());
      } catch (IncorrectOperationException e) {
        return true;
      }
    }
    if (target instanceof PsiDirectory && source instanceof PsiDirectory) {
      final PsiJavaPackage aPackage = JavaDirectoryServiceImpl.getInstance().getPackage((PsiDirectory) source);
      if (aPackage != null && !MoveClassesOrPackagesImpl.checkNesting(target.getProject(), aPackage, target, false)) {
        return true;
      }
    }
    return super.isMoveRedundant(source, target);
  }
}
