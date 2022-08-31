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
package com.intellij.openapi.roots;

import java.util.Collection;
import java.util.Collections;

import javax.annotation.Nonnull;

import com.intellij.java.language.projectRoots.roots.ExternalLibraryDescriptor;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.java.language.LanguageLevel;

/**
 * Provides methods to perform high-level modifications of project configuration accordingly with dependency management system used in the
 * project. E.g. if the project is imported from Maven the methods will modify pom.xml files and invoke reimporting to update IDEA's
 * project model. Since importing the changes to IDEA's project model may take a while the method work asynchronously and returns
 * {@link AsyncResult} objects which may be used to be notified when the project configuration is finally updated.
 *
 * @author nik
 * @see JavaProjectModelModifier
 */
public abstract class JavaProjectModelModificationService
{
	public static JavaProjectModelModificationService getInstance(@Nonnull Project project)
	{
		return ServiceManager.getService(project, JavaProjectModelModificationService.class);
	}

	public AsyncResult<Void> addDependency(@Nonnull Module from, @Nonnull Module to)
	{
		return addDependency(from, to, DependencyScope.COMPILE);
	}

	public abstract AsyncResult<Void> addDependency(@Nonnull Module from, @Nonnull Module to, @Nonnull DependencyScope scope);

	public AsyncResult<Void> addDependency(@Nonnull Module from, @Nonnull ExternalLibraryDescriptor libraryDescriptor)
	{
		return addDependency(from, libraryDescriptor, DependencyScope.COMPILE);
	}

	public AsyncResult<Void> addDependency(@Nonnull Module from, @Nonnull ExternalLibraryDescriptor descriptor, @Nonnull DependencyScope scope)
	{
		return addDependency(Collections.singletonList(from), descriptor, scope);
	}

	public abstract AsyncResult<Void> addDependency(@Nonnull Collection<Module> from, @Nonnull ExternalLibraryDescriptor libraryDescriptor, @Nonnull DependencyScope scope);

	public abstract AsyncResult<Void> addDependency(@Nonnull Module from, @Nonnull Library library, @Nonnull DependencyScope scope);

	public abstract AsyncResult<Void> changeLanguageLevel(@Nonnull Module module, @Nonnull LanguageLevel languageLevel);
}
