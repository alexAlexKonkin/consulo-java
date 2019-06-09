/*
 * Copyright 2003-2011 Dave Griffith, Bas Leijdekkers
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
package com.siyeh.ig.bugs;

import com.intellij.codeInspection.ui.ListTable;
import com.intellij.codeInspection.ui.ListWrappingTableModel;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.ui.UIUtil;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.psiutils.CollectionUtils;
import com.siyeh.ig.psiutils.ParenthesesUtils;
import com.siyeh.ig.psiutils.VariableAccessUtils;
import com.siyeh.ig.ui.ExternalizableStringSet;
import com.siyeh.ig.ui.UiUtils;
import consulo.java.module.util.JavaClassNames;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

public class MismatchedCollectionQueryUpdateInspection extends BaseInspection
{
	@SuppressWarnings({"PublicField"})
	public final ExternalizableStringSet queryNames =
			new ExternalizableStringSet("copyInto", "drainTo", "propertyNames", "save", "store", "write");
	@SuppressWarnings({"PublicField"})
	public final ExternalizableStringSet updateNames =
			new ExternalizableStringSet("add", "clear", "drainTo", "insert", "load", "offer", "poll", "push", "put", "remove", "replace",
					"retain", "set", "take");

	public MismatchedCollectionQueryUpdateInspection()
	{
	}

	@Override
	@Nonnull
	public String getID()
	{
		return "MismatchedQueryAndUpdateOfCollection";
	}

	@Override
	@Nonnull
	public String getDisplayName()
	{
		return InspectionGadgetsBundle.message("mismatched.update.collection.display.name");
	}

	@Override
	@Nonnull
	public String buildErrorString(Object... infos)
	{
		final boolean updated = ((Boolean) infos[0]).booleanValue();
		if(updated)
		{
			return InspectionGadgetsBundle.message("mismatched.update.collection.problem.descriptor.updated.not.queried");
		}
		else
		{
			return InspectionGadgetsBundle.message("mismatched.update.collection.problem.description.queried.not.updated");
		}
	}

	@Override
	public JComponent createOptionsPanel()
	{
		final JPanel panel = new JPanel(new GridLayout(1, 2, UIUtil.DEFAULT_HGAP, UIUtil.DEFAULT_VGAP));
		final ListTable table1 = new ListTable(new ListWrappingTableModel(queryNames, InspectionGadgetsBundle.message("query.column.name")));
		final JPanel tablePanel1 = UiUtils.createAddRemovePanel(table1);

		final ListTable table2 = new ListTable(new ListWrappingTableModel(updateNames, InspectionGadgetsBundle.message("update.column.name")));
		final JPanel tablePanel2 = UiUtils.createAddRemovePanel(table2);

		panel.add(tablePanel1);
		panel.add(tablePanel2);
		return panel;
	}

	@Override
	public boolean isEnabledByDefault()
	{
		return true;
	}

	@Override
	public boolean runForWholeFile()
	{
		return true;
	}

	@Override
	public BaseInspectionVisitor buildVisitor()
	{
		return new MismatchedCollectionQueryUpdateVisitor();
	}

	private static boolean isEmptyCollectionInitializer(PsiExpression initializer)
	{
		if(!(initializer instanceof PsiNewExpression))
		{
			return false;
		}
		final PsiNewExpression newExpression = (PsiNewExpression) initializer;
		final PsiExpressionList argumentList = newExpression.getArgumentList();
		if(argumentList == null)
		{
			return false;
		}
		final PsiExpression[] arguments = argumentList.getExpressions();
		for(final PsiExpression argument : arguments)
		{
			final PsiType argumentType = argument.getType();
			if(argumentType == null)
			{
				return false;
			}
			if(CollectionUtils.isCollectionClassOrInterface(argumentType))
			{
				return false;
			}
			if(argumentType instanceof PsiArrayType)
			{
				return false;
			}
		}
		return true;
	}

	private class MismatchedCollectionQueryUpdateVisitor extends BaseInspectionVisitor
	{

		@Override
		public void visitField(@Nonnull PsiField field)
		{
			super.visitField(field);
			if(!field.hasModifierProperty(PsiModifier.PRIVATE))
			{
				return;
			}
			final PsiClass containingClass = PsiUtil.getTopLevelClass(field);
			if(!checkVariable(field, containingClass))
			{
				return;
			}
			final boolean written = collectionContentsAreUpdated(field, containingClass);
			final boolean read = collectionContentsAreQueried(field, containingClass);
			if(read == written)
			{
				return;
			}
			registerFieldError(field, Boolean.valueOf(written));
		}

		@Override
		public void visitLocalVariable(@Nonnull PsiLocalVariable variable)
		{
			super.visitLocalVariable(variable);
			final PsiCodeBlock codeBlock = PsiTreeUtil.getParentOfType(variable, PsiCodeBlock.class);
			if(!checkVariable(variable, codeBlock))
			{
				return;
			}
			final boolean written = collectionContentsAreUpdated(variable, codeBlock);
			final boolean read = collectionContentsAreQueried(variable, codeBlock);
			if(read != written)
			{
				registerVariableError(variable, Boolean.valueOf(written));
			}
		}

		private boolean checkVariable(PsiVariable variable, PsiElement context)
		{
			if(context == null)
			{
				return false;
			}
			final PsiType type = variable.getType();
			if(!CollectionUtils.isCollectionClassOrInterface(type))
			{
				return false;
			}
			if(VariableAccessUtils.variableIsAssignedFrom(variable, context))
			{
				return false;
			}
			if(VariableAccessUtils.variableIsReturned(variable, context))
			{
				return false;
			}
			if(VariableAccessUtils.variableIsPassedAsMethodArgument(variable, context))
			{
				return false;
			}
			return !VariableAccessUtils.variableIsUsedInArrayInitializer(variable, context);
		}

		private boolean collectionContentsAreUpdated(PsiVariable variable, PsiElement context)
		{
			if(collectionUpdateCalled(variable, context))
			{
				return true;
			}
			final PsiExpression initializer = variable.getInitializer();
			if(initializer != null && !isEmptyCollectionInitializer(initializer))
			{
				return true;
			}
			if(initializer instanceof PsiNewExpression)
			{
				final PsiNewExpression newExpression = (PsiNewExpression) initializer;
				final PsiAnonymousClass anonymousClass = newExpression.getAnonymousClass();
				if(anonymousClass != null)
				{
					if(collectionUpdateCalled(null, anonymousClass))
					{
						return true;
					}
					final ThisPassedAsArgumentVisitor visitor = new ThisPassedAsArgumentVisitor();
					anonymousClass.accept(visitor);
					if(visitor.isPassed())
					{
						return true;
					}
				}
			}
			return VariableAccessUtils.variableIsAssigned(variable, context);
		}

		private boolean collectionContentsAreQueried(PsiVariable variable, PsiElement context)
		{
			if(collectionQueryCalled(variable, context))
			{
				return true;
			}
			final PsiExpression initializer = variable.getInitializer();
			if(initializer != null && !isEmptyCollectionInitializer(initializer))
			{
				return true;
			}
			return collectionQueriedByAssignment(variable, context);
		}

		private boolean collectionQueryCalled(PsiVariable variable, PsiElement context)
		{
			final CollectionQueryCalledVisitor visitor = new CollectionQueryCalledVisitor(variable, queryNames);
			context.accept(visitor);
			return visitor.isQueried();
		}

		private boolean collectionUpdateCalled(@Nullable PsiVariable variable, PsiElement context)
		{
			final CollectionUpdateCalledVisitor visitor = new CollectionUpdateCalledVisitor(variable, updateNames);
			context.accept(visitor);
			return visitor.isUpdated();
		}
	}

	private static boolean collectionQueriedByAssignment(@Nonnull PsiVariable variable, @Nonnull PsiElement context)
	{
		final CollectionQueriedByAssignmentVisitor visitor = new CollectionQueriedByAssignmentVisitor(variable);
		context.accept(visitor);
		return visitor.mayBeQueried();
	}

	private static class CollectionQueriedByAssignmentVisitor extends JavaRecursiveElementVisitor
	{

		private boolean mayBeQueried = false;
		@Nonnull
		private final PsiVariable variable;

		CollectionQueriedByAssignmentVisitor(@Nonnull PsiVariable variable)
		{
			this.variable = variable;
		}

		@Override
		public void visitElement(@Nonnull PsiElement element)
		{
			if(mayBeQueried)
			{
				return;
			}
			super.visitElement(element);
		}

		@Override
		public void visitReferenceExpression(PsiReferenceExpression expression)
		{
			if(mayBeQueried)
			{
				return;
			}
			super.visitReferenceExpression(expression);
			final PsiElement parent = ParenthesesUtils.getParentSkipParentheses(expression);
			if(!(parent instanceof PsiPolyadicExpression))
			{
				return;
			}
			final PsiPolyadicExpression polyadicExpression = (PsiPolyadicExpression) parent;
			final IElementType tokenType = polyadicExpression.getOperationTokenType();
			if(JavaTokenType.PLUS != tokenType)
			{
				return;
			}
			final PsiElement target = expression.resolve();
			if(!variable.equals(target))
			{
				return;
			}
			final PsiType type = polyadicExpression.getType();
			if(type == null || !type.equalsToText(JavaClassNames.JAVA_LANG_STRING))
			{
				return;
			}
			mayBeQueried = true; // query by concatenation ("" + list)
		}

		@Override
		public void visitAssignmentExpression(@Nonnull PsiAssignmentExpression assignment)
		{
			if(mayBeQueried)
			{
				return;
			}
			super.visitAssignmentExpression(assignment);
			final PsiExpression lhs = assignment.getLExpression();
			if(!VariableAccessUtils.mayEvaluateToVariable(lhs, variable))
			{
				return;
			}
			final PsiExpression rhs = assignment.getRExpression();
			if(isEmptyCollectionInitializer(rhs))
			{
				return;
			}
			mayBeQueried = true;
		}

		public boolean mayBeQueried()
		{
			return mayBeQueried;
		}
	}
}
