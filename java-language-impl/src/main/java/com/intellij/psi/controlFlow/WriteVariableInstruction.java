/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.psi.controlFlow;

import com.intellij.java.language.psi.PsiVariable;
import javax.annotation.Nonnull;

public final class WriteVariableInstruction extends SimpleInstruction
{
	@Nonnull
	public final PsiVariable variable;

	WriteVariableInstruction(@Nonnull PsiVariable variable)
	{
		this.variable = variable;
	}

	public String toString()
	{
		return "WRITE " + variable.getName();
	}

	@Override
	public void accept(@Nonnull ControlFlowInstructionVisitor visitor, int offset, int nextOffset)
	{
		visitor.visitWriteVariableInstruction(this, offset, nextOffset);
	}

	@Override
	public boolean equals(Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(o == null || getClass() != o.getClass())
		{
			return false;
		}
		return variable.equals(((WriteVariableInstruction) o).variable);
	}

	@Override
	public int hashCode()
	{
		return 753 + variable.hashCode();
	}
}
