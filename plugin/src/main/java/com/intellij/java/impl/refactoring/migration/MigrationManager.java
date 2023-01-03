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
package com.intellij.java.impl.refactoring.migration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import consulo.project.Project;

public class MigrationManager
{
	private final Project myProject;
	private final MigrationMapSet myMigrationMapSet = new MigrationMapSet();

	public MigrationManager(Project project)
	{
		myProject = project;
	}

	public void showMigrationDialog()
	{
		final MigrationDialog migrationDialog = new MigrationDialog(myProject, myMigrationMapSet);
		if(!migrationDialog.showAndGet())
		{
			return;
		}
		MigrationMap migrationMap = migrationDialog.getMigrationMap();
		if(migrationMap == null)
		{
			return;
		}

		new MigrationProcessor(myProject, migrationMap, migrationDialog.getScope()).run();
	}

	@Nullable
	public MigrationMap findMigrationMap(@Nonnull String name)
	{
		return myMigrationMapSet.findMigrationMap(name);
	}
}