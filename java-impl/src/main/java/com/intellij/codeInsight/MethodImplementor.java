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
package com.intellij.codeInsight;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.codeInsight.generation.GenerationInfo;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.util.Consumer;
import com.intellij.util.IncorrectOperationException;

/**
 * @author peter
 */
public interface MethodImplementor extends MemberImplementorExplorer
{
	ExtensionPointName<MethodImplementor> EXTENSION_POINT_NAME = ExtensionPointName.create("consulo.java.methodImplementor");

	@NotNull
	PsiMethod[] createImplementationPrototypes(final PsiClass inClass, PsiMethod method) throws IncorrectOperationException;

	@Nullable
	GenerationInfo createGenerationInfo(PsiMethod method, boolean mergeIfExists);

	@NotNull
	Consumer<PsiMethod> createDecorator(PsiClass targetClass, PsiMethod baseMethod, boolean toCopyJavaDoc, boolean insertOverrideIfPossible);
}