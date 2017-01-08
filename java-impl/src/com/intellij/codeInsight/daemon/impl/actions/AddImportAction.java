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
package com.intellij.codeInsight.daemon.impl.actions;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

import org.jetbrains.annotations.NotNull;
import com.intellij.application.options.editor.JavaAutoImportConfigurable;
import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.CodeInsightUtil;
import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInsight.actions.OptimizeImportsProcessor;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.hint.QuestionAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.statistics.JavaStatisticsManager;
import com.intellij.psi.statistics.StatisticsManager;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ObjectUtil;
import consulo.annotations.RequiredReadAction;
import consulo.ide.IconDescriptorUpdaters;
import consulo.java.JavaQuickFixBundle;

public class AddImportAction implements QuestionAction
{
	private static final Logger LOG = Logger.getInstance("#com.intellij.codeInsight.daemon.impl.actions.AddImportAction");

	private final Project myProject;
	private final PsiReference myReference;
	private final PsiClass[] myTargetClasses;
	private final Editor myEditor;

	public AddImportAction(@NotNull Project project, @NotNull PsiReference ref, @NotNull Editor editor, @NotNull PsiClass... targetClasses)
	{
		myProject = project;
		myReference = ref;
		myTargetClasses = targetClasses;
		myEditor = editor;
	}

	@Override
	public boolean execute()
	{
		PsiDocumentManager.getInstance(myProject).commitAllDocuments();

		if(!myReference.getElement().isValid())
		{
			return false;
		}

		for(PsiClass myTargetClass : myTargetClasses)
		{
			if(!myTargetClass.isValid())
			{
				return false;
			}
		}

		if(myTargetClasses.length == 1)
		{
			addImport(myReference, myTargetClasses[0]);
		}
		else
		{
			chooseClassAndImport();
		}
		return true;
	}

	private void chooseClassAndImport()
	{
		CodeInsightUtil.sortIdenticalShortNameClasses(myTargetClasses, myReference);

		final BaseListPopupStep<PsiClass> step = new BaseListPopupStep<PsiClass>(JavaQuickFixBundle.message("class.to.import.chooser.title"), myTargetClasses)
		{
			@Override
			public boolean isAutoSelectionEnabled()
			{
				return false;
			}

			@Override
			public boolean isSpeedSearchEnabled()
			{
				return true;
			}

			@Override
			public PopupStep onChosen(PsiClass selectedValue, boolean finalChoice)
			{
				if(selectedValue == null)
				{
					return FINAL_CHOICE;
				}

				if(finalChoice)
				{
					PsiDocumentManager.getInstance(myProject).commitAllDocuments();
					addImport(myReference, selectedValue);
					return FINAL_CHOICE;
				}

				String qname = selectedValue.getQualifiedName();
				if(qname == null)
				{
					return FINAL_CHOICE;
				}

				List<String> toExclude = getAllExcludableStrings(qname);

				return new BaseListPopupStep<String>(null, toExclude)
				{
					@NotNull
					@Override
					public String getTextFor(String value)
					{
						return "Exclude '" + value + "' from auto-import";
					}

					@Override
					public PopupStep onChosen(String selectedValue, boolean finalChoice)
					{
						if(finalChoice)
						{
							excludeFromImport(myProject, selectedValue);
						}

						return super.onChosen(selectedValue, finalChoice);
					}
				};
			}

			@Override
			public boolean hasSubstep(PsiClass selectedValue)
			{
				return true;
			}

			@NotNull
			@Override
			public String getTextFor(PsiClass value)
			{
				return ObjectUtil.assertNotNull(value.getQualifiedName());
			}

			@Override
			public Icon getIconFor(PsiClass aValue)
			{
				return IconDescriptorUpdaters.getIcon(aValue, 0);
			}
		};
		JBPopupFactory.getInstance().createListPopup(step).showInBestPositionFor(myEditor);
	}

	public static void excludeFromImport(final Project project, final String prefix)
	{
		ApplicationManager.getApplication().invokeLater(() -> {
			if(project.isDisposed())
			{
				return;
			}

			final JavaAutoImportConfigurable configurable = new JavaAutoImportConfigurable();
			ShowSettingsUtil.getInstance().editConfigurable(project, configurable, () -> {
				configurable.addExcludePackage(prefix);
			});
		});
	}

	public static List<String> getAllExcludableStrings(@NotNull String qname)
	{
		List<String> toExclude = new ArrayList<String>();
		while(true)
		{
			toExclude.add(qname);
			final int i = qname.lastIndexOf('.');
			if(i < 0 || i == qname.indexOf('.'))
			{
				break;
			}
			qname = qname.substring(0, i);
		}
		return toExclude;
	}

	private void addImport(final PsiReference ref, final PsiClass targetClass)
	{
		StatisticsManager.getInstance().incUseCount(JavaStatisticsManager.createInfo(null, targetClass));
		CommandProcessor.getInstance().executeCommand(myProject, () -> ApplicationManager.getApplication().runWriteAction(() -> {
			_addImport(ref, targetClass);
		}), JavaQuickFixBundle.message("add.import"), null);
	}

	@RequiredReadAction
	private void _addImport(PsiReference ref, PsiClass targetClass)
	{
		if(!ref.getElement().isValid() || !targetClass.isValid() || ref.resolve() == targetClass)
		{
			return;
		}
		if(!FileModificationService.getInstance().preparePsiElementForWrite(ref.getElement()))
		{
			return;
		}

		int caretOffset = myEditor.getCaretModel().getOffset();
		RangeMarker caretMarker = myEditor.getDocument().createRangeMarker(caretOffset, caretOffset);
		int colByOffset = myEditor.offsetToLogicalPosition(caretOffset).column;
		int col = myEditor.getCaretModel().getLogicalPosition().column;
		int virtualSpace = col == colByOffset ? 0 : col - colByOffset;
		int line = myEditor.getCaretModel().getLogicalPosition().line;
		LogicalPosition pos = new LogicalPosition(line, 0);
		myEditor.getCaretModel().moveToLogicalPosition(pos);

		try
		{
			bindReference(ref, targetClass);
			if(CodeInsightSettings.getInstance().OPTIMIZE_IMPORTS_ON_THE_FLY)
			{
				Document document = myEditor.getDocument();
				PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(document);
				new OptimizeImportsProcessor(myProject, psiFile).runWithoutProgress();
			}
		}
		catch(IncorrectOperationException e)
		{
			LOG.error(e);
		}

		line = myEditor.getCaretModel().getLogicalPosition().line;
		LogicalPosition pos1 = new LogicalPosition(line, col);
		myEditor.getCaretModel().moveToLogicalPosition(pos1);
		if(caretMarker.isValid())
		{
			LogicalPosition pos2 = myEditor.offsetToLogicalPosition(caretMarker.getStartOffset());
			int newCol = pos2.column + virtualSpace;
			myEditor.getCaretModel().moveToLogicalPosition(new LogicalPosition(pos2.line, newCol));
			myEditor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
		}
		ApplicationManager.getApplication().invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				if(!myProject.isDisposed() && myProject.isOpen())
				{
					DaemonCodeAnalyzer daemonCodeAnalyzer = DaemonCodeAnalyzer.getInstance(myProject);
					if(daemonCodeAnalyzer != null)
					{
						daemonCodeAnalyzer.updateVisibleHighlighters(myEditor);
					}
				}
			}
		});
	}

	protected void bindReference(PsiReference ref, PsiClass targetClass)
	{
		ref.bindToElement(targetClass);
	}
}
