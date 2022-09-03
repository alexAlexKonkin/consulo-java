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
package com.intellij.java.debugger.impl.memory.action;

import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.internal.com.sun.jdi.ReferenceType;

abstract class ShowInstancesAction extends ClassesActionBase
{
	@Override
	public void update(AnActionEvent e)
	{
		final Presentation presentation = e.getPresentation();
		final ReferenceType ref = getSelectedClass(e);
		final boolean enabled = isEnabled(e) && ref != null && ref.virtualMachine().canGetInstanceInfo();
		presentation.setEnabled(enabled);
		if(enabled)
		{
			presentation.setText(String.format("%s (%d)", getLabel(), getInstancesCount(e)));
		}
	}

	protected abstract String getLabel();

	protected abstract int getInstancesCount(AnActionEvent e);
}
