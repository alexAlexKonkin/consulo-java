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
package com.intellij.testFramework;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.ide.highlighter.ModuleFileType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.impl.ModuleImpl;
import com.intellij.openapi.project.impl.ProjectImpl;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;

public abstract class ModuleTestCase extends IdeaTestCase
{
	private static final Logger LOGGER = Logger.getInstance(ModuleTestCase.class);
	protected final Collection<Module> myModulesToDispose = new ArrayList<Module>();

	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		myModulesToDispose.clear();
	}

	@Override
	protected void tearDown() throws Exception
	{
		try
		{
			final ModuleManager moduleManager = ModuleManager.getInstance(myProject);
			ApplicationManager.getApplication().runWriteAction(new Runnable()
			{
				@Override
				public void run()
				{
					for(Module module : myModulesToDispose)
					{
						String moduleName = module.getName();
						if(moduleManager.findModuleByName(moduleName) != null)
						{
							moduleManager.disposeModule(module);
						}
					}
				}
			});
		}
		finally
		{
			myModulesToDispose.clear();
			super.tearDown();
		}
	}


	protected Module createModule(final File moduleFile)
	{
		final String path = moduleFile.getAbsolutePath();
		return createModule(path);
	}

	protected Module createModule(final String path)
	{
		Module module = ApplicationManager.getApplication().runWriteAction(new Computable<Module>()
		{
			@Override
			public Module compute()
			{
				return ModuleManager.getInstance(myProject).newModule(path, path);
			}
		});

		myModulesToDispose.add(module);
		return module;
	}

	protected Module loadModule(final File moduleFile)
	{
		Module module = ApplicationManager.getApplication().runWriteAction(new Computable<Module>()
		{
			@Override
			public Module compute()
			{
				try
				{
					return ModuleManager.getInstance(myProject).loadModule(moduleFile.getAbsolutePath());
				}
				catch(Exception e)
				{
					LOGGER.error(e);
					return null;
				}
			}
		});

		myModulesToDispose.add(module);
		return module;
	}

	protected Module loadModule(final String modulePath)
	{
		return loadModule(new File(modulePath));
	}

	@Nullable
	protected Module loadAllModulesUnder(@NotNull VirtualFile rootDir) throws Exception
	{
		final Ref<Module> result = Ref.create();

		VfsUtilCore.visitChildrenRecursively(rootDir, new VirtualFileVisitor()
		{
			@Override
			public boolean visitFile(@NotNull VirtualFile file)
			{
				if(!file.isDirectory() && file.getName().endsWith(ModuleFileType.DOT_DEFAULT_EXTENSION))
				{
					ModuleImpl module = (ModuleImpl) loadModule(new File(file.getPath()));
					readJdomExternalizables(module);
					result.setIfNull(module);
				}
				return true;
			}
		});

		return result.get();
	}

	protected void readJdomExternalizables(final ModuleImpl module)
	{
		ApplicationManager.getApplication().runWriteAction(new Runnable()
		{
			@Override
			public void run()
			{
				final ProjectImpl project = (ProjectImpl) myProject;
				project.setOptimiseTestLoadSpeed(false);

				project.setOptimiseTestLoadSpeed(true);
			}
		});
	}

	protected Module createModuleFromTestData(final String dirInTestData, final String newModuleFileName, final boolean addSourceRoot) throws IOException
	{
		final File dirInTestDataFile = new File(dirInTestData);
		assertTrue(dirInTestDataFile.isDirectory());
		final File moduleDir = createTempDirectory();
		FileUtil.copyDir(dirInTestDataFile, moduleDir);
		final Module module = createModule(moduleDir + "/" + newModuleFileName);
		final VirtualFile root = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(moduleDir);
		new WriteCommandAction.Simple(module.getProject())
		{
			@Override
			protected void run() throws Throwable
			{
				root.refresh(false, true);
			}
		}.execute().throwException();
		if(addSourceRoot)
		{
			PsiTestUtil.addSourceContentToRoots(module, root);
		}
		else
		{
			PsiTestUtil.addContentRoot(module, root);
		}
		return module;
	}
}
