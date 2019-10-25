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

import com.intellij.codeInsight.Nullability;
import com.intellij.codeInspection.dataFlow.rangeSet.LongRangeSet;
import com.intellij.codeInspection.dataFlow.value.*;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
	public static final DfaFactType<DfaNullability> NULLABILITY = new DfaFactType<DfaNullability>("Nullability")
	{
		@Nonnull
		@Override
		public String toString(@Nonnull DfaNullability fact)
		{
			return fact.getInternalName();
		}

		@Override
		boolean isUnknown(@Nonnull DfaNullability fact)
		{
			return fact == DfaNullability.UNKNOWN;
		}

		@Nonnull
		@Override
		public String getPresentationText(@Nonnull DfaNullability fact, @Nullable PsiType type)
		{
			if(type instanceof PsiPrimitiveType)
			{
				return "";
			}
			return fact.getPresentationName();
		}

		@Override
		boolean isSuper(@Nullable DfaNullability superFact, @Nullable DfaNullability subFact)
		{
			return (superFact == null && (subFact == DfaNullability.NOT_NULL || subFact == DfaNullability.FLUSHED)) ||
					super.isSuper(superFact, subFact);
		}

		@Nonnull
		@Override
		DfaNullability uniteFacts(@Nonnull DfaNullability left, @Nonnull DfaNullability right)
		{
			if(left == right)
			{
				return left;
			}
			if(left == DfaNullability.NULL || right == DfaNullability.NULL ||
					left == DfaNullability.NULLABLE || right == DfaNullability.NULLABLE)
			{
				return DfaNullability.NULLABLE;
			}
			if(left == DfaNullability.FLUSHED || right == DfaNullability.FLUSHED)
			{
				return DfaNullability.FLUSHED;
			}
			return DfaNullability.UNKNOWN;
		}

		@Nullable
		@Override
		DfaNullability intersectFacts(@Nonnull DfaNullability left, @Nonnull DfaNullability right)
		{
			if(left == DfaNullability.NOT_NULL || right == DfaNullability.NOT_NULL)
			{
				return DfaNullability.NOT_NULL;
			}
			if(left == DfaNullability.FLUSHED && DfaNullability.toNullability(right) == Nullability.NULLABLE ||
					right == DfaNullability.FLUSHED && DfaNullability.toNullability(left) == Nullability.NULLABLE)
			{
				return DfaNullability.NULLABLE;
			}
			return super.intersectFacts(left, right);
		}

		@Nullable
		@Override
		public DfaNullability fromDfaValue(DfaValue value)
		{
			if(value instanceof DfaConstValue)
			{
				return ((DfaConstValue) value).getValue() == null ? DfaNullability.NULL : DfaNullability.NOT_NULL;
			}
			if(value instanceof DfaBoxedValue)
			{
				return DfaNullability.NOT_NULL;
			}
			if(value instanceof DfaFactMapValue && ((DfaFactMapValue) value).get(RANGE) != null)
			{
				return DfaNullability.NOT_NULL;
			}
			return super.fromDfaValue(value);
		}

		@Nullable
		@Override
		DfaNullability calcFromVariable(@Nonnull DfaVariableValue value)
		{
			return NullabilityUtil.calcCanBeNull(value);
		}
	};

	public static final DfaFactType<Mutability> MUTABILITY = new DfaFactType<Mutability>("Mutability")
	{
		@Override
		boolean isUnknown(@Nonnull Mutability fact)
		{
			return fact == Mutability.UNKNOWN;
		}

		@Nonnull
		@Override
		Mutability uniteFacts(@Nonnull Mutability left, @Nonnull Mutability right)
		{
			return left.unite(right);
		}

		@Nonnull
		@Override
		Mutability calcFromVariable(@Nonnull DfaVariableValue value)
		{
			PsiModifierListOwner variable = value.getPsiVariable();
			return variable == null ? Mutability.UNKNOWN : Mutability.getMutability(variable);
		}
	};

	/**
	 * This fact is applied to the integral values (of types byte, char, short, int, long).
	 * Its value represents a range of possible values.
	 */
	public static final DfaFactType<LongRangeSet> RANGE = new DfaFactType<LongRangeSet>("Range")
	{
		@Override
		boolean isSuper(@Nullable LongRangeSet superFact, @Nullable LongRangeSet subFact)
		{
			return superFact == null || subFact != null && superFact.contains(subFact);
		}

		@Override
		boolean isUnknown(@Nonnull LongRangeSet fact)
		{
			return LongRangeSet.all().equals(fact);
		}

		@Nullable
		@Override
		public LongRangeSet fromDfaValue(DfaValue value)
		{
			if(value instanceof DfaVariableValue)
			{
				return calcFromVariable((DfaVariableValue) value);
			}
			if(value instanceof DfaBinOpValue)
			{
				DfaBinOpValue binOp = (DfaBinOpValue) value;
				LongRangeSet left = fromDfaValue(binOp.getLeft());
				LongRangeSet right = fromDfaValue(binOp.getRight());
				if(left != null && right != null)
				{
					return left.binOpFromToken(binOp.getTokenType(), right, PsiType.LONG.equals(binOp.getType()));
				}
			}
			return LongRangeSet.fromDfaValue(value);
		}

		@Nullable
		@Override
		LongRangeSet calcFromVariable(@Nonnull DfaVariableValue var)
		{
			VariableDescriptor descriptor = var.getDescriptor();
			if(descriptor instanceof SpecialField)
			{
				DfaValue defaultValue = ((SpecialField) descriptor).getDefaultValue(var.getFactory(), false);
				LongRangeSet fromSpecialField = LongRangeSet.fromDfaValue(defaultValue);
				if(fromSpecialField != null)
				{
					return fromSpecialField;
				}
			}
			LongRangeSet fromType = LongRangeSet.fromType(var.getType());
			return fromType == null ? null : LongRangeSet.fromPsiElement(var.getPsiVariable()).intersect(fromType);
		}

		@Nullable
		@Override
		LongRangeSet uniteFacts(@Nonnull LongRangeSet left, @Nonnull LongRangeSet right)
		{
			return left.unite(right);
		}

		@Nullable
		@Override
		LongRangeSet intersectFacts(@Nonnull LongRangeSet left, @Nonnull LongRangeSet right)
		{
			LongRangeSet intersection = left.intersect(right);
			return intersection.isEmpty() ? null : intersection;
		}

		@Nonnull
		@Override
		public String getPresentationText(@Nonnull LongRangeSet fact, @Nullable PsiType type)
		{
			LongRangeSet fromType = LongRangeSet.fromType(type);
			if(fact.equals(fromType))
			{
				return "";
			}
			return fact.getPresentationText(type);
		}
	};
	/**
	 * This fact represents a set of possible types of this value
	 * {@link TypeConstraint#empty()} value is equivalent to absent fact (not constrained)
	 */
	public static final DfaFactType<TypeConstraint> TYPE_CONSTRAINT = new DfaFactType<TypeConstraint>("Constraints")
	{
		@Override
		boolean isSuper(@Nullable TypeConstraint superFact, @Nullable TypeConstraint subFact)
		{
			return superFact == null || (subFact != null && superFact.isSuperStateOf(subFact));
		}

		@Nullable
		@Override
		TypeConstraint calcFromVariable(@Nonnull DfaVariableValue value)
		{
			PsiType psiType = value.getType();
			DfaPsiType type = psiType == null ? null : value.getFactory().createDfaType(psiType);
			return type == null ? null : TypeConstraint.empty().withInstanceofValue(type);
		}

		@Override
		boolean isUnknown(@Nonnull TypeConstraint fact)
		{
			return fact.isEmpty();
		}

		@Nullable
		@Override
		TypeConstraint intersectFacts(@Nonnull TypeConstraint left, @Nonnull TypeConstraint right)
		{
			return left.intersect(right);
		}

		@Nullable
		@Override
		TypeConstraint uniteFacts(@Nonnull TypeConstraint left, @Nonnull TypeConstraint right)
		{
			return left.unite(right);
		}

		@Nonnull
		@Override
		public String getPresentationText(@Nonnull TypeConstraint fact, @Nullable PsiType type)
		{
			return fact.getPresentationText(type);
		}
	};

	public static final DfaFactType<Boolean> LOCALITY = new DfaFactType<Boolean>("Locality")
	{
		@Override
		boolean isUnknown(@Nonnull Boolean fact)
		{
			return !fact;
		}

		@Nonnull
		@Override
		public String toString(@Nonnull Boolean fact)
		{
			return fact ? "local object" : "";
		}
	};

	public static final DfaFactType<SpecialFieldValue> SPECIAL_FIELD_VALUE = new DfaFactType<SpecialFieldValue>("Special field value")
	{
		@Nonnull
		@Override
		public String getName(SpecialFieldValue fact)
		{
			return fact == null ? super.getName(null) : StringUtil.wordsToBeginFromUpperCase(fact.getField().toString());
		}

		@Nullable
		@Override
		SpecialFieldValue uniteFacts(@Nonnull SpecialFieldValue left, @Nonnull SpecialFieldValue right)
		{
			return left.unite(right);
		}

		@Nonnull
		@Override
		public String getPresentationText(@Nonnull SpecialFieldValue fact, @Nullable PsiType type)
		{
			return fact.getPresentationText(type);
		}
	};

	@Nonnull
	private final String myName;

	private DfaFactType(@Nonnull String name)
	{
		super("DfaFactType: " + name);
		myName = name;
		// Thread-safe as all DfaFactType instances are created only from DfaFactType class static initializer
		ourFactTypes.add(this);
	}

	@Nonnull
	public String getName(T fact)
	{
		return myName;
	}

	@Nullable
	public T fromDfaValue(DfaValue value)
	{
		return value instanceof DfaFactMapValue ? ((DfaFactMapValue) value).get(this) : null;
	}

	// Could be expensive
	@Nullable
	T calcFromVariable(@Nonnull DfaVariableValue value)
	{
		return null;
	}

	boolean isSuper(@Nullable T superFact, @Nullable T subFact)
	{
		return Objects.equals(superFact, subFact);
	}

	boolean isUnknown(@Nonnull T fact)
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
	@Nullable
	T intersectFacts(@Nonnull T left, @Nonnull T right)
	{
		return left.equals(right) ? left : null;
	}

	/**
	 * Unites two facts of this type.
	 *
	 * @param left  left fact
	 * @param right right fact
	 * @return union fact (null means that the fact can have any value)
	 */
	@Nullable
	T uniteFacts(@Nonnull T left, @Nonnull T right)
	{
		return left.equals(right) ? left : null;
	}

	/**
	 * Produces a short suitable for debug output fact representation
	 *
	 * @param fact a fact to represent
	 * @return a string representation of the fact
	 */
	@Nonnull
	public String toString(@Nonnull T fact)
	{
		return fact.toString();
	}

	/**
	 * Produces a user-friendly presentation of the fact based on the fact itself and the type of the expression
	 *
	 * @param fact a fact to represent
	 * @param type an expression type, if known
	 * @return a user-friendly string representation of the fact; empty string if the fact adds nothing to the expression type
	 * (e.g. fact is Range {0..65535} and type is 'char').
	 */
	@Nonnull
	public String getPresentationText(@Nonnull T fact, @Nullable PsiType type)
	{
		return toString(fact);
	}

	static List<DfaFactType<?>> getTypes()
	{
		return Collections.unmodifiableList(ourFactTypes);
	}
}
