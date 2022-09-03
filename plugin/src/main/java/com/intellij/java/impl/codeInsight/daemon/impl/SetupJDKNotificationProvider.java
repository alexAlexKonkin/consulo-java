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
package com.intellij.java.impl.codeInsight.daemon.impl;

import javax.annotation.Nonnull;

import com.intellij.ProjectTopics;
import com.intellij.java.language.JavaCoreBundle;
import com.intellij.java.language.impl.JavaClassFileType;
import com.intellij.java.language.JavaLanguage;
import consulo.fileEditor.FileEditor;
import consulo.module.Module;
import consulo.language.util.ModuleUtilCore;
import consulo.project.Project;
import consulo.module.content.layer.event.ModuleRootAdapter;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.project.ui.view.internal.ProjectSettingsService;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.ide.impl.idea.ui.EditorNotificationPanel;
import consulo.fileEditor.EditorNotifications;
import consulo.annotation.access.RequiredReadAction;
import consulo.fileEditor.EditorNotificationProvider;
import consulo.java.language.module.extension.JavaModuleExtension;
import consulo.module.extension.ModuleExtension;

/**
 * @author Danila Ponomarenko
 */
public class SetupJDKNotificationProvider implements EditorNotificationProvider<EditorNotificationPanel>
{
	private static final Key<consulo.ide.impl.idea.ui.EditorNotificationPanel> KEY = Key.create("setup.jdk.notifier");

	private final Project myProject;

	public SetupJDKNotificationProvider(Project project, final EditorNotifications notifications)
	{
		myProject = project;
		myProject.getMessageBus().connect().subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootAdapter()
		{
			@Override
			public void rootsChanged(ModuleRootEvent event)
			{
				notifications.updateAllNotifications();
			}
		});
		myProject.getMessageBus().connect().subscribe(ModuleExtension.CHANGE_TOPIC, (oldExtension, newExtension) -> notifications.updateAllNotifications());
	}

	@Nonnull
	@Override
	public Key<EditorNotificationPanel> getKey()
	{
		return KEY;
	}

	@RequiredReadAction
	@Override
	public EditorNotificationPanel createNotificationPanel(@Nonnull VirtualFile file, @Nonnull FileEditor fileEditor)
	{
		if(file.getFileType() == JavaClassFileType.INSTANCE)
		{
			return null;
		}

		final PsiFile psiFile = PsiManager.getInstance(myProject).findFile(file);
		if(psiFile == null)
		{
			return null;
		}

		if(psiFile.getLanguage() != JavaLanguage.INSTANCE)
		{
			return null;
		}

		final Module moduleForPsiElement = ModuleUtilCore.findModuleForPsiElement(psiFile);
		if(moduleForPsiElement == null)
		{
			return null;
		}
		final JavaModuleExtension extension = ModuleUtilCore.getExtension(moduleForPsiElement, JavaModuleExtension.class);
		if(extension == null)
		{
			return null;
		}

		if(extension.getInheritableSdk().isNull())
		{
			return createPanel(myProject, psiFile);
		}
		return null;
	}

	@Nonnull
	private static EditorNotificationPanel createPanel(final @Nonnull Project project, final @Nonnull PsiFile file)
	{
		EditorNotificationPanel panel = new consulo.ide.impl.idea.ui.EditorNotificationPanel();
		panel.setText(JavaCoreBundle.message("module.jdk.not.defined"));
		panel.createActionLabel(JavaCoreBundle.message("module.jdk.setup"), () ->
		{
			final Module moduleForPsiElement = ModuleUtilCore.findModuleForPsiElement(file);
			if(moduleForPsiElement == null)
			{
				return;
			}

			ProjectSettingsService.getInstance(project).openModuleSettings(moduleForPsiElement);
		});
		return panel;
	}
}
