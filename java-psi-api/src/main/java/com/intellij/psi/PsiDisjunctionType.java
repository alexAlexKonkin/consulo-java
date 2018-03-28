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
package com.intellij.psi;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NonNls;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.Function;

/**
 * Composite type resulting from Project Coin's multi-catch statements, i.e. {@code FileNotFoundException | EOFException}.
 * In most cases should be threatened via its least upper bound ({@code IOException} in the example above).
 */
public class PsiDisjunctionType extends PsiType.Stub
{
	private final PsiManager myManager;
	private final List<PsiType> myTypes;
	private final CachedValue<PsiType> myLubCache;

	public PsiDisjunctionType(@Nonnull List<PsiType> types, @Nonnull PsiManager psiManager)
	{
		super(TypeAnnotationProvider.EMPTY);

		myManager = psiManager;
		myTypes = Collections.unmodifiableList(types);

		myLubCache = CachedValuesManager.getManager(myManager.getProject()).createCachedValue(new CachedValueProvider<PsiType>()
		{
			@Override
			public Result<PsiType> compute()
			{
				PsiType lub = myTypes.get(0);
				for(int i = 1; i < myTypes.size(); i++)
				{
					lub = GenericsUtil.getLeastUpperBound(lub, myTypes.get(i), myManager);
					if(lub == null)
					{
						lub = PsiType.getJavaLangObject(myManager, GlobalSearchScope.allScope(myManager.getProject()));
						break;
					}
				}
				return Result.create(lub, PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT);
			}
		}, false);
	}

	@Nonnull
	public static PsiType createDisjunction(@Nonnull List<PsiType> types, @Nonnull PsiManager psiManager)
	{
		assert !types.isEmpty();
		return types.size() == 1 ? types.get(0) : new PsiDisjunctionType(types, psiManager);
	}

	@Nonnull
	public PsiType getLeastUpperBound()
	{
		return myLubCache.getValue();
	}

	@Nonnull
	public List<PsiType> getDisjunctions()
	{
		return myTypes;
	}

	@Nonnull
	public PsiDisjunctionType newDisjunctionType(final List<PsiType> types)
	{
		return new PsiDisjunctionType(types, myManager);
	}

	@Nonnull
	@Override
	public String getPresentableText(final boolean annotated)
	{
		return StringUtil.join(myTypes, new Function<PsiType, String>()
		{
			@Override
			public String fun(PsiType psiType)
			{
				return psiType.getPresentableText(annotated);
			}
		}, " | ");
	}

	@Nonnull
	@Override
	public String getCanonicalText(final boolean annotated)
	{
		return StringUtil.join(myTypes, new Function<PsiType, String>()
		{
			@Override
			public String fun(PsiType psiType)
			{
				return psiType.getCanonicalText(annotated);
			}
		}, " | ");
	}

	@Nonnull
	@Override
	public String getInternalCanonicalText()
	{
		return StringUtil.join(myTypes, new Function<PsiType, String>()
		{
			@Override
			public String fun(PsiType psiType)
			{
				return psiType.getInternalCanonicalText();
			}
		}, " | ");
	}

	@Override
	public boolean isValid()
	{
		for(PsiType type : myTypes)
		{
			if(!type.isValid())
			{
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean equalsToText(@Nonnull @NonNls final String text)
	{
		return Comparing.equal(text, getCanonicalText());
	}

	@Override
	public <A> A accept(@Nonnull final PsiTypeVisitor<A> visitor)
	{
		return visitor.visitDisjunctionType(this);
	}

	@Override
	public GlobalSearchScope getResolveScope()
	{
		return getLeastUpperBound().getResolveScope();
	}

	@Nonnull
	@Override
	public PsiType[] getSuperTypes()
	{
		final PsiType lub = getLeastUpperBound();
		if(lub instanceof PsiIntersectionType)
		{
			return ((PsiIntersectionType) lub).getConjuncts();
		}
		else
		{
			return new PsiType[]{lub};
		}
	}

	@Override
	public int hashCode()
	{
		return myTypes.get(0).hashCode();
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

		final PsiDisjunctionType that = (PsiDisjunctionType) o;
		if(that.myTypes.size() != myTypes.size())
		{
			return false;
		}

		for(int i = 0; i < myTypes.size(); i++)
		{
			if(!myTypes.get(i).equals(that.myTypes.get(i)))
			{
				return false;
			}
		}

		return true;
	}
}