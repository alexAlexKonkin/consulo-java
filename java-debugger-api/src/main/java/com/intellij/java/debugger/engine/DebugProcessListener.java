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
package com.intellij.java.debugger.engine;

import java.util.EventListener;

import consulo.execution.configuration.RunProfileState;
import consulo.process.ExecutionException;
import com.intellij.java.execution.configurations.RemoteConnection;
import consulo.internal.com.sun.jdi.ThreadReference;

public interface DebugProcessListener extends EventListener
{
	//executed in manager thread
	default void connectorIsReady()
	{
	}

	//executed in manager thread
	default void paused(SuspendContext suspendContext)
	{
	}

	//executed in manager thread
	default void resumed(SuspendContext suspendContext)
	{
	}

	//executed in manager thread
	default void processDetached(DebugProcess process, boolean closedByUser)
	{
	}

	//executed in manager thread
	default void processAttached(DebugProcess process)
	{
	}

	default void attachException(RunProfileState state, ExecutionException exception, RemoteConnection remoteConnection)
	{
	}

	default void threadStarted(DebugProcess proc, ThreadReference thread)
	{
	}

	default void threadStopped(DebugProcess proc, ThreadReference thread)
	{
	}
}

