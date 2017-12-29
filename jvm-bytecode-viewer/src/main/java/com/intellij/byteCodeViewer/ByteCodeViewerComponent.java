package com.intellij.byteCodeViewer;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import com.intellij.codeInsight.hint.EditorFragmentComponent;
import com.intellij.execution.filters.LineNumbersMapping;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.ui.LightColors;
import consulo.annotations.RequiredWriteAction;

/**
 * User: anna
 * Date: 5/7/12
 */
public class ByteCodeViewerComponent extends JPanel implements Disposable
{

	private final Editor myEditor;

	public ByteCodeViewerComponent(Project project, AnAction[] additionalActions)
	{
		super(new BorderLayout());
		final EditorFactory factory = EditorFactory.getInstance();
		final Document doc = factory.createDocument("");
		doc.setReadOnly(true);
		myEditor = factory.createEditor(doc, project);
		EditorHighlighterFactory editorHighlighterFactory = EditorHighlighterFactory.getInstance();
		final SyntaxHighlighter syntaxHighlighter = SyntaxHighlighterFactory.getSyntaxHighlighter(PlainTextFileType.INSTANCE, project, null);
		((EditorEx) myEditor).setHighlighter(editorHighlighterFactory.createEditorHighlighter(syntaxHighlighter,
				EditorColorsManager.getInstance().getGlobalScheme()));
		((EditorEx) myEditor).setBackgroundColor(EditorFragmentComponent.getBackgroundColor(myEditor));
		myEditor.getColorsScheme().setColor(EditorColors.CARET_ROW_COLOR, LightColors.SLIGHTLY_GRAY);
		((EditorEx) myEditor).setCaretVisible(true);

		final EditorSettings settings = myEditor.getSettings();
		settings.setLineMarkerAreaShown(false);
		settings.setIndentGuidesShown(false);
		settings.setLineNumbersShown(false);
		settings.setFoldingOutlineShown(false);

		myEditor.setBorder(null);
		add(myEditor.getComponent(), BorderLayout.CENTER);
		final ActionManager actionManager = ActionManager.getInstance();
		final DefaultActionGroup actions = new DefaultActionGroup();
		if(additionalActions != null)
		{
			for(final AnAction action : additionalActions)
			{
				actions.add(action);
			}
		}
		add(actionManager.createActionToolbar(ActionPlaces.JAVADOC_TOOLBAR, actions, true).getComponent(), BorderLayout.NORTH);
	}

	public void setText(final String bytecode)
	{
		setText(bytecode, 0);
	}

	public void setText(final String bytecode, PsiElement element)
	{
		int offset = 0;
		PsiFile psiFile = element.getContainingFile();
		final Document document = PsiDocumentManager.getInstance(element.getProject()).getDocument(psiFile);
		if(document != null)
		{
			int lineNumber = document.getLineNumber(element.getTextOffset());
			VirtualFile file = psiFile.getVirtualFile();
			if(file != null)
			{
				LineNumbersMapping mapping = file.getUserData(LineNumbersMapping.LINE_NUMBERS_MAPPING_KEY);
				if(mapping != null)
				{
					int mappedLine = mapping.sourceToBytecode(lineNumber);
					while(mappedLine == -1 && lineNumber < document.getLineCount())
					{
						mappedLine = mapping.sourceToBytecode(++lineNumber);
					}
					if(mappedLine > 0)
					{
						lineNumber = mappedLine;
					}
				}
			}
			offset = bytecode.indexOf("LINENUMBER " + lineNumber);
			while(offset == -1 && lineNumber < document.getLineCount())
			{
				offset = bytecode.indexOf("LINENUMBER " + (lineNumber++));
			}
		}
		setText(bytecode, Math.max(0, offset));
	}

	public void setText(final String bytecode, final int offset)
	{
		CommandProcessor.getInstance().runUndoTransparentAction(new Runnable()
		{
			@Override
			@RequiredWriteAction
			public void run()
			{
				ApplicationManager.getApplication().runWriteAction(new Runnable()
				{
					@Override
					public void run()
					{
						Document fragmentDoc = myEditor.getDocument();
						fragmentDoc.setReadOnly(false);
						fragmentDoc.replaceString(0, fragmentDoc.getTextLength(), bytecode);
						fragmentDoc.setReadOnly(true);
						myEditor.getCaretModel().moveToOffset(offset);
						myEditor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
					}
				});
			}
		});
	}

	public String getText()
	{
		return myEditor.getDocument().getText();
	}

	@Override
	public void dispose()
	{
		EditorFactory.getInstance().releaseEditor(myEditor);
	}
}