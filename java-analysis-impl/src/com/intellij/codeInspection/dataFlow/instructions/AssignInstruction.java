/*
 * Copyright 2013-2017 consulo.io
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

/*
 * Created by IntelliJ IDEA.
 * User: max
 * Date: Jan 26, 2002
 * Time: 10:47:33 PM
 * To change template for new class use 
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.intellij.codeInspection.dataFlow.instructions;

import org.jetbrains.annotations.Nullable;
import com.intellij.codeInspection.dataFlow.DataFlowRunner;
import com.intellij.codeInspection.dataFlow.DfaInstructionState;
import com.intellij.codeInspection.dataFlow.DfaMemoryState;
import com.intellij.codeInspection.dataFlow.InstructionVisitor;
import com.intellij.codeInspection.dataFlow.value.DfaValue;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiVariable;

public class AssignInstruction extends Instruction
{
	private final PsiExpression myRExpression;
	@Nullable
	private final DfaValue myAssignedValue;

	public AssignInstruction(PsiExpression RExpression, @Nullable DfaValue assignedValue)
	{
		myRExpression = RExpression;
		myAssignedValue = assignedValue;
	}

	@Override
	public DfaInstructionState[] accept(DataFlowRunner runner, DfaMemoryState stateBefore, InstructionVisitor visitor)
	{
		return visitor.visitAssign(this, runner, stateBefore);
	}

	@Nullable
	public PsiExpression getRExpression()
	{
		return myRExpression;
	}

	public boolean isVariableInitializer()
	{
		return myRExpression != null && myRExpression.getParent() instanceof PsiVariable;
	}

	@Nullable
	public DfaValue getAssignedValue()
	{
		return myAssignedValue;
	}

	public String toString()
	{
		return "ASSIGN";
	}
}
