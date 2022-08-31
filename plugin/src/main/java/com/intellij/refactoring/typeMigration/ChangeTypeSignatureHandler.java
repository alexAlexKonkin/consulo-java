// Copyright 2000-2017 JetBrains s.r.o.
// Use of this source code is governed by the Apache 2.0 license that can be
// found in the LICENSE file.
package com.intellij.refactoring.typeMigration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import com.intellij.java.language.psi.*;
import com.intellij.openapi.actionSystem.DataContext;
import consulo.logging.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.typeMigration.ui.TypeMigrationDialog;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import consulo.codeInsight.TargetElementUtil;

public class ChangeTypeSignatureHandler implements RefactoringActionHandler
{
	private static final Logger LOG = Logger.getInstance(ChangeTypeSignatureHandler.class);

	public static final String REFACTORING_NAME = "Type Migration";

	@Override
	public void invoke(@Nonnull Project project, Editor editor, PsiFile file, DataContext dataContext)
	{
		editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
		final int offset = TargetElementUtil.adjustOffset(file, editor.getDocument(), editor.getCaretModel().getOffset());
		final PsiElement element = file.findElementAt(offset);
		PsiTypeElement typeElement = PsiTreeUtil.getParentOfType(element, PsiTypeElement.class);
		while(typeElement != null)
		{
			final PsiElement parent = typeElement.getParent();
			PsiElement[] toMigrate = null;
			if(parent instanceof PsiVariable)
			{
				toMigrate = extractReferencedVariables(typeElement);
			}
			else if((parent instanceof PsiMember && !(parent instanceof PsiClass)) || isClassArgument(parent))
			{
				toMigrate = new PsiElement[]{parent};
			}
			if(toMigrate != null && toMigrate.length > 0)
			{
				invoke(project, toMigrate, editor);
				return;
			}
			typeElement = PsiTreeUtil.getParentOfType(parent, PsiTypeElement.class, false);
		}
		CommonRefactoringUtil.showErrorHint(project, editor, "The caret should be positioned on type of field, variable, method or method parameter to be refactored", REFACTORING_NAME, "refactoring" +
				".migrateType");
	}

	@Override
	public void invoke(@Nonnull final Project project, @Nonnull final PsiElement[] elements, final DataContext dataContext)
	{
		LOG.assertTrue(elements.length == 1);
		final PsiElement element = elements[0];
		invokeOnElement(project, element);
	}

	private static void invokeOnElement(final Project project, final PsiElement element)
	{
		if(element instanceof PsiVariable || (element instanceof PsiMember && !(element instanceof PsiClass)) || element instanceof PsiFile || isClassArgument(element))
		{
			invoke(project, new PsiElement[]{element}, (Editor) null);
		}
	}

	private static boolean isClassArgument(PsiElement element)
	{
		if(element instanceof PsiReferenceParameterList)
		{
			final PsiMember member = PsiTreeUtil.getParentOfType(element, PsiMember.class);
			if(member instanceof PsiAnonymousClass)
			{
				return ((PsiAnonymousClass) member).getBaseClassReference().getParameterList() == element;
			}
			if(member instanceof PsiClass)
			{
				final PsiReferenceList implementsList = ((PsiClass) member).getImplementsList();
				final PsiReferenceList extendsList = ((PsiClass) member).getExtendsList();
				return PsiTreeUtil.isAncestor(implementsList, element, false) || PsiTreeUtil.isAncestor(extendsList, element, false);
			}
		}
		return false;
	}

	private static void invoke(@Nonnull Project project, @Nonnull PsiElement[] roots, @javax.annotation.Nullable Editor editor)
	{
		if(Util.canBeMigrated(roots))
		{
			TypeMigrationDialog dialog = new TypeMigrationDialog.SingleElement(project, roots);
			dialog.show();
			return;
		}

		CommonRefactoringUtil.showErrorHint(project, editor, RefactoringBundle.message("only.fields.variables.of.methods.of.valid.type.can.be.considered"), RefactoringBundle.message("unable.to.start" +
				".type.migration"), null);
	}

	@Nonnull
	private static PsiElement[] extractReferencedVariables(@Nonnull PsiTypeElement typeElement)
	{
		final PsiElement parent = typeElement.getParent();
		if(parent instanceof PsiVariable)
		{
			if(parent instanceof PsiField)
			{
				PsiField aField = (PsiField) parent;
				List<PsiField> fields = new ArrayList<>();
				while(true)
				{
					fields.add(aField);
					aField = PsiTreeUtil.getNextSiblingOfType(aField, PsiField.class);
					if(aField == null || aField.getTypeElement() != typeElement)
					{
						return fields.toArray(PsiElement.EMPTY_ARRAY);
					}
				}
			}
			else if(parent instanceof PsiLocalVariable)
			{
				final PsiDeclarationStatement declaration = PsiTreeUtil.getParentOfType(parent, PsiDeclarationStatement.class);
				if(declaration != null)
				{
					return Arrays.stream(declaration.getDeclaredElements()).filter(PsiVariable.class::isInstance).toArray(PsiVariable[]::new);
				}
			}
			return new PsiElement[]{parent};
		}
		else
		{
			return PsiElement.EMPTY_ARRAY;
		}
	}
}