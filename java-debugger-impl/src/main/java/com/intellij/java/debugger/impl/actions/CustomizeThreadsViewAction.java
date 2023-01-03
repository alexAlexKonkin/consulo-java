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
package com.intellij.java.debugger.impl.actions;

import com.intellij.java.debugger.DebuggerBundle;
import com.intellij.java.debugger.impl.settings.ThreadsViewSettings;
import consulo.ui.ex.action.ActionsBundle;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.project.Project;
import consulo.java.debugger.impl.settings.ThreadsViewConfigurable;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;

/**
 * User: lex
 * Date: Sep 26, 2003
 * Time: 4:40:12 PM
 */
public class CustomizeThreadsViewAction extends DebuggerAction
{
	@RequiredUIAccess
	@Override
	public void actionPerformed(@Nonnull AnActionEvent e)
	{
		Project project = e.getData(CommonDataKeys.PROJECT);

		ShowSettingsUtil.getInstance().editConfigurable(DebuggerBundle.message("threads.view.configurable.display.name"), project, new ThreadsViewConfigurable(ThreadsViewSettings::getInstance));
	}

	@RequiredUIAccess
	@Override
	public void update(@Nonnull AnActionEvent e)
	{
		e.getPresentation().setVisible(true);
		e.getPresentation().setText(ActionsBundle.actionText(DebuggerActions.CUSTOMIZE_THREADS_VIEW));
	}
}
