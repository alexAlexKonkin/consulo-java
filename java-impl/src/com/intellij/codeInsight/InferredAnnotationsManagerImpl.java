/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.codeInsight;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.codeInspection.bytecodeAnalysis.ProjectBytecodeAnalysis;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiModifierListOwner;

public class InferredAnnotationsManagerImpl extends InferredAnnotationsManager
{
	@Nullable
	@Override
	public PsiAnnotation findInferredAnnotation(@NotNull PsiModifierListOwner listOwner, @NotNull String annotationFQN)
	{
		return ProjectBytecodeAnalysis.getInstance(listOwner.getProject()).findInferredAnnotation(listOwner, annotationFQN);
	}

	@NotNull
	@Override
	public PsiAnnotation[] findInferredAnnotations(@NotNull PsiModifierListOwner listOwner)
	{
		return ProjectBytecodeAnalysis.getInstance(listOwner.getProject()).findInferredAnnotations(listOwner);
	}

	@Override
	public boolean isInferredAnnotation(@NotNull PsiAnnotation annotation)
	{
		return annotation.getUserData(ProjectBytecodeAnalysis.INFERRED_ANNOTATION) != null;
	}
}
