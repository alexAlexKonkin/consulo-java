/*
 * Copyright 2003-2016 Dave Griffith, Bas Leijdekkers
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
package com.intellij.java.impl.ig.controlflow;

import javax.annotation.Nonnull;

import com.intellij.java.language.psi.PsiDoWhileStatement;
import com.intellij.java.language.psi.PsiForStatement;
import com.intellij.java.language.psi.PsiStatement;
import com.intellij.java.language.psi.PsiWhileStatement;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.psiutils.ControlFlowUtils;
import consulo.annotation.component.ExtensionImpl;

@ExtensionImpl
public class InfiniteLoopStatementInspection extends BaseInspection
{

	@Override
	@Nonnull
	public String getDisplayName()
	{
		return InspectionGadgetsBundle.message("infinite.loop.statement.display.name");
	}

	@Override
	public boolean isEnabledByDefault()
	{
		return true;
	}

	@Override
	@Nonnull
	protected String buildErrorString(Object... infos)
	{
		return InspectionGadgetsBundle.message("infinite.loop.statement.problem.descriptor");
	}

	@Override
	public BaseInspectionVisitor buildVisitor()
	{
		return new InfiniteLoopStatementsVisitor();
	}

	private static class InfiniteLoopStatementsVisitor extends BaseInspectionVisitor
	{

		@Override
		public void visitForStatement(@Nonnull PsiForStatement statement)
		{
			super.visitForStatement(statement);
			checkStatement(statement);
		}

		@Override
		public void visitWhileStatement(@Nonnull PsiWhileStatement statement)
		{
			super.visitWhileStatement(statement);
			checkStatement(statement);
		}

		@Override
		public void visitDoWhileStatement(@Nonnull PsiDoWhileStatement statement)
		{
			super.visitDoWhileStatement(statement);
			checkStatement(statement);
		}

		private void checkStatement(PsiStatement statement)
		{
			if(ControlFlowUtils.statementMayCompleteNormally(statement))
			{
				return;
			}
			if(ControlFlowUtils.containsReturn(statement))
			{
				return;
			}
			if(ControlFlowUtils.containsSystemExit(statement))
			{
				return;
			}
			registerStatementError(statement);
		}
	}
}