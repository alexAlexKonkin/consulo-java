/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.execution.actions;

import javax.annotation.Nonnull;

import com.intellij.execution.testframework.Filter;
import com.intellij.execution.testframework.JavaAwareFilter;
import com.intellij.execution.testframework.TestConsoleProperties;
import com.intellij.execution.testframework.actions.AbstractRerunFailedTestsAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentContainer;
import com.intellij.psi.search.GlobalSearchScope;

/**
 * @author anna
 * @since 24-Dec-2008
 */
public class JavaRerunFailedTestsAction extends AbstractRerunFailedTestsAction
{
	public JavaRerunFailedTestsAction(@Nonnull ComponentContainer componentContainer, @Nonnull TestConsoleProperties consoleProperties)
	{
		super(componentContainer);
		init(consoleProperties);
	}

	@Nonnull
	@Override
	protected Filter getFilter(@Nonnull Project project, @Nonnull GlobalSearchScope searchScope)
	{
		return super.getFilter(project, searchScope).and(JavaAwareFilter.METHOD(project, searchScope));
	}
}
