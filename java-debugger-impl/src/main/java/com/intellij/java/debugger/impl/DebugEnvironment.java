/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.java.debugger.impl;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.java.execution.configurations.RemoteConnection;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.psi.search.GlobalSearchScope;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface DebugEnvironment
{

	int LOCAL_START_TIMEOUT = 30000;

	@Nullable
	ExecutionResult createExecutionResult() throws ExecutionException;

	@Nonnull
	GlobalSearchScope getSearchScope();

	@Nullable
	default Sdk getAlternativeJre()
	{
		return null;
	}

	@Nullable
	default Sdk getRunJre()
	{
		return null;
	}

	boolean isRemote();

	RemoteConnection getRemoteConnection();

	long getPollTimeout();

	String getSessionName();
}
