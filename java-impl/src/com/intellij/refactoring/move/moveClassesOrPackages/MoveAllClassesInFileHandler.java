/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.refactoring.move.moveClassesOrPackages;

import java.util.Map;

import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;

public abstract class MoveAllClassesInFileHandler
{
	public static ExtensionPointName<MoveAllClassesInFileHandler> EP_NAME =
			new ExtensionPointName<MoveAllClassesInFileHandler>("consulo.java.refactoring.moveAllClassesInFileHandler");

	public abstract void processMoveAllClassesInFile(@NotNull Map<PsiClass, Boolean> allClasses, PsiClass psiClass, PsiElement... elementsToMove);
}
