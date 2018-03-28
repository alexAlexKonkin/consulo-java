package com.intellij.refactoring.move.moveClassesOrPackages;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.light.LightClass;
import com.intellij.util.ArrayUtil;
import javax.annotation.Nonnull;

import java.util.Map;

public class MoveJavaClassesInFileHandler extends MoveAllClassesInFileHandler {

  @Override
  public void processMoveAllClassesInFile(@Nonnull Map<PsiClass, Boolean> allClasses, PsiClass psiClass, PsiElement... elementsToMove) {
    if (psiClass instanceof LightClass) return;
    final PsiClassOwner containingFile = (PsiClassOwner)psiClass.getContainingFile();
    final PsiClass[] classes = containingFile.getClasses();
    boolean all = true;
    for (PsiClass aClass : classes) {
      if (ArrayUtil.find(elementsToMove, aClass) == -1) {
        all = false;
        break;
      }
    }
    for (PsiClass aClass : classes) {
      allClasses.put(aClass, all);
    }
  }
}
