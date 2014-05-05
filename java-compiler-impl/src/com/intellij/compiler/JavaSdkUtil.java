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
package com.intellij.compiler;

import org.consulo.java.module.extension.JavaModuleExtension;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.compiler.impl.ModuleChunk;
import com.intellij.openapi.module.EffectiveLanguageLevelUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.rt.compiler.JavacRunner;
import com.intellij.util.PathUtil;
import com.intellij.util.PathsList;

public class JavaSdkUtil
{
	@NonNls
	public static final String IDEA_PREPEND_RTJAR = "idea.prepend.rtjar";

	public static void addRtJar(PathsList pathsList)
	{
		final String ideaRtJarPath = getIdeaRtJarPath();
		if(Boolean.getBoolean(IDEA_PREPEND_RTJAR))
		{
			pathsList.addFirst(ideaRtJarPath);
		}
		else
		{
			pathsList.addTail(ideaRtJarPath);
		}
	}


	public static String getJunit4JarPath()
	{
		try
		{
			return PathUtil.getJarPathForClass(Class.forName("org.junit.Test"));
		}
		catch(ClassNotFoundException e)
		{
			throw new RuntimeException(e);
		}
	}

	public static String getJunit3JarPath()
	{
		try
		{
			return PathUtil.getJarPathForClass(Class.forName("junit.runner.TestSuiteLoader")); //junit3 specific class
		}
		catch(ClassNotFoundException e)
		{
			throw new RuntimeException(e);
		}
	}

	public static String getIdeaRtJarPath()
	{
		return PathUtil.getJarPathForClass(JavacRunner.class);
	}

	@Nullable
	public static Sdk getSdkForCompilation(@NotNull final Module module)
	{
		JavaModuleExtension extension = ModuleUtilCore.getExtension(module, JavaModuleExtension.class);
		if(extension == null)
		{
			return null;
		}
		return extension.getSdkForCompilation();
	}

	@Nullable
	public static Sdk getSdkForCompilation(final ModuleChunk chunk)
	{
		return getSdkForCompilation(chunk.getModule());
	}

	@Nullable
	public static String getCompilationClasspath(final ModuleChunk moduleChunk)
	{
		JavaModuleExtension extension = ModuleUtilCore.getExtension(moduleChunk.getModule(), JavaModuleExtension.class);
		if(extension == null)
		{
			return null;
		}
		return extension.getCompilationClasspath(moduleChunk);
	}

	@Nullable
	public static String getCompilationBootClasspath(final ModuleChunk moduleChunk)
	{
		JavaModuleExtension extension = ModuleUtilCore.getExtension(moduleChunk.getModule(), JavaModuleExtension.class);
		if(extension == null)
		{
			return null;
		}
		return extension.getCompilationBootClasspath(moduleChunk);
	}

	@Nullable
	public static LanguageLevel getLanguageLevelForCompilation(final ModuleChunk chunk)
	{
		return EffectiveLanguageLevelUtil.getEffectiveLanguageLevel(chunk.getModule());
	}
}
