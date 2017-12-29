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
 * Date: Jan 28, 2002
 * Time: 9:40:01 PM
 * To change template for new class use 
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.intellij.codeInspection.dataFlow;

import org.jetbrains.annotations.NotNull;
import com.intellij.codeInspection.dataFlow.instructions.Instruction;

public class DfaInstructionState implements Comparable<DfaInstructionState>
{
	public static final DfaInstructionState[] EMPTY_ARRAY = new DfaInstructionState[0];
	private final DfaMemoryState myBeforeMemoryState;
	private final Instruction myInstruction;

	public DfaInstructionState(@NotNull Instruction myInstruction, @NotNull DfaMemoryState myBeforeMemoryState)
	{
		this.myBeforeMemoryState = myBeforeMemoryState;
		this.myInstruction = myInstruction;
	}

	@NotNull
	public Instruction getInstruction()
	{
		return myInstruction;
	}

	@NotNull
	public DfaMemoryState getMemoryState()
	{
		return myBeforeMemoryState;
	}

	public String toString()
	{
		return getInstruction().getIndex() + " " + getInstruction() + ":   " + getMemoryState().toString();
	}

	@Override
	public int compareTo(@NotNull DfaInstructionState o)
	{
		return myInstruction.getIndex() - o.myInstruction.getIndex();
	}
}