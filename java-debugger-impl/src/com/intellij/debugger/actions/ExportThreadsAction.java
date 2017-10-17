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

/**
 * class ExportThreadsAction
 * @author Jeka
 */
package com.intellij.debugger.actions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.jetbrains.annotations.NotNull;
import com.intellij.debugger.DebuggerManagerEx;
import com.intellij.debugger.impl.DebuggerContextImpl;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.debugger.ui.ExportDialog;
import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.SystemProperties;

public class ExportThreadsAction extends AnAction implements AnAction.TransparentUpdate
{
	@Override
	public void actionPerformed(@NotNull AnActionEvent e)
	{
		Project project = e.getData(CommonDataKeys.PROJECT);
		if(project == null)
		{
			return;
		}
		DebuggerContextImpl context = (DebuggerManagerEx.getInstanceEx(project)).getContext();

		if(context.getDebuggerSession() != null)
		{
			String destinationDirectory = "";
			final VirtualFile baseDir = project.getBaseDir();
			if(baseDir != null)
			{
				destinationDirectory = baseDir.getPresentableUrl();
			}

			ExportDialog dialog = new ExportDialog(context.getDebugProcess(), destinationDirectory);
			dialog.show();
			if(dialog.isOK())
			{
				try
				{
					File file = new File(dialog.getFilePath());
					try (BufferedWriter writer = new BufferedWriter(new FileWriter(file)))
					{
						String text = StringUtil.convertLineSeparators(dialog.getTextToSave(), SystemProperties.getLineSeparator());
						writer.write(text);
					}
				}
				catch(IOException ex)
				{
					Messages.showMessageDialog(project, ex.getMessage(), ActionsBundle.actionText(DebuggerActions.EXPORT_THREADS), Messages.getErrorIcon());
				}
			}
		}
	}

	@Override
	public void update(@NotNull AnActionEvent event)
	{
		Presentation presentation = event.getPresentation();
		Project project = event.getData(CommonDataKeys.PROJECT);
		if(project == null)
		{
			presentation.setEnabled(false);
			return;
		}
		DebuggerSession debuggerSession = (DebuggerManagerEx.getInstanceEx(project)).getContext().getDebuggerSession();
		presentation.setEnabled(debuggerSession != null && debuggerSession.isPaused());
	}
}