/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.psi.impl.source.tree.injected;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.intellij.lang.injection.ConcatenationAwareInjector;
import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.extensions.ExtensionPoint;
import com.intellij.openapi.extensions.ExtensionPointListener;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.PsiParameterizedCachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.ParameterizedCachedValue;
import com.intellij.psi.util.ParameterizedCachedValueProvider;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.containers.ContainerUtil;

/**
 * @author cdr
 */
@Singleton
public class JavaConcatenationInjectorManager implements ModificationTracker
{
	//FIXME [VISTALL] move ConcatenationAwareInjector to java plugin?
	public static final ExtensionPointName<ConcatenationAwareInjector> CONCATENATION_INJECTOR_EP_NAME = ExtensionPointName.create("com.intellij.concatenationAwareInjector");
	private volatile long myModificationCounter;

	@Inject
	public JavaConcatenationInjectorManager(Project project, PsiManager psiManager)
	{
		final ExtensionPoint<ConcatenationAwareInjector> concatPoint = Extensions.getArea(project).getExtensionPoint(CONCATENATION_INJECTOR_EP_NAME);
		concatPoint.addExtensionPointListener(new ExtensionPointListener<ConcatenationAwareInjector>()
		{
			@Override
			public void extensionAdded(@Nonnull ConcatenationAwareInjector injector, @Nullable PluginDescriptor pluginDescriptor)
			{
				registerConcatenationInjector(injector);
			}

			@Override
			public void extensionRemoved(@Nonnull ConcatenationAwareInjector injector, @Nullable PluginDescriptor pluginDescriptor)
			{
				unregisterConcatenationInjector(injector);
			}
		});
		((PsiManagerEx) psiManager).registerRunnableToRunOnAnyChange(new Runnable()
		{
			@Override
			public void run()
			{
				myModificationCounter++; // clear caches even on non-physical changes
			}
		});
	}

	public static JavaConcatenationInjectorManager getInstance(final Project project)
	{
		return ServiceManager.getService(project, JavaConcatenationInjectorManager.class);
	}

	@Override
	public long getModificationCount()
	{
		return myModificationCounter;
	}

	private static Pair<PsiElement, PsiElement[]> computeAnchorAndOperandsImpl(@Nonnull PsiElement context)
	{
		PsiElement element = context;
		PsiElement parent = context.getParent();
		while(parent instanceof PsiPolyadicExpression && ((PsiPolyadicExpression) parent).getOperationTokenType() == JavaTokenType.PLUS || parent
				instanceof PsiAssignmentExpression && ((PsiAssignmentExpression) parent).getOperationTokenType() == JavaTokenType.PLUSEQ || parent
				instanceof PsiConditionalExpression && ((PsiConditionalExpression) parent).getCondition() != element || parent instanceof
				PsiTypeCastExpression || parent instanceof PsiParenthesizedExpression)
		{
			element = parent;
			parent = parent.getParent();
		}

		PsiElement[] operands;
		PsiElement anchor;
		if(element instanceof PsiPolyadicExpression)
		{
			operands = ((PsiPolyadicExpression) element).getOperands();
			anchor = element;
		}
		else if(element instanceof PsiAssignmentExpression)
		{
			PsiExpression rExpression = ((PsiAssignmentExpression) element).getRExpression();
			operands = new PsiElement[]{rExpression == null ? element : rExpression};
			anchor = element;
		}
		else
		{
			operands = new PsiElement[]{context};
			anchor = context;
		}

		return Pair.create(anchor, operands);
	}

	private static MultiHostRegistrarImpl doCompute(@Nonnull PsiFile containingFile, @Nonnull Project project, @Nonnull PsiElement anchor,
													@Nonnull PsiElement[] operands)
	{
		MultiHostRegistrarImpl registrar = new MultiHostRegistrarImpl(project, containingFile, anchor);
		JavaConcatenationInjectorManager concatenationInjectorManager = getInstance(project);
		for(ConcatenationAwareInjector concatenationInjector : concatenationInjectorManager.myConcatenationInjectors)
		{
			concatenationInjector.getLanguagesToInject(registrar, operands);
			if(registrar.getResult() != null)
			{
				break;
			}
		}

		if(registrar.getResult() == null)
		{
			registrar = null;
		}
		return registrar;
	}

	private static final Key<ParameterizedCachedValue<MultiHostRegistrarImpl, PsiElement>> INJECTED_PSI_IN_CONCATENATION = Key.create
			("INJECTED_PSI_IN_CONCATENATION");
	private static final Key<Integer> NO_CONCAT_INJECTION_TIMESTAMP = Key.create("NO_CONCAT_INJECTION_TIMESTAMP");

	public abstract static class BaseConcatenation2InjectorAdapter implements MultiHostInjector
	{
		private final JavaConcatenationInjectorManager myManager;

		public BaseConcatenation2InjectorAdapter(Project project)
		{
			myManager = getInstance(project);
		}

		@Override
		public void injectLanguages(@Nonnull MultiHostRegistrar registrar, @Nonnull PsiElement context)
		{
			if(myManager.myConcatenationInjectors.isEmpty())
			{
				return;
			}

			final PsiFile containingFile = ((MultiHostRegistrarImpl) registrar).getHostPsiFile();
			Project project = containingFile.getProject();
			long modificationCount = PsiManager.getInstance(project).getModificationTracker().getModificationCount();
			Pair<PsiElement, PsiElement[]> pair = computeAnchorAndOperands(context);
			PsiElement anchor = pair.first;
			PsiElement[] operands = pair.second;
			Integer noInjectionTimestamp = anchor.getUserData(NO_CONCAT_INJECTION_TIMESTAMP);

			MultiHostRegistrarImpl result;
			ParameterizedCachedValue<MultiHostRegistrarImpl, PsiElement> data = null;
			if(operands.length == 0 || noInjectionTimestamp != null && noInjectionTimestamp == modificationCount)
			{
				result = null;
			}
			else
			{
				data = anchor.getUserData(INJECTED_PSI_IN_CONCATENATION);

				if(data == null)
				{
					result = doCompute(containingFile, project, anchor, operands);
				}
				else
				{
					result = data.getValue(context);
				}
			}
			if(result != null && result.getResult() != null)
			{
				for(Pair<Place, PsiFile> p : result.getResult())
				{
					((MultiHostRegistrarImpl) registrar).addToResults(p.first, p.second, result);
				}

				if(data == null)
				{
					CachedValueProvider.Result<MultiHostRegistrarImpl> cachedResult = CachedValueProvider.Result.create(result,
							PsiModificationTracker.MODIFICATION_COUNT, getInstance(project));
					data = CachedValuesManager.getManager(project).createParameterizedCachedValue(new ParameterizedCachedValueProvider<MultiHostRegistrarImpl, PsiElement>()
					{
						@Override
						public CachedValueProvider.Result<MultiHostRegistrarImpl> compute(PsiElement context)
						{
							PsiFile containingFile1 = context.getContainingFile();
							Project project1 = containingFile1.getProject();
							Pair<PsiElement, PsiElement[]> pair = computeAnchorAndOperands(context);
							MultiHostRegistrarImpl registrar = pair.second.length == 0 ? null : doCompute(containingFile1, project1, pair.first,
									pair.second);
							return registrar == null ? null : CachedValueProvider.Result.create(registrar,
									PsiModificationTracker.MODIFICATION_COUNT, getInstance(project1));
						}
					}, false);
					((PsiParameterizedCachedValue<MultiHostRegistrarImpl, PsiElement>) data).setValue(cachedResult);

					anchor.putUserData(INJECTED_PSI_IN_CONCATENATION, data);
					if(anchor.getUserData(NO_CONCAT_INJECTION_TIMESTAMP) != null)
					{
						anchor.putUserData(NO_CONCAT_INJECTION_TIMESTAMP, null);
					}
				}
			}
			else
			{
				// cache no-injection flag
				if(anchor.getUserData(INJECTED_PSI_IN_CONCATENATION) != null)
				{
					anchor.putUserData(INJECTED_PSI_IN_CONCATENATION, null);
				}
				anchor.putUserData(NO_CONCAT_INJECTION_TIMESTAMP, (int) modificationCount);
			}
		}

		protected abstract Pair<PsiElement, PsiElement[]> computeAnchorAndOperands(@Nonnull PsiElement context);
	}

	public static class Concatenation2InjectorAdapter extends BaseConcatenation2InjectorAdapter implements MultiHostInjector
	{

		public Concatenation2InjectorAdapter(Project project)
		{
			super(project);
		}

		@Override
		public Pair<PsiElement, PsiElement[]> computeAnchorAndOperands(@Nonnull PsiElement context)
		{
			return computeAnchorAndOperandsImpl(context);
		}
	}

	private final List<ConcatenationAwareInjector> myConcatenationInjectors = ContainerUtil.createLockFreeCopyOnWriteList();

	public void registerConcatenationInjector(@Nonnull ConcatenationAwareInjector injector)
	{
		myConcatenationInjectors.add(injector);
		concatenationInjectorsChanged();
	}

	public boolean unregisterConcatenationInjector(@Nonnull ConcatenationAwareInjector injector)
	{
		boolean removed = myConcatenationInjectors.remove(injector);
		concatenationInjectorsChanged();
		return removed;
	}

	private void concatenationInjectorsChanged()
	{
		myModificationCounter++;
	}
}
