/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.debugger.actions;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.JavaValue;
import com.intellij.debugger.engine.SuspendContextImpl;
import com.intellij.debugger.engine.events.SuspendContextCommandImpl;
import com.intellij.debugger.impl.DebuggerContextImpl;
import com.intellij.debugger.settings.ArrayRendererConfigurable;
import com.intellij.debugger.ui.impl.watch.DebuggerTreeNodeImpl;
import com.intellij.debugger.ui.impl.watch.NodeDescriptorImpl;
import com.intellij.debugger.ui.impl.watch.ValueDescriptorImpl;
import com.intellij.debugger.ui.tree.render.ArrayRenderer;
import com.intellij.debugger.ui.tree.render.ChildrenRenderer;
import com.intellij.debugger.ui.tree.render.CompoundNodeRenderer;
import com.intellij.debugger.ui.tree.render.ExpressionChildrenRenderer;
import com.intellij.debugger.ui.tree.render.Renderer;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.xdebugger.frame.XValue;
import com.intellij.xdebugger.impl.ui.tree.actions.XDebuggerTreeActionBase;
import com.intellij.xdebugger.impl.ui.tree.nodes.XValueNodeImpl;
import consulo.annotations.RequiredDispatchThread;

public abstract class ArrayAction extends DebuggerAction
{
	@RequiredDispatchThread
	@Override
	public void actionPerformed(@Nonnull AnActionEvent e)
	{
		DebuggerContextImpl debuggerContext = DebuggerAction.getDebuggerContext(e.getDataContext());

		DebugProcessImpl debugProcess = debuggerContext.getDebugProcess();
		if(debugProcess == null)
		{
			return;
		}

		final XValueNodeImpl node = XDebuggerTreeActionBase.getSelectedNode(e.getDataContext());
		if(node == null)
		{
			return;
		}

		ArrayRenderer renderer = getArrayRenderer(node.getValueContainer());
		if(renderer == null)
		{
			return;
		}

		//String title = createNodeTitle("", selectedNode);
		//String label = selectedNode.toString();
		//int index = label.indexOf('=');
		//if (index > 0) {
		//  title = title + " " + label.substring(index);
		//}
		createNewRenderer(node, renderer, debuggerContext, node.getName()).doWhenDone(newRenderer -> setArrayRenderer(newRenderer, node, debuggerContext));
	}

	@Nonnull
	protected abstract AsyncResult<ArrayRenderer> createNewRenderer(XValueNodeImpl node, ArrayRenderer original, @Nonnull DebuggerContextImpl debuggerContext, String title);

	@RequiredDispatchThread
	@Override
	public void update(@Nonnull AnActionEvent e)
	{
		boolean enable = false;
		List<JavaValue> values = ViewAsGroup.getSelectedValues(e);
		if(values.size() == 1)
		{
			enable = getArrayRenderer(values.get(0)) != null;
		}
		e.getPresentation().setEnabledAndVisible(enable);
	}

	@Nullable
	public static ArrayRenderer getArrayRenderer(XValue value)
	{
		if(value instanceof JavaValue)
		{
			ValueDescriptorImpl descriptor = ((JavaValue) value).getDescriptor();
			Renderer lastRenderer = descriptor.getLastRenderer();
			if(lastRenderer instanceof CompoundNodeRenderer)
			{
				ChildrenRenderer childrenRenderer = ((CompoundNodeRenderer) lastRenderer).getChildrenRenderer();
				if(childrenRenderer instanceof ExpressionChildrenRenderer)
				{
					lastRenderer = ExpressionChildrenRenderer.getLastChildrenRenderer(descriptor);
					if(lastRenderer == null)
					{
						lastRenderer = ((ExpressionChildrenRenderer) childrenRenderer).getPredictedRenderer();
					}
				}
			}
			if(lastRenderer instanceof ArrayRenderer)
			{
				return (ArrayRenderer) lastRenderer;
			}
		}
		return null;
	}

	public static void setArrayRenderer(ArrayRenderer newRenderer, @Nonnull XValueNodeImpl node, @Nonnull DebuggerContextImpl debuggerContext)
	{
		XValue container = node.getValueContainer();

		ArrayRenderer renderer = getArrayRenderer(container);
		if(renderer == null)
		{
			return;
		}

		ValueDescriptorImpl descriptor = ((JavaValue) container).getDescriptor();

		DebugProcessImpl debugProcess = debuggerContext.getDebugProcess();
		if(debugProcess != null)
		{
			debugProcess.getManagerThread().schedule(new SuspendContextCommandImpl(debuggerContext.getSuspendContext())
			{
				@Override
				public void contextAction(@Nonnull SuspendContextImpl suspendContext) throws Exception
				{
					final Renderer lastRenderer = descriptor.getLastRenderer();
					if(lastRenderer instanceof ArrayRenderer)
					{
						((JavaValue) container).setRenderer(newRenderer, node);
						node.invokeNodeUpdate(() -> node.getTree().expandPath(node.getPath()));
					}
					else if(lastRenderer instanceof CompoundNodeRenderer)
					{
						final CompoundNodeRenderer compoundRenderer = (CompoundNodeRenderer) lastRenderer;
						final ChildrenRenderer childrenRenderer = compoundRenderer.getChildrenRenderer();
						if(childrenRenderer instanceof ExpressionChildrenRenderer)
						{
							ExpressionChildrenRenderer.setPreferableChildrenRenderer(descriptor, newRenderer);
							((JavaValue) container).reBuild(node);
						}
					}
				}
			});
		}
	}

	private static String createNodeTitle(String prefix, DebuggerTreeNodeImpl node)
	{
		if(node != null)
		{
			DebuggerTreeNodeImpl parent = node.getParent();
			NodeDescriptorImpl descriptor = parent.getDescriptor();
			if(descriptor instanceof ValueDescriptorImpl && ((ValueDescriptorImpl) descriptor).isArray())
			{
				int index = parent.getIndex(node);
				return createNodeTitle(prefix, parent) + "[" + index + "]";
			}
			String name = (node.getDescriptor() != null) ? node.getDescriptor().getName() : null;
			return (name != null) ? prefix + " " + name : prefix;
		}
		return prefix;
	}

	private static class NamedArrayConfigurable extends ArrayRendererConfigurable implements Configurable
	{
		private final String myTitle;

		public NamedArrayConfigurable(String title, ArrayRenderer renderer)
		{
			super(renderer);
			myTitle = title;
		}

		@Override
		public String getDisplayName()
		{
			return myTitle;
		}

		@Override
		public String getHelpTopic()
		{
			return null;
		}
	}

	public static class AdjustArrayRangeAction extends ArrayAction
	{
		@Nonnull
		@Override
		protected AsyncResult<ArrayRenderer> createNewRenderer(XValueNodeImpl node, ArrayRenderer original, @Nonnull DebuggerContextImpl debuggerContext, String title)
		{
			ArrayRenderer clonedRenderer = original.clone();
			clonedRenderer.setForced(true);
			if(ShowSettingsUtil.getInstance().editConfigurable(debuggerContext.getProject(), new NamedArrayConfigurable(title, clonedRenderer)))
			{
				return AsyncResult.done(clonedRenderer);
			}
			return AsyncResult.rejected();
		}
	}

	public static class FilterArrayAction extends ArrayAction
	{
		@Nonnull
		@Override
		protected AsyncResult<ArrayRenderer> createNewRenderer(XValueNodeImpl node, ArrayRenderer original, @Nonnull DebuggerContextImpl debuggerContext, String title)
		{
			//TODO [VISTALL] ArrayFilterInplaceEditor.editParent(node);
			return AsyncResult.rejected();
		}
	}
}
