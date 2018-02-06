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
package com.intellij.execution;

import org.jetbrains.annotations.Nullable;
import com.intellij.execution.configurations.ConfigurationWithAlternativeJre;

public interface CommonJavaRunConfigurationParameters extends CommonProgramRunConfigurationParameters, ConfigurationWithAlternativeJre
{
	void setVMParameters(String value);

	String getVMParameters();

	@Override
	boolean isAlternativeJrePathEnabled();

	void setAlternativeJrePathEnabled(boolean enabled);

	@Override
	String getAlternativeJrePath();

	void setAlternativeJrePath(String path);

	@Nullable
	String getRunClass();

	@Nullable
	String getPackage();
}