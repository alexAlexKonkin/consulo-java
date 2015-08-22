/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.psi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;

/**
 * Created by Max Medvedev on 10/25/13
 */
public interface ClassTypePointerFactory
{
	ExtensionPointName<ClassTypePointerFactory> EP_NAME = ExtensionPointName.create("org.consulo.java" +
			".classTypePointerFactory");

	@Nullable
	SmartTypePointer createClassTypePointer(@NotNull PsiClassType classType, @NotNull Project project);
}