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
package com.intellij.ide.hierarchy.call;

import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.swing.Icon;

import com.intellij.codeInsight.highlighting.HighlightManager;
import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.hierarchy.HierarchyNodeDescriptor;
import com.intellij.ide.hierarchy.JavaHierarchyUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.util.CompositeAppearance;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.TextRange;
import com.intellij.pom.Navigatable;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.java.language.psi.PsiMember;
import com.intellij.java.language.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.java.language.psi.PsiSubstitutor;
import com.intellij.psi.SyntheticElement;
import com.intellij.java.language.impl.psi.presentation.java.ClassPresentationUtil;
import com.intellij.java.language.psi.util.PsiFormatUtil;
import com.intellij.psi.util.PsiFormatUtilBase;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilBase;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.awt.TargetAWT;
import consulo.ide.IconDescriptorUpdaters;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;

public final class CallHierarchyNodeDescriptor extends HierarchyNodeDescriptor implements Navigatable
{
	private int myUsageCount = 1;
	private final List<PsiReference> myReferences = new ArrayList<PsiReference>();
	private final boolean myNavigateToReference;

	public CallHierarchyNodeDescriptor(@Nonnull Project project, final HierarchyNodeDescriptor parentDescriptor, @Nonnull PsiElement element, final boolean isBase, final boolean navigateToReference)
	{
		super(project, parentDescriptor, element, isBase);
		myNavigateToReference = navigateToReference;
	}

	/**
	 * @return PsiMethod or PsiClass or JspFile
	 */
	public final PsiMember getEnclosingElement()
	{
		PsiElement element = getPsiElement();
		return element == null ? null : getEnclosingElement(element);
	}

	public static PsiMember getEnclosingElement(final PsiElement element)
	{
		return PsiTreeUtil.getNonStrictParentOfType(element, PsiMethod.class, PsiClass.class);
	}

	public final void incrementUsageCount()
	{
		myUsageCount++;
	}

	/**
	 * Element for OpenFileDescriptor
	 */
	public final PsiElement getTargetElement()
	{
		return getPsiElement();
	}

	@Override
	public final boolean isValid()
	{
		return getEnclosingElement() != null;
	}

	@Override
	@RequiredUIAccess
	public final boolean update()
	{
		final CompositeAppearance oldText = myHighlightedText;
		final Icon oldIcon = TargetAWT.to(getIcon());

		int flags = Iconable.ICON_FLAG_VISIBILITY;
		if(isMarkReadOnly())
		{
			flags |= Iconable.ICON_FLAG_READ_STATUS;
		}

		boolean changes = super.update();

		final PsiElement enclosingElement = getEnclosingElement();

		if(enclosingElement == null)
		{
			final String invalidPrefix = IdeBundle.message("node.hierarchy.invalid");
			if(!myHighlightedText.getText().startsWith(invalidPrefix))
			{
				myHighlightedText.getBeginning().addText(invalidPrefix, HierarchyNodeDescriptor.getInvalidPrefixAttributes());
			}
			return true;
		}

		Image newIcon = IconDescriptorUpdaters.getIcon(enclosingElement, flags);
		if(changes && myIsBase)
		{
			newIcon = ImageEffects.appendRight(AllIcons.Hierarchy.Base, newIcon);
		}
		setIcon(newIcon);

		myHighlightedText = new CompositeAppearance();
		TextAttributes mainTextAttributes = null;
		if(myColor != null)
		{
			mainTextAttributes = new TextAttributes(myColor, null, null, null, Font.PLAIN);
		}
		if(enclosingElement instanceof PsiMethod)
		{
			if(enclosingElement instanceof SyntheticElement)
			{
				PsiFile file = enclosingElement.getContainingFile();
				myHighlightedText.getEnding().addText(file != null ? file.getName() : IdeBundle.message("node.call.hierarchy.unknown.jsp"), mainTextAttributes);
			}
			else
			{
				final PsiMethod method = (PsiMethod) enclosingElement;
				final StringBuilder buffer = new StringBuilder(128);
				final PsiClass containingClass = method.getContainingClass();
				if(containingClass != null)
				{
					buffer.append(ClassPresentationUtil.getNameForClass(containingClass, false));
					buffer.append('.');
				}
				final String methodText = PsiFormatUtil.formatMethod(method, PsiSubstitutor.EMPTY, PsiFormatUtilBase.SHOW_NAME | PsiFormatUtilBase.SHOW_PARAMETERS, PsiFormatUtilBase.SHOW_TYPE);
				buffer.append(methodText);

				myHighlightedText.getEnding().addText(buffer.toString(), mainTextAttributes);
			}
		}
		else
		{
			myHighlightedText.getEnding().addText(ClassPresentationUtil.getNameForClass((PsiClass) enclosingElement, false), mainTextAttributes);
		}

		if(myUsageCount > 1)
		{
			myHighlightedText.getEnding().addText(IdeBundle.message("node.call.hierarchy.N.usages", myUsageCount), HierarchyNodeDescriptor.getUsageCountPrefixAttributes());
		}

		final PsiClass containingClass = enclosingElement instanceof PsiMethod ? ((PsiMethod) enclosingElement).getContainingClass() : (PsiClass) enclosingElement;
		if(containingClass != null)
		{
			final String packageName = JavaHierarchyUtil.getPackageName(containingClass);
			myHighlightedText.getEnding().addText("  (" + packageName + ")", HierarchyNodeDescriptor.getPackageNameAttributes());
		}

		myName = myHighlightedText.getText();

		if(!Comparing.equal(myHighlightedText, oldText) || !Comparing.equal(getIcon(), oldIcon))
		{
			changes = true;
		}
		return changes;
	}

	public void addReference(final PsiReference reference)
	{
		myReferences.add(reference);
	}

	public boolean hasReference(PsiReference reference)
	{
		return myReferences.contains(reference);
	}

	@Override
	public void navigate(boolean requestFocus)
	{
		if(!myNavigateToReference)
		{
			PsiElement element = getPsiElement();
			if(element instanceof Navigatable && ((Navigatable) element).canNavigate())
			{
				((Navigatable) element).navigate(requestFocus);
			}
			return;
		}

		final PsiReference firstReference = myReferences.get(0);
		final PsiElement element = firstReference.getElement();
		if(element == null)
		{
			return;
		}
		final PsiElement callElement = element.getParent();
		if(callElement instanceof Navigatable && ((Navigatable) callElement).canNavigate())
		{
			((Navigatable) callElement).navigate(requestFocus);
		}
		else
		{
			final PsiFile psiFile = callElement.getContainingFile();
			if(psiFile == null || psiFile.getVirtualFile() == null)
			{
				return;
			}
			FileEditorManager.getInstance(myProject).openFile(psiFile.getVirtualFile(), requestFocus);
		}

		Editor editor = PsiUtilBase.findEditor(callElement);

		if(editor != null)
		{

			HighlightManager highlightManager = HighlightManager.getInstance(myProject);
			EditorColorsManager colorManager = EditorColorsManager.getInstance();
			TextAttributes attributes = colorManager.getGlobalScheme().getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES);
			ArrayList<RangeHighlighter> highlighters = new ArrayList<RangeHighlighter>();
			for(PsiReference psiReference : myReferences)
			{
				final PsiElement eachElement = psiReference.getElement();
				if(eachElement != null)
				{
					final PsiElement eachMethodCall = eachElement.getParent();
					if(eachMethodCall != null)
					{
						final TextRange textRange = eachMethodCall.getTextRange();
						highlightManager.addRangeHighlight(editor, textRange.getStartOffset(), textRange.getEndOffset(), attributes, false, highlighters);
					}
				}
			}
		}
	}

	@Override
	public boolean canNavigate()
	{
		if(!myNavigateToReference)
		{
			PsiElement element = getPsiElement();
			return element instanceof Navigatable && ((Navigatable) element).canNavigate();
		}
		if(myReferences.isEmpty())
		{
			return false;
		}
		final PsiReference firstReference = myReferences.get(0);
		final PsiElement callElement = firstReference.getElement().getParent();
		if(callElement == null || !callElement.isValid())
		{
			return false;
		}
		if(!(callElement instanceof Navigatable) || !((Navigatable) callElement).canNavigate())
		{
			final PsiFile psiFile = callElement.getContainingFile();
			if(psiFile == null)
			{
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean canNavigateToSource()
	{
		return canNavigate();
	}
}
