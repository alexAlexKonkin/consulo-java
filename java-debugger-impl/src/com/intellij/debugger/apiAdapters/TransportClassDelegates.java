/*
 * Copyright 2013-2015 must-be.org
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

package com.intellij.debugger.apiAdapters;

import org.consulo.lombok.annotations.LazyInstance;

/**
 * @author VISTALL
 * @since 31.05.2015
 */
public class TransportClassDelegates
{
	@LazyInstance
	public static Class<?> getSocketTransportServiceClass()
	{
		try
		{
			return Class.forName("consulo.internal.com.sun.tools.jdi.SocketTransportService");
		}
		catch(ClassNotFoundException e)
		{
			throw new Error(e);
		}
	}

	@LazyInstance(notNull = false)
	public static Class<?> getSharedMemoryTransportServiceClass()
	{
		try
		{
			return Class.forName("consulo.internal.com.sun.tools.jdi.SharedMemoryTransportService");
		}
		catch(Throwable e)
		{
			return null;
		}
	}
}