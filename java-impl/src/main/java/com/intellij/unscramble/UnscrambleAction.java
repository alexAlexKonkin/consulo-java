/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.unscramble;

import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import consulo.annotations.RequiredDispatchThread;

/**
 * @author Konstantin Bulenkov
 */
public final class UnscrambleAction extends AnAction implements DumbAware
{
	@RequiredDispatchThread
	@Override
	public void actionPerformed(@NotNull AnActionEvent e)
	{
		final Project project = e.getData(CommonDataKeys.PROJECT);
		new UnscrambleDialog(project).show();
	}

	@RequiredDispatchThread
	@Override
	public void update(@NotNull AnActionEvent event)
	{
		final Presentation presentation = event.getPresentation();
		final Project project = event.getData(CommonDataKeys.PROJECT);
		presentation.setEnabled(project != null);
	}
}