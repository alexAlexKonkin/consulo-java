package com.intellij.java.indexing.impl.search;

import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiMethod;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.*;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.Processor;

import javax.annotation.Nonnull;

/**
 * @author max
 */
public class ConstructorReferencesSearcher extends QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>
{
	@Override
	public void processQuery(@Nonnull final ReferencesSearch.SearchParameters p, @Nonnull Processor<? super PsiReference> consumer)
	{
		final PsiElement element = p.getElementToSearch();
		if(!(element instanceof PsiMethod))
		{
			return;
		}
		final PsiMethod method = (PsiMethod) element;
		final PsiManager[] manager = new PsiManager[1];
		PsiClass aClass = ApplicationManager.getApplication().runReadAction(new Computable<PsiClass>()
		{
			@Override
			public PsiClass compute()
			{
				if(!method.isConstructor())
				{
					return null;
				}
				PsiClass aClass = method.getContainingClass();
				manager[0] = aClass == null ? null : aClass.getManager();
				return aClass;
			}
		});
		if(manager[0] == null)
		{
			return;
		}
		SearchScope scope = ApplicationManager.getApplication().runReadAction(new Computable<SearchScope>()
		{
			@Override
			public SearchScope compute()
			{
				return p.getEffectiveSearchScope();
			}
		});
		new ConstructorReferencesSearchHelper(manager[0]).processConstructorReferences(consumer, method, aClass, scope, p.getProject(), p.isIgnoreAccessScope(), true, p.getOptimizer());
	}
}
