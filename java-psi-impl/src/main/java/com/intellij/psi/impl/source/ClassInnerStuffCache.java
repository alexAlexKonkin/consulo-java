/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.psi.impl.source;

import static com.intellij.psi.util.PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT;

import gnu.trove.THashMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.psi.ExternallyDefinedPsiElement;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.augment.PsiAugmentProvider;
import com.intellij.psi.impl.PsiClassImplUtil;
import com.intellij.psi.impl.PsiImplUtil;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;

public class ClassInnerStuffCache
{
	private final PsiExtensibleClass myClass;
	private final SimpleModificationTracker myTracker;

	public ClassInnerStuffCache(@Nonnull PsiExtensibleClass aClass)
	{
		myClass = aClass;
		myTracker = new SimpleModificationTracker();
	}

	@Nonnull
	public PsiMethod[] getConstructors()
	{
		return CachedValuesManager.getCachedValue(myClass, new CachedValueProvider<PsiMethod[]>()
		{
			@Nullable
			@Override
			public Result<PsiMethod[]> compute()
			{
				return Result.create(PsiImplUtil.getConstructors(myClass), OUT_OF_CODE_BLOCK_MODIFICATION_COUNT, myTracker);
			}
		});
	}

	@Nonnull
	public PsiField[] getFields()
	{
		return CachedValuesManager.getCachedValue(myClass, new CachedValueProvider<PsiField[]>()
		{
			@Nullable
			@Override
			public Result<PsiField[]> compute()
			{
				return Result.create(getAllFields(), OUT_OF_CODE_BLOCK_MODIFICATION_COUNT, myTracker);
			}
		});
	}

	@Nonnull
	public PsiMethod[] getMethods()
	{
		return CachedValuesManager.getCachedValue(myClass, new CachedValueProvider<PsiMethod[]>()
		{
			@Nullable
			@Override
			public Result<PsiMethod[]> compute()
			{
				return Result.create(getAllMethods(), OUT_OF_CODE_BLOCK_MODIFICATION_COUNT, myTracker);
			}
		});
	}

	@Nonnull
	public PsiClass[] getInnerClasses()
	{
		return CachedValuesManager.getCachedValue(myClass, new CachedValueProvider<PsiClass[]>()
		{
			@Nullable
			@Override
			public Result<PsiClass[]> compute()
			{
				return Result.create(getAllInnerClasses(), OUT_OF_CODE_BLOCK_MODIFICATION_COUNT, myTracker);
			}
		});
	}

	@Nullable
	public PsiField findFieldByName(String name, boolean checkBases)
	{
		if(checkBases)
		{
			return PsiClassImplUtil.findFieldByName(myClass, name, true);
		}
		return CachedValuesManager.getCachedValue(myClass, new CachedValueProvider<Map<String, PsiField>>()
		{
			@Nullable
			@Override
			public Result<Map<String, PsiField>> compute()
			{
				return Result.create(getFieldsMap(), OUT_OF_CODE_BLOCK_MODIFICATION_COUNT, myTracker);
			}
		}).get(name);
	}

	@Nonnull
	public PsiMethod[] findMethodsByName(String name, boolean checkBases)
	{
		if(checkBases)
		{
			return PsiClassImplUtil.findMethodsByName(myClass, name, true);
		}
		PsiMethod[] methods = CachedValuesManager.getCachedValue(myClass, new CachedValueProvider<Map<String, PsiMethod[]>>()
		{
			@Nullable
			@Override
			public Result<Map<String, PsiMethod[]>> compute()
			{
				return Result.create(getMethodsMap(), OUT_OF_CODE_BLOCK_MODIFICATION_COUNT, myTracker);
			}
		}).get(name);
		return methods == null ? PsiMethod.EMPTY_ARRAY : methods;
	}

	@javax.annotation.Nullable
	public PsiClass findInnerClassByName(final String name, final boolean checkBases)
	{
		if(checkBases)
		{
			return PsiClassImplUtil.findInnerByName(myClass, name, true);
		}
		else
		{
			return CachedValuesManager.getCachedValue(myClass, new CachedValueProvider<Map<String, PsiClass>>()
			{
				@Nullable
				@Override
				public Result<Map<String, PsiClass>> compute()
				{
					return Result.create(getInnerClassesMap(), OUT_OF_CODE_BLOCK_MODIFICATION_COUNT, myTracker);
				}
			}).get(name);
		}
	}

	@Nonnull
	private PsiField[] getAllFields()
	{
		List<PsiField> own = myClass.getOwnFields();
		List<PsiField> ext = PsiAugmentProvider.collectAugments(myClass, PsiField.class);
		return ArrayUtil.mergeCollections(own, ext, PsiField.ARRAY_FACTORY);
	}

	@Nonnull
	private PsiMethod[] getAllMethods()
	{
		List<PsiMethod> own = myClass.getOwnMethods();
		List<PsiMethod> ext = PsiAugmentProvider.collectAugments(myClass, PsiMethod.class);
		return ArrayUtil.mergeCollections(own, ext, PsiMethod.ARRAY_FACTORY);
	}

	@Nonnull
	private PsiClass[] getAllInnerClasses()
	{
		List<PsiClass> own = myClass.getOwnInnerClasses();
		List<PsiClass> ext = PsiAugmentProvider.collectAugments(myClass, PsiClass.class);
		return ArrayUtil.mergeCollections(own, ext, PsiClass.ARRAY_FACTORY);
	}

	@Nonnull
	private Map<String, PsiField> getFieldsMap()
	{
		PsiField[] fields = getFields();
		if(fields.length == 0)
		{
			return Collections.emptyMap();
		}

		Map<String, PsiField> cachedFields = new THashMap<String, PsiField>();
		for(PsiField field : fields)
		{
			String name = field.getName();
			if(!(field instanceof ExternallyDefinedPsiElement) || !cachedFields.containsKey(name))
			{
				cachedFields.put(name, field);
			}
		}
		return cachedFields;
	}

	@Nonnull
	private Map<String, PsiMethod[]> getMethodsMap()
	{
		PsiMethod[] methods = getMethods();
		if(methods.length == 0)
		{
			return Collections.emptyMap();
		}

		Map<String, List<PsiMethod>> collectedMethods = ContainerUtil.newHashMap();
		for(PsiMethod method : methods)
		{
			List<PsiMethod> list = collectedMethods.get(method.getName());
			if(list == null)
			{
				collectedMethods.put(method.getName(), list = ContainerUtil.newSmartList());
			}
			list.add(method);
		}

		Map<String, PsiMethod[]> cachedMethods = ContainerUtil.newTroveMap();
		for(Map.Entry<String, List<PsiMethod>> entry : collectedMethods.entrySet())
		{
			List<PsiMethod> list = entry.getValue();
			cachedMethods.put(entry.getKey(), list.toArray(new PsiMethod[list.size()]));
		}
		return cachedMethods;
	}

	@Nonnull
	private Map<String, PsiClass> getInnerClassesMap()
	{
		PsiClass[] classes = getInnerClasses();
		if(classes.length == 0)
		{
			return Collections.emptyMap();
		}

		Map<String, PsiClass> cachedInners = new THashMap<String, PsiClass>();
		for(PsiClass psiClass : classes)
		{
			String name = psiClass.getName();
			if(!(psiClass instanceof ExternallyDefinedPsiElement) || !cachedInners.containsKey(name))
			{
				cachedInners.put(name, psiClass);
			}
		}
		return cachedInners;
	}

	public void dropCaches()
	{
		myTracker.incModificationCount();
	}
}
