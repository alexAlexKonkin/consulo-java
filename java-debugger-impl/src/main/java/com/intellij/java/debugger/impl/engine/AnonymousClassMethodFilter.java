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
package com.intellij.java.debugger.impl.engine;

import com.intellij.java.debugger.SourcePosition;
import com.intellij.java.language.psi.PsiCodeBlock;
import com.intellij.java.language.psi.PsiMethod;
import com.intellij.java.language.psi.PsiStatement;
import consulo.util.lang.Range;

import jakarta.annotation.Nullable;

/**
 * @author Eugene Zhuravlev
 *         Date: 10/26/13
 */
public class AnonymousClassMethodFilter extends BasicStepMethodFilter implements BreakpointStepMethodFilter
{
	@Nullable
	private final SourcePosition myBreakpointPosition;
	private final int myLastStatementLine;

	public AnonymousClassMethodFilter(PsiMethod psiMethod, Range<Integer> lines)
	{
		super(psiMethod, lines);
		SourcePosition firstStatementPosition = null;
		SourcePosition lastStatementPosition = null;
		final PsiCodeBlock body = psiMethod.getBody();
		if(body != null)
		{
			final PsiStatement[] statements = body.getStatements();
			if(statements.length > 0)
			{
				firstStatementPosition = SourcePosition.createFromElement(statements[0]);
				if(firstStatementPosition != null)
				{
					final PsiStatement lastStatement = statements[statements.length - 1];
					lastStatementPosition = SourcePosition.createFromOffset(firstStatementPosition.getFile(), lastStatement.getTextRange().getEndOffset());
				}
			}
		}
		myBreakpointPosition = firstStatementPosition;
		myLastStatementLine = lastStatementPosition != null ? lastStatementPosition.getLine() : -1;
	}

	@Override
	@Nullable
	public SourcePosition getBreakpointPosition()
	{
		return myBreakpointPosition;
	}

	@Override
	public int getLastStatementLine()
	{
		return myLastStatementLine;
	}
}
