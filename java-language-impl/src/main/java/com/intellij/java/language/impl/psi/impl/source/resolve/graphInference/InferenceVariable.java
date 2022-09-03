/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.java.language.impl.psi.impl.source.resolve.graphInference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiClassType;
import consulo.language.psi.PsiElement;
import com.intellij.java.language.psi.PsiType;
import com.intellij.java.language.psi.PsiTypeParameter;
import com.intellij.java.language.psi.PsiTypeParameterListOwner;
import com.intellij.java.language.psi.TypeAnnotationProvider;
import com.intellij.java.language.psi.augment.TypeAnnotationModifier;
import com.intellij.java.language.impl.psi.impl.light.LightTypeParameter;
import consulo.language.psi.util.PsiTreeUtil;
import com.intellij.java.language.psi.util.PsiUtil;
import com.intellij.java.language.psi.util.TypeConversionUtil;

/**
 * User: anna
 */
public class InferenceVariable extends LightTypeParameter
{
	private final PsiElement myContext;

	public PsiTypeParameter getParameter()
	{
		return getDelegate();
	}

	private boolean myThrownBound;
	private final Map<InferenceBound, List<PsiType>> myBounds = new HashMap<InferenceBound, List<PsiType>>();
	private final String myName;

	private PsiType myInstantiation = PsiType.NULL;

	InferenceVariable(PsiElement context, PsiTypeParameter parameter, String name)
	{
		super(parameter);
		myName = name;
		myContext = context;
		TypeConversionUtil.markAsFreshVariable(this, context);
	}

	public PsiType getInstantiation()
	{
		return myInstantiation;
	}

	public void setInstantiation(PsiType instantiation)
	{
		myInstantiation = instantiation;
	}

	@Nonnull
	@Override
	public PsiClassType[] getExtendsListTypes()
	{
		final List<PsiClassType> result = new ArrayList<PsiClassType>();
		for(PsiType type : getBounds(InferenceBound.UPPER))
		{
			if(type instanceof PsiClassType)
			{
				result.add((PsiClassType) type);
			}
		}
		return result.toArray(new PsiClassType[result.size()]);
	}

	public static void addBound(PsiType inferenceVariableType, PsiType boundType, InferenceBound inferenceBound, InferenceSession session)
	{
		final InferenceVariable variable = session.getInferenceVariable(inferenceVariableType);
		if(variable != null)
		{
			for(TypeAnnotationModifier modifier : TypeAnnotationModifier.EP_NAME.getExtensions())
			{
				if(boundType instanceof PsiClassType)
				{
					final TypeAnnotationProvider annotationProvider = modifier.modifyAnnotations(inferenceVariableType, (PsiClassType) boundType);
					if(annotationProvider != null)
					{
						boundType = boundType.annotate(annotationProvider);
					}
				}
			}

			variable.addBound(boundType, inferenceBound, session.myIncorporationPhase);
		}
	}

	public boolean addBound(PsiType classType, InferenceBound inferenceBound, @Nullable InferenceIncorporationPhase incorporationPhase)
	{
		if(inferenceBound == InferenceBound.EQ && PsiUtil.resolveClassInClassTypeOnly(classType) == this)
		{
			return false;
		}
		List<PsiType> bounds = myBounds.get(inferenceBound);
		if(bounds == null)
		{
			bounds = new ArrayList<PsiType>();
			myBounds.put(inferenceBound, bounds);
		}

		if(classType == null)
		{
			classType = PsiType.NULL;
		}

		if(incorporationPhase == null || !bounds.contains(classType))
		{
			bounds.add(classType);
			if(incorporationPhase != null)
			{
				incorporationPhase.addBound(this, classType, inferenceBound);
			}
			return true;
		}
		return false;
	}

	public List<PsiType> getBounds(InferenceBound inferenceBound)
	{
		final List<PsiType> bounds = myBounds.get(inferenceBound);
		return bounds != null ? new ArrayList<PsiType>(bounds) : Collections.<PsiType>emptyList();
	}

	public List<PsiType> getReadOnlyBounds(InferenceBound inferenceBound)
	{
		final List<PsiType> bounds = myBounds.get(inferenceBound);
		return bounds != null ? bounds : Collections.<PsiType>emptyList();
	}

	public Set<InferenceVariable> getDependencies(InferenceSession session)
	{
		final Set<InferenceVariable> dependencies = new LinkedHashSet<InferenceVariable>();
		collectBoundDependencies(session, dependencies);
		collectTransitiveDependencies(session, dependencies, dependencies);

		if(!session.hasCapture(this) && dependencies.isEmpty())
		{
			return dependencies;
		}

		if(!session.hasCapture(this))
		{
			return dependencies;
		}

		for(Iterator<InferenceVariable> iterator = dependencies.iterator(); iterator.hasNext(); )
		{
			if(!session.hasCapture(iterator.next()))
			{
				iterator.remove();
			}
		}
		session.collectCaptureDependencies(this, dependencies);
		return dependencies;
	}

	private void collectTransitiveDependencies(InferenceSession session, Set<InferenceVariable> dependencies, Set<InferenceVariable> rootDependencies)
	{
		final LinkedHashSet<InferenceVariable> newDependencies = new LinkedHashSet<InferenceVariable>();

		for(InferenceVariable dependency : dependencies)
		{
			dependency.collectBoundDependencies(session, newDependencies);
		}
		newDependencies.removeAll(rootDependencies);
		newDependencies.remove(this);

		if(!newDependencies.isEmpty())
		{
			rootDependencies.addAll(newDependencies);
			collectTransitiveDependencies(session, newDependencies, rootDependencies);
		}
	}

	private void collectBoundDependencies(InferenceSession session, Set<InferenceVariable> dependencies)
	{
		for(Collection<PsiType> boundTypes : myBounds.values())
		{
			if(boundTypes != null)
			{
				for(PsiType bound : boundTypes)
				{
					session.collectDependencies(bound, dependencies);
				}
			}
		}
	}

	public boolean isThrownBound()
	{
		return myThrownBound;
	}

	public void setThrownBound()
	{
		myThrownBound = true;
	}

	@Override
	public boolean isInheritor(@Nonnull PsiClass baseClass, boolean checkDeep)
	{
		for(PsiType type : getBounds(InferenceBound.UPPER))
		{
			PsiClass psiClass = PsiUtil.resolveClassInClassTypeOnly(type);
			if(psiClass != null)
			{
				if(getManager().areElementsEquivalent(baseClass, psiClass))
				{
					return true;
				}
				if(checkDeep && psiClass.isInheritor(baseClass, true))
				{
					return true;
				}
			}
		}

		return super.isInheritor(baseClass, checkDeep);
	}

	@Override
	public boolean isEquivalentTo(PsiElement another)
	{
		if(this == another)
		{
			return true;
		}

		if(getDelegate() == another && myContext != null && !PsiTreeUtil.isAncestor(((PsiTypeParameter) another).getOwner(), myContext, false))
		{
			return true;
		}
		return false;
	}

	@Override
	public boolean useDelegateToSubstitute()
	{
		return false;
	}

	@Override
	public String toString()
	{
		return getDelegate().toString();
	}

	@Override
	public PsiTypeParameterListOwner getOwner()
	{
		return null;
	}

	@Nullable
	@Override
	public String getName()
	{
		return myName;
	}

	public PsiElement getCallContext()
	{
		return myContext;
	}
}
