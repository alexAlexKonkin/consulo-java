/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package com.intellij.psi.impl.java.stubs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.impl.cache.TypeInfo;

/**
 * @author max
 */
public interface PsiMethodStub extends PsiMemberStub<PsiMethod>
{
	boolean isConstructor();

	boolean isVarArgs();

	boolean isAnnotationMethod();

	@Nullable
	String getDefaultValueText();

	@NotNull
	TypeInfo getReturnTypeText(boolean doResolve);

	boolean isDeprecated();

	boolean hasDeprecatedAnnotation();

	boolean hasDocComment();

	PsiParameterStub findParameter(int idx);
}