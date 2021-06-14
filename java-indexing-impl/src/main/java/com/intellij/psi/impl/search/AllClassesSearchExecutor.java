/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
 * @author max
 */
package com.intellij.psi.impl.search;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.AllClassesSearch;
import com.intellij.util.Processor;
import com.intellij.util.QueryExecutor;
import com.intellij.util.indexing.IdFilter;

import javax.annotation.Nonnull;
import java.util.*;

public class AllClassesSearchExecutor implements QueryExecutor<PsiClass, AllClassesSearch.SearchParameters>
{
	@Override
	public boolean execute(@Nonnull final AllClassesSearch.SearchParameters queryParameters, @Nonnull final Processor<? super PsiClass> consumer)
	{
		SearchScope scope = queryParameters.getScope();

		if(scope instanceof GlobalSearchScope)
		{
			return processAllClassesInGlobalScope((GlobalSearchScope) scope, queryParameters, consumer);
		}

		PsiElement[] scopeRoots = ((LocalSearchScope) scope).getScope();
		for(final PsiElement scopeRoot : scopeRoots)
		{
			if(!processScopeRootForAllClasses(scopeRoot, consumer))
			{
				return false;
			}
		}
		return true;
	}

	private static boolean processAllClassesInGlobalScope(@Nonnull final GlobalSearchScope scope, @Nonnull final AllClassesSearch.SearchParameters parameters, @Nonnull Processor<? super PsiClass> processor)
	{
		final Set<String> names = new HashSet<String>(10000);
		processClassNames(parameters.getProject(), scope, s -> {
			if(parameters.nameMatches(s))
			{
				names.add(s);
			}
			return true;
		});

		List<String> sorted = new ArrayList<String>(names);
		Collections.sort(sorted, String.CASE_INSENSITIVE_ORDER);

		return processClassesByNames(parameters.getProject(), scope, sorted, processor);
	}

	public static boolean processClassesByNames(Project project, final GlobalSearchScope scope, Collection<String> names, Processor<? super PsiClass> processor)
	{
		final PsiShortNamesCache cache = PsiShortNamesCache.getInstance(project);
		for(final String name : names)
		{
			ProgressIndicatorProvider.checkCanceled();
			final PsiClass[] classes = MethodUsagesSearcher.resolveInReadAction(project, new Computable<PsiClass[]>()
			{
				@Override
				public PsiClass[] compute()
				{
					return cache.getClassesByName(name, scope);
				}
			});
			for(PsiClass psiClass : classes)
			{
				ProgressIndicatorProvider.checkCanceled();
				if(!processor.process(psiClass))
				{
					return false;
				}
			}
		}
		return true;
	}

	public static Project processClassNames(final Project project, final GlobalSearchScope scope, final Processor<String> consumer)
	{
		final ProgressIndicator indicator = ProgressIndicatorProvider.getGlobalProgressIndicator();

		MethodUsagesSearcher.resolveInReadAction(project, new Computable<Void>()
		{
			@Override
			public Void compute()
			{
				PsiShortNamesCache.getInstance(project).processAllClassNames(new Processor<String>()
				{
					int i = 0;

					@Override
					public boolean process(String s)
					{
						if(indicator != null && i++ % 512 == 0)
						{
							indicator.checkCanceled();
						}
						return consumer.process(s);
					}
				}, scope, IdFilter.getProjectIdFilter(project, true));
				return null;
			}
		});

		if(indicator != null)
		{
			indicator.checkCanceled();
		}
		return project;
	}

	private static boolean processScopeRootForAllClasses(@Nonnull final PsiElement scopeRoot, @Nonnull final Processor<? super PsiClass> processor)
	{
		final boolean[] stopped = {false};

		final JavaElementVisitor visitor = scopeRoot instanceof PsiCompiledElement ? new JavaRecursiveElementVisitor()
		{
			@Override
			public void visitElement(PsiElement element)
			{
				if(!stopped[0])
				{
					super.visitElement(element);
				}
			}

			@Override
			public void visitClass(PsiClass aClass)
			{
				stopped[0] = !processor.process(aClass);
				super.visitClass(aClass);
			}
		} : new JavaRecursiveElementWalkingVisitor()
		{
			@Override
			public void visitElement(PsiElement element)
			{
				if(!stopped[0])
				{
					super.visitElement(element);
				}
			}

			@Override
			public void visitClass(PsiClass aClass)
			{
				stopped[0] = !processor.process(aClass);
				super.visitClass(aClass);
			}
		};
		ApplicationManager.getApplication().runReadAction(new Runnable()
		{
			@Override
			public void run()
			{
				scopeRoot.accept(visitor);
			}
		});

		return !stopped[0];
	}
}
