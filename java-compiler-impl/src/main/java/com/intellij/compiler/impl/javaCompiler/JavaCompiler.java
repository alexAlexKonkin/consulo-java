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

package com.intellij.compiler.impl.javaCompiler;

import gnu.trove.THashMap;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import jakarta.inject.Inject;

import com.intellij.compiler.CompilerException;
import com.intellij.compiler.impl.CompileDriver;
import com.intellij.compiler.make.CacheCorruptedException;
import com.intellij.ide.highlighter.JavaClassFileType;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.compiler.TranslatingCompiler;
import com.intellij.openapi.compiler.ex.CompileContextEx;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import consulo.util.dataholder.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Chunk;
import com.intellij.util.ExceptionUtil;
import consulo.java.module.extension.JavaModuleExtension;
import consulo.roots.ContentFolderTypeProvider;
import consulo.roots.impl.ProductionResourceContentFolderTypeProvider;
import consulo.roots.impl.TestResourceContentFolderTypeProvider;

/*
 * @author: Eugene Zhuravlev
 * Date: Jan 17, 2003
 * Time: 3:22:59 PM
 */
public class JavaCompiler implements TranslatingCompiler
{
	private static final Logger LOGGER = Logger.getInstance(JavaCompiler.class);

	public static final Key<Map<File, FileObject>> ourOutputFileParseInfo = Key.create("ourOutputFileParseInfo");

	private final Project myProject;

	@Inject
	public JavaCompiler(Project project)
	{
		myProject = project;
	}

	@Override
	@Nonnull
	public String getDescription()
	{
		return CompilerBundle.message("java.compiler.description");
	}

	@Override
	public boolean isCompilableFile(VirtualFile file, CompileContext context)
	{
		return file.getFileType() == JavaFileType.INSTANCE;
	}

	@Override
	public void compile(CompileContext context, Chunk<Module> moduleChunk, VirtualFile[] files, OutputSink sink)
	{
		boolean found = false;
		for(Module module : moduleChunk.getNodes())
		{
			JavaModuleExtension extension = ModuleUtilCore.getExtension(module, JavaModuleExtension.class);
			if(extension != null)
			{
				found = true;
				break;
			}
		}

		if(!found)
		{
			return;
		}

		Map<File, FileObject> parsingInfo = new THashMap<>(FileUtil.FILE_HASHING_STRATEGY);
		context.putUserData(ourOutputFileParseInfo, parsingInfo);

		final BackendCompiler backEndCompiler = getBackEndCompiler();
		final BackendCompilerWrapper wrapper = new BackendCompilerWrapper(this, moduleChunk, myProject, filterResourceFiles(context, files), (CompileContextEx) context, backEndCompiler, sink);
		try
		{
			if(CompileDriver.ourDebugMode)
			{
				System.out.println("Starting java compiler; with backend compiler: " + backEndCompiler.getClass().getName());
			}
			wrapper.compile(parsingInfo);
		}
		catch(CompilerException e)
		{
			if(CompileDriver.ourDebugMode)
			{
				e.printStackTrace();
			}
			context.addMessage(CompilerMessageCategory.ERROR, ExceptionUtil.getThrowableText(e), null, -1, -1);
			LOGGER.info(e);
		}
		catch(CacheCorruptedException e)
		{
			if(CompileDriver.ourDebugMode)
			{
				e.printStackTrace();
			}
			LOGGER.info(e);
			context.requestRebuildNextTime(e.getMessage());
		}
	}

	@Nonnull
	private static List<VirtualFile> filterResourceFiles(CompileContext compileContext, VirtualFile[] virtualFiles)
	{
		ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(compileContext.getProject());

		List<VirtualFile> list = new ArrayList<>(virtualFiles.length);
		for(VirtualFile file : virtualFiles)
		{
			ContentFolderTypeProvider provider = fileIndex.getContentFolderTypeForFile(file);
			if(provider == ProductionResourceContentFolderTypeProvider.getInstance() || provider == TestResourceContentFolderTypeProvider.getInstance())
			{
				continue;
			}
			list.add(file);
		}
		return list;
	}

	@Nonnull
	@Override
	public FileType[] getInputFileTypes()
	{
		return new FileType[]{JavaFileType.INSTANCE};
	}

	@Nonnull
	@Override
	public FileType[] getOutputFileTypes()
	{
		return new FileType[]{JavaClassFileType.INSTANCE};
	}

	@Override
	public boolean validateConfiguration(CompileScope scope)
	{
		return getBackEndCompiler().checkCompiler(scope);
	}

	@Override
	public void registerCompilableFileTypes(@Nonnull Consumer<FileType> fileTypeConsumer)
	{
		fileTypeConsumer.accept(JavaFileType.INSTANCE);
	}

	private BackendCompiler getBackEndCompiler()
	{
		return JavaCompilerConfiguration.getInstance(myProject).getActiveCompiler();
	}
}
