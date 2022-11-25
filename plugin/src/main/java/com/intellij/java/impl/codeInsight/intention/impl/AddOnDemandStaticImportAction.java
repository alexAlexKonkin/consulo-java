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
package com.intellij.java.impl.codeInsight.intention.impl;

import consulo.language.editor.CodeInsightBundle;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.highlight.HighlightManager;
import consulo.language.editor.intention.PsiElementBaseIntentionAction;
import com.intellij.java.language.psi.*;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorColors;
import consulo.colorScheme.EditorColorsManager;
import consulo.project.Project;
import consulo.util.lang.Comparing;
import consulo.language.psi.*;
import consulo.language.psi.util.PsiTreeUtil;
import com.intellij.java.language.psi.util.PsiUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.collection.primitive.ints.IntLists;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.PrimitiveIterator;

/**
 * @author ven
 */
public class AddOnDemandStaticImportAction extends PsiElementBaseIntentionAction
{
	private static final Logger LOG = Logger.getInstance(AddOnDemandStaticImportAction.class);

	@Override
	@Nonnull
	public String getFamilyName()
	{
		return CodeInsightBundle.message("intention.add.on.demand.static.import.family");
	}

	/**
	 * Allows to check if static import may be performed for the given element.
	 *
	 * @param element element to check
	 * @return target class that may be statically imported if any; <code>null</code> otherwise
	 */
	@Nullable
	public static PsiClass getClassToPerformStaticImport(@Nonnull PsiElement element)
	{
		if(!PsiUtil.isLanguageLevel5OrHigher(element))
		{
			return null;
		}
		if(!(element instanceof PsiIdentifier) || !(element.getParent() instanceof PsiJavaCodeReferenceElement))
		{
			return null;
		}
		PsiJavaCodeReferenceElement refExpr = (PsiJavaCodeReferenceElement) element.getParent();
		if(refExpr instanceof PsiMethodReferenceExpression)
		{
			return null;
		}
		final PsiElement gParent = refExpr.getParent();
		if(gParent instanceof PsiMethodReferenceExpression)
		{
			return null;
		}
		if(!(gParent instanceof PsiJavaCodeReferenceElement) ||
				isParameterizedReference((PsiJavaCodeReferenceElement) gParent))
		{
			return null;
		}

		PsiElement resolved = refExpr.resolve();
		if(!(resolved instanceof PsiClass))
		{
			return null;
		}
		PsiClass psiClass = (PsiClass) resolved;
		if(Comparing.strEqual(psiClass.getName(), psiClass.getQualifiedName()) || psiClass.hasModifierProperty(PsiModifier.PRIVATE))
		{
			return null;
		}
		PsiFile file = refExpr.getContainingFile();
		if(!(file instanceof PsiJavaFile))
		{
			return null;
		}
		PsiImportList importList = ((PsiJavaFile) file).getImportList();
		if(importList == null)
		{
			return null;
		}
		for(PsiImportStaticStatement statement : importList.getImportStaticStatements())
		{
			PsiClass staticResolve = statement.resolveTargetClass();
			if(psiClass == staticResolve)
			{
				return null; //already imported
			}
		}

		return psiClass;
	}

	@Override
	public boolean isAvailable(@Nonnull Project project, Editor editor, @Nonnull PsiElement element)
	{
		PsiClass classToImport = getClassToPerformStaticImport(element);
		if(classToImport != null)
		{
			String text = CodeInsightBundle.message("intention.add.on.demand.static.import.text", classToImport.getQualifiedName());
			setText(text);
		}
		return classToImport != null;
	}

	public static void invoke(final Project project, PsiFile file, final Editor editor, PsiElement element)
	{
		if(!FileModificationService.getInstance().prepareFileForWrite(file))
		{
			return;
		}

		final PsiJavaCodeReferenceElement refExpr = (PsiJavaCodeReferenceElement) element.getParent();
		final PsiClass aClass = (PsiClass) refExpr.resolve();
		if(aClass == null)
		{
			return;
		}
		PsiImportStaticStatement importStaticStatement =
				JavaPsiFacade.getInstance(file.getProject()).getElementFactory().createImportStaticStatement(aClass, "*");
		PsiImportList importList = ((PsiJavaFile) file).getImportList();
		if(importList == null)
		{
			return;
		}
		importList.add(importStaticStatement);

		List<PsiFile> roots = file.getViewProvider().getAllFiles();
		for(final PsiFile root : roots)
		{
			PsiElement copy = root.copy();
			final PsiManager manager = root.getManager();

			final IntList expressionToDequalifyOffsets = IntLists.newArrayList();
			copy.accept(new JavaRecursiveElementWalkingVisitor()
			{
				int delta = 0;

				@Override
				public void visitReferenceElement(PsiJavaCodeReferenceElement expression)
				{
					if(isParameterizedReference(expression))
					{
						return;
					}
					PsiElement qualifierExpression = expression.getQualifier();
					if(qualifierExpression instanceof PsiJavaCodeReferenceElement && ((PsiJavaCodeReferenceElement) qualifierExpression).isReferenceTo(aClass))
					{
						try
						{
							PsiElement resolved = expression.resolve();
							int end = expression.getTextRange().getEndOffset();
							qualifierExpression.delete();
							delta += end - expression.getTextRange().getEndOffset();
							PsiElement after = expression.resolve();
							if(manager.areElementsEquivalent(after, resolved))
							{
								expressionToDequalifyOffsets.add(expression.getTextRange().getStartOffset() + delta);
							}
						}
						catch(IncorrectOperationException e)
						{
							LOG.error(e);
						}
					}
					super.visitElement(expression);
				}
			});

			IntLists.reverse(expressionToDequalifyOffsets);
			for(PrimitiveIterator.OfInt iterator = expressionToDequalifyOffsets.iterator(); iterator.hasNext(); )
			{
				int offset = iterator.nextInt();
				PsiJavaCodeReferenceElement expression = PsiTreeUtil.findElementOfClassAtOffset(root, offset, PsiJavaCodeReferenceElement.class, false);
				if(expression == null)
				{
					break;
				}
				
				PsiElement qualifierExpression = expression.getQualifier();
				if(qualifierExpression instanceof PsiJavaCodeReferenceElement && ((PsiJavaCodeReferenceElement) qualifierExpression).isReferenceTo(aClass))
				{
					qualifierExpression.delete();
					HighlightManager.getInstance(project)
							.addRangeHighlight(editor, expression.getTextRange().getStartOffset(), expression.getTextRange().getEndOffset(),
									EditorColorsManager.getInstance().getGlobalScheme().getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES),
									false, null);
				}

			}
		}
	}

	@Override
	public void invoke(@Nonnull Project project, Editor editor, @Nonnull PsiElement element) throws IncorrectOperationException
	{
		invoke(project, element.getContainingFile(), editor, element);
	}

	private static boolean isParameterizedReference(final PsiJavaCodeReferenceElement expression)
	{
		if(expression.getParameterList() == null)
		{
			return false;
		}
		PsiReferenceParameterList parameterList = expression.getParameterList();
		return parameterList != null && parameterList.getFirstChild() != null;
	}
}
