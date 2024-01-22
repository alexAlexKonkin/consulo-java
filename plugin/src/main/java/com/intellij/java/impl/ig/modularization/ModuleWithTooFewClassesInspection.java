/*
 * Copyright 2006-2012 Dave Griffith, Bas Leijdekkers
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
package com.intellij.java.impl.ig.modularization;

import com.intellij.java.analysis.codeInspection.reference.RefClass;
import com.intellij.java.impl.ig.BaseGlobalInspection;
import com.siyeh.InspectionGadgetsBundle;
import consulo.deadCodeNotWorking.impl.SingleIntegerFieldOptionsPanel;
import consulo.language.editor.inspection.CommonProblemDescriptor;
import consulo.language.editor.inspection.GlobalInspectionContext;
import consulo.language.editor.inspection.reference.RefEntity;
import consulo.language.editor.inspection.reference.RefModule;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.scope.AnalysisScope;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

import jakarta.annotation.Nullable;
import javax.swing.*;
import java.util.List;

public abstract class ModuleWithTooFewClassesInspection extends BaseGlobalInspection
{

	@SuppressWarnings({"PublicField"})
	public int limit = 10;

	@Nonnull
	@Override
	public String getDisplayName()
	{
		return InspectionGadgetsBundle.message("module.with.too.few.classes.display.name");
	}

	@Override
	@Nullable
	public CommonProblemDescriptor[] checkElement(RefEntity refEntity, AnalysisScope analysisScope, InspectionManager inspectionManager, GlobalInspectionContext globalInspectionContext, Object state)
	{
		if(!(refEntity instanceof RefModule))
		{
			return null;
		}
		final RefModule refModule = (RefModule) refEntity;
		final List<RefEntity> children = refModule.getChildren();
		if(children == null)
		{
			return null;
		}
		int numClasses = 0;
		for(RefEntity child : children)
		{
			if(child instanceof RefClass)
			{
				numClasses++;
			}
		}
		if(numClasses >= limit || numClasses == 0)
		{
			return null;
		}
		final Project project = globalInspectionContext.getProject();
		final Module[] modules = ModuleManager.getInstance(project).getModules();
		if(modules.length == 1)
		{
			return null;
		}
		final String errorString = InspectionGadgetsBundle.message("module.with.too.few.classes.problem.descriptor",
				refModule.getName(), Integer.valueOf(numClasses), Integer.valueOf(limit));
		return new CommonProblemDescriptor[]{
				inspectionManager.createProblemDescriptor(errorString)
		};
	}

	@Override
	public JComponent createOptionsPanel()
	{
		return new SingleIntegerFieldOptionsPanel(InspectionGadgetsBundle.message("module.with.too.few.classes.min.option"), this, "limit");
	}
}