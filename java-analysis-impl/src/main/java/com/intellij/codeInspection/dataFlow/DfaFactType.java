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
package com.intellij.codeInspection.dataFlow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.intellij.codeInspection.dataFlow.rangeSet.LongRangeSet;
import com.intellij.codeInspection.dataFlow.value.DfaBoxedValue;
import com.intellij.codeInspection.dataFlow.value.DfaConstValue;
import com.intellij.codeInspection.dataFlow.value.DfaOptionalValue;
import com.intellij.codeInspection.dataFlow.value.DfaRangeValue;
import com.intellij.codeInspection.dataFlow.value.DfaTypeValue;
import com.intellij.codeInspection.dataFlow.value.DfaUnboxedValue;
import com.intellij.codeInspection.dataFlow.value.DfaValue;
import com.intellij.codeInspection.dataFlow.value.DfaVariableValue;
import com.intellij.openapi.util.Key;

/**
 * A type of the fact which restricts some value.
 *
 * @author Tagir Valeev
 */
public abstract class DfaFactType<T> extends Key<T>
{
	private static final List<DfaFactType<?>> ourFactTypes = new ArrayList<>();

	/**
	 * This fact specifies whether the value can be null. The absence of the fact means that the nullability is unknown.
	 */
	public static final DfaFactType<Boolean> CAN_BE_NULL = new DfaFactType<Boolean>("Can be null")
	{
		@Override
		String toString(Boolean fact)
		{
			return fact ? "Nullable" : "NotNull";
		}

		@Nullable
		@Override
		Boolean fromDfaValue(DfaValue value)
		{
			if(value instanceof DfaConstValue)
			{
				return ((DfaConstValue) value).getValue() == null;
			}
			if(value instanceof DfaBoxedValue || value instanceof DfaUnboxedValue || value instanceof DfaRangeValue)
			{
				return false;
			}
			if(value instanceof DfaTypeValue)
			{
				return NullnessUtil.toBoolean(((DfaTypeValue) value).getNullness());
			}
			return null;
		}

		@Nullable
		@Override
		Boolean calcFromVariable(@Nonnull DfaVariableValue value)
		{
			return NullnessUtil.calcCanBeNull(value);
		}
	};

	/**
	 * This fact is applied to the Optional values (like {@link java.util.Optional} or Guava Optional).
	 * When its value is true, then optional is known to be present.
	 * When its value is false, then optional is known to be empty (absent).
	 */
	public static final DfaFactType<Boolean> OPTIONAL_PRESENCE = new DfaFactType<Boolean>("Optional presense")
	{
		@Override
		String toString(Boolean fact)
		{
			return fact ? "present Optional" : "absent Optional";
		}

		@Nullable
		@Override
		Boolean fromDfaValue(DfaValue value)
		{
			return value instanceof DfaOptionalValue ? ((DfaOptionalValue) value).isPresent() : null;
		}
	};

	/**
	 * This fact is applied to the integral values (of types byte, char, short, int, long).
	 * Its value represents a range of possible values.
	 */
	public static final DfaFactType<LongRangeSet> RANGE = new DfaFactType<LongRangeSet>("Range")
	{
		@Override
		boolean isSuper(@Nonnull LongRangeSet superFact, @Nonnull LongRangeSet subFact)
		{
			return superFact.contains(subFact);
		}

		@Nullable
		@Override
		LongRangeSet fromDfaValue(DfaValue value)
		{
			if(value instanceof DfaVariableValue)
			{
				return calcFromVariable((DfaVariableValue) value);
			}
			return LongRangeSet.fromDfaValue(value);
		}

		@Nullable
		@Override
		LongRangeSet calcFromVariable(@Nonnull DfaVariableValue var)
		{
			if(var.getQualifier() != null)
			{
				for(SpecialField sf : SpecialField.values())
				{
					if(sf.isMyAccessor(var.getPsiVariable()))
					{
						return sf.getRange();
					}
				}
			}
			return LongRangeSet.fromType(var.getVariableType());
		}

		@Nullable
		@Override
		LongRangeSet intersectFacts(@Nonnull LongRangeSet left, @Nonnull LongRangeSet right)
		{
			LongRangeSet intersection = left.intersect(right);
			return intersection.isEmpty() ? null : intersection;
		}

		@Override
		String toString(LongRangeSet fact)
		{
			return fact.toString();
		}
	};

	private DfaFactType(String name)
	{
		super("DfaFactType: " + name);
		// Thread-safe as all DfaFactType instances are created only from DfaFactType class static initializer
		ourFactTypes.add(this);
	}

	@Nullable
	T fromDfaValue(DfaValue value)
	{
		return null;
	}

	// Could be expensive
	@Nullable
	T calcFromVariable(@Nonnull DfaVariableValue value)
	{
		return null;
	}

	boolean isSuper(@Nonnull T superFact, @Nonnull T subFact)
	{
		return false;
	}

	/**
	 * Intersects two facts of this type.
	 *
	 * @param left  left fact
	 * @param right right fact
	 * @return intersection fact or null if facts are incompatible
	 */
	@javax.annotation.Nullable
	T intersectFacts(@Nonnull T left, @Nonnull T right)
	{
		return left.equals(right) ? left : null;
	}

	String toString(T fact)
	{
		return fact.toString();
	}

	static List<DfaFactType<?>> getTypes()
	{
		return Collections.unmodifiableList(ourFactTypes);
	}
}
