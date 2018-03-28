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

package com.intellij.codeInspection.dataFlow.value;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import com.intellij.codeInspection.dataFlow.DfaFactMap;
import com.intellij.codeInspection.dataFlow.DfaFactType;
import com.intellij.codeInspection.dataFlow.Nullness;
import com.intellij.codeInspection.dataFlow.NullnessUtil;
import com.intellij.codeInspection.dataFlow.SpecialField;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Trinity;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.util.TypeConversionUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.MultiMap;

public class DfaVariableValue extends DfaValue
{

	public static class Factory
	{
		private final MultiMap<Trinity<Boolean, String, DfaVariableValue>, DfaVariableValue> myExistingVars = new MultiMap<>();
		private final DfaValueFactory myFactory;

		Factory(DfaValueFactory factory)
		{
			myFactory = factory;
		}

		public DfaVariableValue createVariableValue(PsiVariable variable, boolean isNegated)
		{
			PsiType varType = variable.getType();
			if(varType instanceof PsiEllipsisType)
			{
				varType = new PsiArrayType(((PsiEllipsisType) varType).getComponentType());
			}
			return createVariableValue(variable, varType, isNegated, null);
		}

		@Nonnull
		public DfaVariableValue createVariableValue(@Nonnull PsiModifierListOwner myVariable, @javax.annotation.Nullable PsiType varType, boolean isNegated, @javax.annotation.Nullable DfaVariableValue qualifier)
		{
			Trinity<Boolean, String, DfaVariableValue> key = Trinity.create(isNegated, ((PsiNamedElement) myVariable).getName(), qualifier);
			for(DfaVariableValue aVar : myExistingVars.get(key))
			{
				if(aVar.hardEquals(myVariable, varType, isNegated, qualifier))
				{
					return aVar;
				}
			}

			DfaVariableValue result = new DfaVariableValue(myVariable, varType, isNegated, myFactory, qualifier);
			myExistingVars.putValue(key, result);
			while(qualifier != null)
			{
				qualifier.myDependents.add(result);
				qualifier = qualifier.getQualifier();
			}
			return result;
		}

		@Nonnull
		public List<DfaVariableValue> getAllQualifiedBy(@Nonnull DfaVariableValue value)
		{
			return value.myDependents;
		}
	}

	private final PsiModifierListOwner myVariable;
	private final PsiType myVarType;
	@javax.annotation.Nullable
	private final DfaVariableValue myQualifier;
	private DfaVariableValue myNegatedValue;
	private final boolean myIsNegated;
	private DfaFactMap myInherentFacts;
	private final DfaTypeValue myTypeValue;
	private final List<DfaVariableValue> myDependents = new SmartList<>();

	private DfaVariableValue(@Nonnull PsiModifierListOwner variable, @javax.annotation.Nullable PsiType varType, boolean isNegated, DfaValueFactory factory, @javax.annotation.Nullable DfaVariableValue qualifier)
	{
		super(factory);
		myVariable = variable;
		myIsNegated = isNegated;
		myQualifier = qualifier;
		myVarType = varType;
		DfaValue typeValue = myFactory.createTypeValue(varType, Nullness.UNKNOWN);
		myTypeValue = typeValue instanceof DfaTypeValue ? (DfaTypeValue) typeValue : null;
		if(varType != null && !varType.isValid())
		{
			PsiUtil.ensureValidType(varType, "Variable: " + variable + " of class " + variable.getClass());
		}
	}

	@javax.annotation.Nullable
	public DfaTypeValue getTypeValue()
	{
		return myTypeValue;
	}

	@Nonnull
	public PsiModifierListOwner getPsiVariable()
	{
		return myVariable;
	}

	@javax.annotation.Nullable
	public PsiType getVariableType()
	{
		return myVarType;
	}

	public boolean isNegated()
	{
		return myIsNegated;
	}

	@javax.annotation.Nullable
	public DfaVariableValue getNegatedValue()
	{
		return myNegatedValue;
	}

	@Override
	public DfaVariableValue createNegated()
	{
		if(myNegatedValue != null)
		{
			return myNegatedValue;
		}
		return myNegatedValue = myFactory.getVarFactory().createVariableValue(myVariable, myVarType, !myIsNegated, myQualifier);
	}

	@SuppressWarnings({"HardCodedStringLiteral"})
	public String toString()
	{
		return (myIsNegated ? "!" : "") + ((PsiNamedElement) myVariable).getName() + (myQualifier == null ? "" : "|" + myQualifier.toString());
	}

	private boolean hardEquals(PsiModifierListOwner psiVar, PsiType varType, boolean negated, DfaVariableValue qualifier)
	{
		return psiVar == myVariable && negated == myIsNegated && qualifier == myQualifier && Comparing.equal(TypeConversionUtil.erasure(varType), TypeConversionUtil.erasure(myVarType));
	}

	@javax.annotation.Nullable
	public DfaVariableValue getQualifier()
	{
		return myQualifier;
	}

	public DfaFactMap getInherentFacts()
	{
		if(myInherentFacts == null)
		{
			myInherentFacts = DfaFactMap.calcFromVariable(this);
		}

		return myInherentFacts;
	}

	@Nonnull
	public Nullness getInherentNullability()
	{
		return NullnessUtil.fromBoolean(getInherentFacts().get(DfaFactType.CAN_BE_NULL));
	}

	public boolean isFlushableByCalls()
	{
		if(myVariable instanceof PsiLocalVariable || myVariable instanceof PsiParameter)
		{
			return false;
		}
		boolean finalField = myVariable instanceof PsiVariable && myVariable.hasModifierProperty(PsiModifier.FINAL);
		boolean specialFinalField = myVariable instanceof PsiMethod && Arrays.stream(SpecialField.values()).anyMatch(sf -> sf.isFinal() && sf.isMyAccessor(myVariable));
		if(finalField || specialFinalField)
		{
			return myQualifier != null && myQualifier.isFlushableByCalls();
		}
		return true;
	}

	public boolean containsCalls()
	{
		return myVariable instanceof PsiMethod || myQualifier != null && myQualifier.containsCalls();
	}
}
