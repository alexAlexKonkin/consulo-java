/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.java.impl.javadoc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import consulo.language.ast.ASTNode;
import consulo.application.ApplicationManager;
import consulo.codeEditor.CaretModel;
import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.codeEditor.LogicalPosition;
import consulo.project.Project;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import com.intellij.java.language.psi.JavaDocTokenType;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.CodeStyleSettingsManager;
import com.intellij.java.impl.psi.codeStyle.JavaCodeStyleSettings;
import com.intellij.java.language.psi.javadoc.PsiDocTag;
import com.intellij.java.language.psi.javadoc.PsiDocTagValue;
import com.intellij.java.language.psi.javadoc.PsiDocToken;
import consulo.language.ast.IElementType;
import consulo.language.psi.util.PsiTreeUtil;

/**
 * This class is not singleton but provides {@link #getInstance() single-point-of-usage field}.
 *
 * @author Denis Zhdanov
 * @since 5/27/11 2:35 PM
 */
public class JavadocHelper
{

	private static final String PARAM_TEXT = "param";

	private static final Pair<JavadocParameterInfo, List<JavadocParameterInfo>> EMPTY
			= new Pair<JavadocParameterInfo, List<JavadocParameterInfo>>(null, Collections.<JavadocParameterInfo>emptyList());
	private static final JavadocHelper INSTANCE = new JavadocHelper();

	@Nonnull
	public static JavadocHelper getInstance()
	{
		return INSTANCE;
	}

	/**
	 * Tries to navigate caret at the given editor to the target position inserting missing white spaces if necessary.
	 *
	 * @param position target caret position
	 * @param editor   target editor
	 * @param project  target project
	 */
	@SuppressWarnings("MethodMayBeStatic")
	public void navigate(@Nonnull LogicalPosition position, @Nonnull Editor editor, @Nonnull final Project project)
	{
		final Document document = editor.getDocument();
		final CaretModel caretModel = editor.getCaretModel();
		final int endLineOffset = document.getLineEndOffset(position.line);
		final LogicalPosition endLinePosition = editor.offsetToLogicalPosition(endLineOffset);
		if(endLinePosition.column < position.column && !editor.getSettings().isVirtualSpace() && !editor.isViewer())
		{
			final String toInsert = StringUtil.repeat(" ", position.column - endLinePosition.column);
			ApplicationManager.getApplication().runWriteAction(new Runnable()
			{
				@Override
				public void run()
				{
					document.insertString(endLineOffset, toInsert);
					PsiDocumentManager.getInstance(project).commitDocument(document);
				}
			});

		}
		caretModel.moveToLogicalPosition(position);
	}

	/**
	 * Calculates desired position of target javadoc parameter's description start.
	 *
	 * @param psiFile PSI holder
	 * @param data    parsed adjacent javadoc parameters
	 * @param anchor  descriptor for the target parameter
	 * @return logical position that points to the desired parameter description start location
	 */
	@SuppressWarnings("MethodMayBeStatic")
	@Nonnull
	public LogicalPosition calculateDescriptionStartPosition(@Nonnull PsiFile psiFile,
															 @Nonnull Collection<JavadocParameterInfo> data,
															 @Nonnull JavadocHelper.JavadocParameterInfo anchor)
	{
		int descriptionStartColumn = -1;
		int parameterNameEndColumn = -1;
		for(JavadocHelper.JavadocParameterInfo parameterInfo : data)
		{
			parameterNameEndColumn = Math.max(parameterNameEndColumn, parameterInfo.parameterNameEndPosition.column);
			if(parameterInfo.parameterDescriptionStartPosition != null)
			{
				descriptionStartColumn = Math.max(descriptionStartColumn, parameterInfo.parameterDescriptionStartPosition.column);
			}
		}

		final CodeStyleSettings codeStyleSettings = CodeStyleSettingsManager.getInstance(psiFile.getProject()).getCurrentSettings();
		final int indentSize = codeStyleSettings.getIndentSize(psiFile.getFileType());
		int column;
		if(codeStyleSettings.getCustomSettings(JavaCodeStyleSettings.class).JD_ALIGN_PARAM_COMMENTS)
		{
			column = Math.max(descriptionStartColumn, parameterNameEndColumn);
			if(column <= parameterNameEndColumn)
			{
				column = parameterNameEndColumn + indentSize;
			}
		}
		else
		{
			column = anchor.parameterNameEndPosition.column + indentSize;
		}
		return new LogicalPosition(anchor.parameterNameEndPosition.line, column);
	}

	/**
	 * Returns information about all lines that contain javadoc parameters and are adjacent to the one that holds given offset.
	 *
	 * @param psiFile PSI holder for the document exposed the given editor
	 * @param editor  target editor
	 * @param offset  target offset that identifies anchor line to check
	 * @return pair like (javadoc info for the line identified by the given offset; list of javadoc parameter infos for
	 * adjacent lines if any
	 */
	@SuppressWarnings("MethodMayBeStatic")
	@Nonnull
	public Pair<JavadocParameterInfo, List<JavadocParameterInfo>> parse(@Nonnull PsiFile psiFile, @Nonnull Editor editor, int offset)
	{
		List<JavadocParameterInfo> result = new ArrayList<JavadocParameterInfo>();
		final PsiElement elementAtCaret = psiFile.findElementAt(offset);
		if(elementAtCaret == null)
		{
			return EMPTY;
		}

		PsiDocTag tag = PsiTreeUtil.getParentOfType(elementAtCaret, PsiDocTag.class);
		if(tag == null)
		{
			// Due to javadoc PSI specifics.
			if(elementAtCaret instanceof PsiWhiteSpace)
			{
				for(PsiElement e = elementAtCaret.getPrevSibling(); e != null && tag == null; e = e.getPrevSibling())
				{
					tag = PsiTreeUtil.getParentOfType(e, PsiDocTag.class, false);
					if(e instanceof PsiWhiteSpace
							|| (e instanceof PsiDocToken && ((PsiDocToken) e).getTokenType() == JavaDocTokenType.DOC_COMMENT_LEADING_ASTERISKS))
					{
						continue;
					}
					break;
				}
			}
		}
		if(tag == null)
		{
			return EMPTY;
		}

		JavadocParameterInfo anchorInfo = parse(tag, editor);
		if(anchorInfo == null)
		{
			return EMPTY;
		}

		// Parse previous parameters.
		for(PsiElement e = tag.getPrevSibling(); e != null; e = e.getPrevSibling())
		{
			JavadocParameterInfo info = parse(e, editor);
			if(info == null)
			{
				break;
			}
			result.add(0, info);
		}

		result.add(anchorInfo);

		// Parse subsequent parameters.
		for(PsiElement e = tag.getNextSibling(); e != null; e = e.getNextSibling())
		{
			JavadocParameterInfo info = parse(e, editor);
			if(info == null)
			{
				break;
			}
			result.add(info);
		}

		return new Pair<JavadocParameterInfo, List<JavadocParameterInfo>>(anchorInfo, result);
	}

	@Nullable
	private static JavadocParameterInfo parse(@Nonnull PsiElement element, @Nonnull Editor editor)
	{
		final PsiDocTag tag = PsiTreeUtil.getParentOfType(element, PsiDocTag.class, false);
		if(tag == null || !PARAM_TEXT.equals(tag.getName()))
		{
			return null;
		}

		final PsiDocTagValue paramRef = PsiTreeUtil.getChildOfType(tag, PsiDocTagValue.class);
		if(paramRef == null)
		{
			return null;
		}

		for(PsiElement e = paramRef.getNextSibling(); e != null; e = e.getNextSibling())
		{
			final ASTNode node = e.getNode();
			if(node == null)
			{
				break;
			}
			final IElementType elementType = node.getElementType();
			if(elementType == JavaDocTokenType.DOC_COMMENT_DATA)
			{
				return new JavadocParameterInfo(
						editor.offsetToLogicalPosition(paramRef.getTextRange().getEndOffset()),
						editor.offsetToLogicalPosition(e.getTextRange().getStartOffset()),
						editor.getDocument().getLineNumber(e.getTextRange().getEndOffset())
				);
			}
			else if(elementType == JavaDocTokenType.DOC_COMMENT_LEADING_ASTERISKS)
			{
				break;
			}
		}
		return new JavadocParameterInfo(
				editor.offsetToLogicalPosition(paramRef.getTextRange().getEndOffset()),
				null,
				editor.getDocument().getLineNumber(paramRef.getTextRange().getEndOffset())
		);
	}

	/**
	 * Encapsulates information about source code line that holds javadoc parameter.
	 */
	public static class JavadocParameterInfo
	{

		/**
		 * Logical position that points to location just after javadoc parameter name.
		 * <p>
		 * Example:
		 * <pre>
		 *   /**
		 *    * @param i[X]  description
		 *    *&#47;
		 * </pre>
		 */
		@Nonnull
		public final LogicalPosition parameterNameEndPosition;
		@Nullable
		public final LogicalPosition parameterDescriptionStartPosition;
		/**
		 * Last logical line occupied by the current javadoc parameter.
		 */
		public final int lastLine;

		public JavadocParameterInfo(@Nonnull LogicalPosition parameterNameEndPosition,
									LogicalPosition parameterDescriptionStartPosition,
									int lastLine)
		{
			this.parameterNameEndPosition = parameterNameEndPosition;
			this.parameterDescriptionStartPosition = parameterDescriptionStartPosition;
			this.lastLine = lastLine;
		}

		@Override
		public String toString()
		{
			return "name end: " + parameterNameEndPosition + ", description start: " + parameterDescriptionStartPosition;
		}
	}
}
