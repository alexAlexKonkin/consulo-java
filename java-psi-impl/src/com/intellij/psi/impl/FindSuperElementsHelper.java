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
package com.intellij.psi.impl;

import gnu.trove.THashSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiAnchor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.util.MethodSignatureUtil;
import com.intellij.psi.util.PsiSuperMethodUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.util.TypeConversionUtil;
import com.intellij.util.Processor;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.containers.hash.HashMap;

public class FindSuperElementsHelper
{
	@NotNull
	public static PsiElement[] findSuperElements(@NotNull PsiElement element)
	{
		if(element instanceof PsiClass)
		{
			PsiClass aClass = (PsiClass) element;
			List<PsiClass> allSupers = new ArrayList<>(Arrays.asList(aClass.getSupers()));
			for(Iterator<PsiClass> iterator = allSupers.iterator(); iterator.hasNext(); )
			{
				PsiClass superClass = iterator.next();
				if(CommonClassNames.JAVA_LANG_OBJECT.equals(superClass.getQualifiedName()))
				{
					iterator.remove();
				}
			}
			return allSupers.toArray(new PsiClass[allSupers.size()]);
		}
		if(element instanceof PsiMethod)
		{
			PsiMethod method = (PsiMethod) element;
			if(method.isConstructor())
			{
				PsiMethod constructorInSuper = PsiSuperMethodUtil.findConstructorInSuper(method);
				if(constructorInSuper != null)
				{
					return new PsiMethod[]{constructorInSuper};
				}
			}
			else
			{
				PsiMethod[] superMethods = method.findSuperMethods(false);
				if(superMethods.length == 0)
				{
					PsiMethod superMethod = getSiblingInheritedViaSubClass(method);
					if(superMethod != null)
					{
						superMethods = new PsiMethod[]{superMethod};
					}
				}
				return superMethods;
			}
		}
		return PsiElement.EMPTY_ARRAY;
	}

	public static PsiMethod getSiblingInheritedViaSubClass(@NotNull PsiMethod method)
	{
		SiblingInfo info = getSiblingInfoInheritedViaSubClass(method);
		return info == null ? null : info.superMethod;
	}

	/**
	 * @return (super method, sub class) or null if can't find any siblings
	 */
	@Nullable
	public static SiblingInfo getSiblingInfoInheritedViaSubClass(@NotNull final PsiMethod method)
	{
		return getSiblingInheritanceInfos(Collections.singletonList(method)).get(method);
	}

	@NotNull
	public static Map<PsiMethod, SiblingInfo> getSiblingInheritanceInfos(@NotNull final Collection<PsiMethod> methods)
	{
		MultiMap<PsiClass, PsiMethod> byClass = MultiMap.create();
		for(PsiMethod method : methods)
		{
			PsiClass containingClass = method.getContainingClass();
			if(canHaveSiblingSuper(method, containingClass))
			{
				byClass.putValue(containingClass, method);
			}
		}

		Map<PsiMethod, SiblingInfo> result = new HashMap<>();
		for(PsiClass psiClass : byClass.keySet())
		{
			SiblingInheritorSearcher searcher = new SiblingInheritorSearcher(byClass.get(psiClass), psiClass);
			ClassInheritorsSearch.search(psiClass, psiClass.getUseScope(), true, true, false).forEach(searcher);
			result.putAll(searcher.getResult());
		}
		return result;
	}

	private static boolean canHaveSiblingSuper(PsiMethod method, PsiClass containingClass)
	{
		return containingClass != null &&
				PsiUtil.canBeOverriden(method) &&
				!method.hasModifierProperty(PsiModifier.ABSTRACT) &&
				!method.hasModifierProperty(PsiModifier.NATIVE) &&
				method.hasModifierProperty(PsiModifier.PUBLIC) &&
				!containingClass.isInterface() &&
				!CommonClassNames.JAVA_LANG_OBJECT.equals(containingClass.getQualifiedName());
	}

	public static class SiblingInfo
	{
		@NotNull
		public final PsiMethod superMethod;
		@NotNull
		public final PsiClass subClass;

		private SiblingInfo(@NotNull PsiMethod superMethod, @NotNull PsiClass subClass)
		{
			this.superMethod = superMethod;
			this.subClass = subClass;
		}
	}

	private static class SiblingInheritorSearcher implements Processor<PsiClass>
	{
		private final PsiClass myContainingClass;
		private final Set<PsiMethod> myRemainingMethods;
		private final Map<PsiMethod, SiblingInfo> myResult = new HashMap<>();
		private final Collection<PsiAnchor> myCheckedInterfaces = new THashSet<>();

		SiblingInheritorSearcher(Collection<PsiMethod> methods, PsiClass containingClass)
		{
			myContainingClass = containingClass;
			myRemainingMethods = new HashSet<>(methods);
			myCheckedInterfaces.add(PsiAnchor.create(containingClass));
		}

		@Override
		public boolean process(PsiClass inheritor)
		{
			ProgressManager.checkCanceled();
			for(PsiClassType interfaceType : inheritor.getImplementsListTypes())
			{
				ProgressManager.checkCanceled();
				PsiClass anInterface = interfaceType.resolveGenerics().getElement();
				if(anInterface != null && myCheckedInterfaces.add(PsiAnchor.create(anInterface)))
				{
					processInterface(inheritor, anInterface);
				}
			}
			return !myRemainingMethods.isEmpty();
		}

		private void processInterface(PsiClass inheritor, PsiClass anInterface)
		{
			for(Iterator<PsiMethod> methodIterator = myRemainingMethods.iterator(); methodIterator.hasNext(); )
			{
				PsiMethod method = methodIterator.next();
				SiblingInfo info = findSibling(inheritor, anInterface, method);
				if(info != null)
				{
					myResult.put(method, info);
					methodIterator.remove();
				}
			}
		}

		@Nullable
		private SiblingInfo findSibling(PsiClass inheritor, PsiClass anInterface, PsiMethod method)
		{
			for(PsiMethod superMethod : anInterface.findMethodsByName(method.getName(), true))
			{
				PsiElement navigationElement = superMethod.getNavigationElement();
				if(!(navigationElement instanceof PsiMethod))
				{
					continue; // Kotlin
				}
				superMethod = (PsiMethod) navigationElement;
				ProgressManager.checkCanceled();
				PsiClass superInterface = superMethod.getContainingClass();
				if(superInterface == null || myContainingClass.isInheritor(superInterface, true))
				{
					// if containingClass implements the superInterface then it's not a sibling inheritance but a pretty boring the usual one
					continue;
				}

				if(isOverridden(inheritor, method, superMethod, superInterface))
				{
					return new SiblingInfo(superMethod, inheritor);
				}
			}
			return null;
		}

		private boolean isOverridden(PsiClass inheritor, PsiMethod method, PsiMethod superMethod, PsiClass superInterface)
		{
			// calculate substitutor of containingClass --> inheritor
			PsiSubstitutor substitutor = TypeConversionUtil.getSuperClassSubstitutor(myContainingClass, inheritor, PsiSubstitutor.EMPTY);
			// calculate substitutor of inheritor --> superInterface
			PsiSubstitutor superInterfaceSubstitutor = TypeConversionUtil.getSuperClassSubstitutor(superInterface, inheritor, substitutor);

			return MethodSignatureUtil.isSubsignature(superMethod.getSignature(superInterfaceSubstitutor), method.getSignature(substitutor));
		}

		Map<PsiMethod, SiblingInfo> getResult()
		{
			return myResult;
		}
	}
}
