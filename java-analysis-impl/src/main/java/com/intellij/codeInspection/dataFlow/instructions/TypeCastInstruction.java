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

package com.intellij.codeInspection.dataFlow.instructions;

import com.intellij.codeInspection.dataFlow.*;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeCastExpression;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TypeCastInstruction extends Instruction implements ExpressionPushingInstruction
{
	private final PsiTypeCastExpression myCastExpression;
	private final PsiExpression myCasted;
	private final PsiType myCastTo;
	private final
	@Nullable
	DfaControlTransferValue myTransferValue;

	public TypeCastInstruction(PsiTypeCastExpression castExpression,
							   PsiExpression casted,
							   PsiType castTo,
							   @Nullable DfaControlTransferValue value)
	{
		assert !(castTo instanceof PsiPrimitiveType);
		myCastExpression = castExpression;
		myCasted = casted;
		myCastTo = castTo;
		myTransferValue = value;
	}

	@Nullable
	public DfaControlTransferValue getCastExceptionTransfer()
	{
		return myTransferValue;
	}

	public PsiExpression getCasted()
	{
		return myCasted;
	}

	public PsiType getCastTo()
	{
		return myCastTo;
	}

	@Override
	public DfaInstructionState[] accept(DataFlowRunner runner, DfaMemoryState stateBefore, InstructionVisitor visitor)
	{
		return visitor.visitTypeCast(this, runner, stateBefore);
	}

	@Override
	public String toString()
	{
		return "CAST_TO " + myCastTo.getCanonicalText();
	}

	@Nonnull
	@Override
	public PsiTypeCastExpression getExpression()
	{
		return myCastExpression;
	}
}
