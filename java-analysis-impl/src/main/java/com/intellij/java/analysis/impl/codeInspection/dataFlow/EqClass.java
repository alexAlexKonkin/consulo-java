// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.java.analysis.impl.codeInspection.dataFlow;

import com.intellij.java.analysis.impl.codeInspection.dataFlow.value.DfaValue;
import com.intellij.java.analysis.impl.codeInspection.dataFlow.value.DfaValueFactory;
import com.intellij.java.analysis.impl.codeInspection.dataFlow.value.DfaVariableValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import one.util.streamex.StreamEx;

import java.util.*;

/**
 * @author peter
 */
class EqClass extends SortedIntSet implements Iterable<DfaVariableValue>
{
	private final DfaValueFactory myFactory;

	/**
	 * A comparator which allows to select a "canonical" variable of several variables (which is minimal of them).
	 * Variables with shorter qualifier chain are preferred to be canonical.
	 */
	static final Comparator<DfaVariableValue> CANONICAL_VARIABLE_COMPARATOR =
			Comparator.nullsFirst((v1, v2) -> {
				int result = EqClass.CANONICAL_VARIABLE_COMPARATOR.compare(v1.getQualifier(), v2.getQualifier());
				if(result != 0)
				{
					return result;
				}
				return Integer.compare(v1.getID(), v2.getID());
			});

	EqClass(DfaValueFactory factory)
	{
		myFactory = factory;
	}

	EqClass(@Nonnull EqClass toCopy)
	{
		super(toCopy.toNativeArray());
		myFactory = toCopy.myFactory;
	}

	@Override
	public String toString()
	{
		StringBuilder buf = new StringBuilder();
		buf.append("(");
		for(int i = 0; i < size(); i++)
		{
			if(i > 0)
			{
				buf.append(", ");
			}
			int value = get(i);
			DfaValue dfaValue = myFactory.getValue(value);
			buf.append(dfaValue);
		}
		buf.append(")");
		return buf.toString();
	}

	DfaVariableValue getVariable(int index)
	{
		return (DfaVariableValue) myFactory.getValue(get(index));
	}

	/**
	 * @return copy of variables from this class as a list. Use this method if you expect
	 * class updates during the iteration.
	 */
	List<DfaVariableValue> asList()
	{
		List<DfaVariableValue> vars = new ArrayList<>(size());
		forEach(id -> {
			vars.add((DfaVariableValue) myFactory.getValue(id));
			return true;
		});
		return vars;
	}

	/**
	 * @return the "canonical" variable for this class (according to {@link #CANONICAL_VARIABLE_COMPARATOR}) or
	 * null if the class does not contain variables.
	 */
	@Nullable
	DfaVariableValue getCanonicalVariable()
	{
		if(size() == 1)
		{
			return getVariable(0);
		}
		return StreamEx.of(iterator()).min(CANONICAL_VARIABLE_COMPARATOR).orElse(null);
	}

	@Nonnull
	@Override
	public Iterator<DfaVariableValue> iterator()
	{
		return new Iterator<>()
		{
			int pos;

			@Override
			public boolean hasNext()
			{
				return pos < size();
			}

			@Override
			public DfaVariableValue next()
			{
				if(pos >= size())
				{
					throw new NoSuchElementException();
				}
				return (DfaVariableValue) myFactory.getValue(get(pos++));
			}
		};
	}
}
