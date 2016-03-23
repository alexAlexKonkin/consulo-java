/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.debugger.engine;

import java.util.List;

import javax.swing.Icon;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl;
import com.intellij.debugger.engine.events.SuspendContextCommandImpl;
import com.intellij.debugger.ui.impl.watch.FieldDescriptorImpl;
import com.intellij.debugger.ui.impl.watch.NodeDescriptorImpl;
import com.intellij.debugger.ui.impl.watch.NodeDescriptorProvider;
import com.intellij.debugger.ui.impl.watch.NodeManagerImpl;
import com.intellij.debugger.ui.impl.watch.StaticDescriptorImpl;
import com.intellij.icons.AllIcons;
import com.intellij.xdebugger.frame.XCompositeNode;
import com.intellij.xdebugger.frame.XValueChildrenList;
import com.intellij.xdebugger.frame.XValueGroup;
import consulo.internal.com.sun.jdi.Field;
import consulo.internal.com.sun.jdi.ReferenceType;

/**
 * @author egor
 */
public class JavaStaticGroup extends XValueGroup implements NodeDescriptorProvider
{
	private final StaticDescriptorImpl myStaticDescriptor;
	private final EvaluationContextImpl myEvaluationContext;
	private final NodeManagerImpl myNodeManager;

	public JavaStaticGroup(
			StaticDescriptorImpl staticDescriptor, EvaluationContextImpl evaluationContext, NodeManagerImpl nodeManager)
	{
		super(staticDescriptor.getName());
		myStaticDescriptor = staticDescriptor;
		myEvaluationContext = evaluationContext;
		myNodeManager = nodeManager;
	}

	@NotNull
	@Override
	public String getSeparator()
	{
		return "";
	}

	@Nullable
	@Override
	public Icon getIcon()
	{
		return AllIcons.Nodes.Static;
	}

	@Override
	public NodeDescriptorImpl getDescriptor()
	{
		return myStaticDescriptor;
	}

	@Override
	public void computeChildren(@NotNull final XCompositeNode node)
	{
		myEvaluationContext.getDebugProcess().getManagerThread().schedule(new SuspendContextCommandImpl(myEvaluationContext.getSuspendContext())
		{
			@Override
			public void contextAction() throws Exception
			{
				final XValueChildrenList children = new XValueChildrenList();

				final ReferenceType refType = myStaticDescriptor.getType();
				List<Field> fields = refType.allFields();
				for(Field field : fields)
				{
					if(field.isStatic())
					{
						final FieldDescriptorImpl fieldDescriptor = myNodeManager.getFieldDescriptor(myStaticDescriptor, null, field);
						children.add(JavaValue.create(fieldDescriptor, myEvaluationContext, myNodeManager));
						//final DebuggerTreeNodeImpl node = myNodeManager.createNode(fieldDescriptor, myEvaluationContext);
						//myChildren.add(node);
					}
				}

				node.addChildren(children, true);
			}
		});
	}
}
