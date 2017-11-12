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
 * User: anna
 * Date: 20-Aug-2007
 */
package com.intellij.execution.filters;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import consulo.java.module.extension.JavaModuleExtension;
import consulo.module.extension.ModuleExtensionHelper;

public class DefaultConsoleFiltersProvider implements ConsoleFilterProviderEx
{
	@NotNull
	@Override
	public Filter[] getDefaultFilters(@NotNull Project project)
	{
		return getDefaultFilters(project, GlobalSearchScope.allScope(project));
	}

	@Override
	@NotNull
	public Filter[] getDefaultFilters(@NotNull Project project, @NotNull GlobalSearchScope scope)
	{
		if(!ModuleExtensionHelper.getInstance(project).hasModuleExtension(JavaModuleExtension.class))
		{
			return Filter.EMPTY_ARRAY;
		}

		List<Filter> filters = ExceptionFilters.getFilters(scope);
		filters.add(new YourkitFilter(project));
		return filters.toArray(new Filter[filters.size()]);
	}
}