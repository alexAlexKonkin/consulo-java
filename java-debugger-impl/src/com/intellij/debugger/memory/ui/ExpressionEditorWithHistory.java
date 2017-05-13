/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.debugger.memory.ui;

import java.util.List;

import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.SwingWorker;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.debugger.engine.DebuggerUtils;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.popup.list.ListPopupImpl;
import com.intellij.xdebugger.XExpression;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import com.intellij.xdebugger.impl.breakpoints.XExpressionImpl;
import com.intellij.xdebugger.impl.ui.XDebuggerExpressionEditor;

class ExpressionEditorWithHistory extends XDebuggerExpressionEditor
{
	private static final String HISTORY_ID_PREFIX = "filtering";

	ExpressionEditorWithHistory(final @NotNull Project project,
			final @NotNull String className,
			final @NotNull XDebuggerEditorsProvider debuggerEditorsProvider,
			final @Nullable Disposable parentDisposable)
	{
		super(project, debuggerEditorsProvider, HISTORY_ID_PREFIX + className, null, XExpressionImpl.EMPTY_EXPRESSION, false, true, true);

		new AnAction("InstancesWindow.ShowHistory")
		{
			@Override
			public void actionPerformed(AnActionEvent e)
			{
				showHistory();
			}

			@Override
			public void update(AnActionEvent e)
			{
				e.getPresentation().setEnabled(LookupManager.getActiveLookup(getEditor()) == null);
			}
		}.registerCustomShortcutSet(CustomShortcutSet.fromString("DOWN"), getComponent(), parentDisposable);

		new SwingWorker<Void, Void>()
		{
			@Override
			protected Void doInBackground() throws Exception
			{
				ApplicationManager.getApplication().runReadAction(() ->
				{
					final PsiClass psiClass = DebuggerUtils.findClass(className, project, GlobalSearchScope.allScope(project));
					ApplicationManager.getApplication().invokeLater(() -> setContext(psiClass));
				});
				return null;
			}
		}.execute();
	}

	private void showHistory()
	{
		List<XExpression> expressions = getRecentExpressions();
		if(!expressions.isEmpty())
		{
			ListPopupImpl historyPopup = new ListPopupImpl(new BaseListPopupStep<XExpression>(null, expressions)
			{
				@Override
				public PopupStep onChosen(XExpression selectedValue, boolean finalChoice)
				{
					setExpression(selectedValue);
					requestFocusInEditor();
					return FINAL_CHOICE;
				}
			})
			{
				@Override
				protected ListCellRenderer getListElementRenderer()
				{
					return new ColoredListCellRenderer<XExpression>()
					{
						@Override
						protected void customizeCellRenderer(@NotNull JList list, XExpression value, int index, boolean selected, boolean hasFocus)
						{
							append(value.getExpression(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
						}
					};
				}
			};

			historyPopup.getList().setFont(EditorUtil.getEditorFont());
			historyPopup.showUnderneathOf(getEditorComponent());
		}
	}
}
