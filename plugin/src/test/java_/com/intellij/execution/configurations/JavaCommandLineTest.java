/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.execution.configurations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import consulo.process.cmd.GeneralCommandLine;
import org.junit.Assert;
import com.intellij.JavaTestUtil;
import consulo.execution.CantRunException;
import consulo.execution.ExecutionBundle;
import consulo.process.ExecutionException;
import consulo.process.internal.OSProcessHandler;
import consulo.ide.IdeBundle;
import consulo.content.bundle.Sdk;
import com.intellij.testFramework.LightIdeaTestCase;
import consulo.java.execution.configurations.OwnJavaParameters;

public abstract class JavaCommandLineTest extends LightIdeaTestCase
{
	public void testJdk()
	{
		try
		{
			new OwnJavaParameters().toCommandLine();
			fail("CantRunException (main class is not specified) expected");
		}
		catch(CantRunException e)
		{
			Assert.assertEquals(ExecutionBundle.message("run.configuration.error.no.jdk.specified"), e.getMessage());
		}
	}

	public void testMainClass()
	{
		try
		{
			OwnJavaParameters javaParameters = new OwnJavaParameters();
			javaParameters.setJdk(getProjectJDK());
			javaParameters.toCommandLine();
			fail("CantRunException (main class is not specified) expected");
		}
		catch(CantRunException e)
		{
			assertEquals(ExecutionBundle.message("main.class.is.not.specified.error.message"), e.getMessage());
		}
	}

	public void testClasspath() throws CantRunException
	{
		OwnJavaParameters javaParameters;
		String commandLineString;

		javaParameters = new OwnJavaParameters();
		final Sdk internalJdk = JavaTestUtil.getTestJdk();
		javaParameters.setJdk(internalJdk);
		javaParameters.setMainClass("Main");
		commandLineString =javaParameters.toCommandLine().getCommandLineString();
		assertTrue(containsClassPath(commandLineString));

		javaParameters = new OwnJavaParameters();
		javaParameters.setJdk(internalJdk);
		javaParameters.setMainClass("Main");
		javaParameters.getVMParametersList().add("-cp");
		javaParameters.getVMParametersList().add("..");
		commandLineString = javaParameters.toCommandLine().getCommandLineString();
		commandLineString = removeClassPath(commandLineString, "-cp ..");
		assertTrue(!containsClassPath(commandLineString));

		javaParameters = new OwnJavaParameters();
		javaParameters.setJdk(internalJdk);
		javaParameters.setMainClass("Main");
		javaParameters.getVMParametersList().add("-classpath");
		javaParameters.getVMParametersList().add("..");
		commandLineString = javaParameters.toCommandLine().getCommandLineString();
		commandLineString = removeClassPath(commandLineString, "-classpath ..");
		assertTrue(!containsClassPath(commandLineString));
	}

	private static boolean containsClassPath(String commandLineString)
	{
		return commandLineString.contains("-cp") || commandLineString.contains("-classpath");
	}

	private static String removeClassPath(String commandLineString, String pathString)
	{
		int i = commandLineString.indexOf(pathString);
		commandLineString = commandLineString.substring(0, i) + commandLineString.substring(i + pathString.length());
		return commandLineString;
	}

	public void testCreateProcess()
	{
		try
		{
			new OSProcessHandler(new GeneralCommandLine());
			fail("ExecutionException (executable is not specified) expected");
		}
		catch(ExecutionException e)
		{
			assertEquals(IdeBundle.message("run.configuration.error.executable.not.specified"), e.getMessage());
		}
	}
}
