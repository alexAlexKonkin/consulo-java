/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package com.intellij.codeInspection.dataFlow;

import com.intellij.codeInspection.dataFlow.value.DfaValue;
import com.intellij.codeInspection.dataFlow.value.DfaValueFactory;
import com.intellij.codeInspection.dataFlow.value.DfaVariableValue;
import com.intellij.psi.PsiExpression;
import com.intellij.util.containers.FList;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Gregory.Shrago
 */
class ValuableDataFlowRunner extends DataFlowRunner
{
	@Nonnull
	@Override
	protected DfaMemoryState createMemoryState()
	{
		return new MyDfaMemoryState(getFactory());
	}

	static class MyDfaMemoryState extends DfaMemoryStateImpl
	{
		private MyDfaMemoryState(@Nonnull DfaValueFactory factory)
		{
			super(factory);
		}

		private MyDfaMemoryState(@Nonnull DfaMemoryStateImpl toCopy)
		{
			super(toCopy);
		}

		@Nonnull
		@Override
		public DfaMemoryStateImpl createCopy()
		{
			return new MyDfaMemoryState(this);
		}

		@Nonnull
		@Override
		protected DfaVariableState createVariableState(@Nonnull DfaVariableValue var)
		{
			return new ValuableDfaVariableState(var);
		}

		@Override
		public void flushFields()
		{
			// this is a compile time values analysis/resolve so do not flush variables
		}
	}

	static class ValuableDfaVariableState extends DfaVariableState
	{
		private final DfaValue myValue;
		@Nonnull
		final FList<PsiExpression> myConcatenation;

		private ValuableDfaVariableState(@Nonnull DfaVariableValue psiVariable)
		{
			super(psiVariable);
			myValue = null;
			myConcatenation = FList.emptyList();
		}

		private ValuableDfaVariableState(DfaValue value,
										 @Nonnull FList<PsiExpression> concatenation,
										 @Nonnull DfaFactMap factMap)
		{
			super(factMap);
			myValue = value;
			myConcatenation = concatenation;
		}

		@Nonnull
		@Override
		protected DfaVariableState createCopy(@Nonnull DfaFactMap factMap)
		{
			return new ValuableDfaVariableState(myValue, myConcatenation, factMap);
		}

		@Nonnull
		@Override
		public DfaVariableState withValue(@Nullable final DfaValue value)
		{
			if(value == myValue)
			{
				return this;
			}
			return new ValuableDfaVariableState(value, myConcatenation, myFactMap);
		}

		ValuableDfaVariableState withExpression(@Nonnull final FList<PsiExpression> concatenation)
		{
			if(concatenation == myConcatenation)
			{
				return this;
			}
			return new ValuableDfaVariableState(myValue, concatenation, myFactMap);
		}

		@Override
		public DfaValue getValue()
		{
			return myValue;
		}

		@Override
		public boolean equals(Object o)
		{
			if(this == o)
			{
				return true;
			}
			if(!(o instanceof ValuableDfaVariableState))
			{
				return false;
			}
			if(!super.equals(o))
			{
				return false;
			}

			ValuableDfaVariableState state = (ValuableDfaVariableState) o;

			if(!myConcatenation.equals(state.myConcatenation))
			{
				return false;
			}
			if(myValue != null ? !myValue.equals(state.myValue) : state.myValue != null)
			{
				return false;
			}

			return true;
		}

		@Override
		public int hashCode()
		{
			int result = super.hashCode();
			result = 31 * result + (myValue != null ? myValue.hashCode() : 0);
			result = 31 * result + myConcatenation.hashCode();
			return result;
		}
	}
}
