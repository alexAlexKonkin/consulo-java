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
package com.intellij.java.debugger.impl.ui.breakpoints;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.breakpoint.XLineBreakpoint;
import org.jdom.Element;
import com.intellij.java.debugger.impl.breakpoints.properties.JavaBreakpointProperties;
import com.intellij.java.debugger.DebuggerBundle;
import com.intellij.java.debugger.impl.DebuggerInvocationUtil;
import com.intellij.java.debugger.impl.DebuggerManagerEx;
import com.intellij.java.debugger.impl.InstanceFilter;
import com.intellij.java.debugger.SourcePosition;
import com.intellij.java.debugger.engine.DebugProcess;
import com.intellij.java.debugger.impl.engine.DebugProcessImpl;
import com.intellij.java.debugger.impl.engine.DebuggerManagerThreadImpl;
import com.intellij.java.debugger.impl.engine.JVMNameUtil;
import com.intellij.java.debugger.impl.engine.events.DebuggerCommandImpl;
import com.intellij.java.debugger.impl.engine.requests.RequestManagerImpl;
import com.intellij.java.debugger.impl.DebuggerContextImpl;
import com.intellij.java.debugger.impl.DebuggerUtilsEx;
import consulo.application.ApplicationManager;
import consulo.application.ReadAction;
import consulo.logging.Logger;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.project.Project;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import com.intellij.java.language.psi.PsiClass;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import com.intellij.java.debugger.ui.classFilter.ClassFilter;
import consulo.execution.debug.XDebuggerManager;
import consulo.execution.debug.breakpoint.XBreakpoint;
import consulo.util.lang.xml.CommonXmlStrings;
import consulo.internal.com.sun.jdi.Location;
import consulo.internal.com.sun.jdi.ReferenceType;
import consulo.ui.image.Image;

public abstract class BreakpointWithHighlighter<P extends JavaBreakpointProperties> extends Breakpoint<P>
{
	private static final Logger LOG = Logger.getInstance(BreakpointWithHighlighter.class);

	@Nullable
	private SourcePosition mySourcePosition;

	private boolean myVisible = true;
	private volatile Image myIcon = getSetIcon(false);
	@Nullable
	private String myClassName;
	@Nullable
	private String myPackageName;
	@Nullable
	private String myInvalidMessage;

	protected abstract void createRequestForPreparedClass(final DebugProcessImpl debugProcess, final ReferenceType classType);

	protected abstract Image getDisabledIcon(boolean isMuted);

	protected abstract Image getInvalidIcon(boolean isMuted);

	protected Image getSetIcon(boolean isMuted)
	{
		return null;
	}

	protected abstract Image getVerifiedIcon(boolean isMuted);

	protected abstract Image getVerifiedWarningsIcon(boolean isMuted);

	@Override
	public Image getIcon()
	{
		return myIcon;
	}

	@Nullable
	@Override
	public String getClassName()
	{
		return myClassName;
	}

	@Override
	@Nullable
	public String getShortClassName()
	{
		final SourcePosition pos = getSourcePosition();
		if(pos != null)
		{
			/*if(pos.getFile() instanceof JspFile)
			{
				return getClassName();
			} */
		}
		return super.getShortClassName();
	}

	@Nullable
	@Override
	public String getPackageName()
	{
		return myPackageName;
	}

	@Nullable
	public BreakpointWithHighlighter init()
	{
		if(!isValid())
		{
			return null;
		}

		if(!ApplicationManager.getApplication().isUnitTestMode())
		{
			updateUI();
			updateGutter();
		}

		return this;
	}

	private void updateCaches(DebugProcessImpl debugProcess)
	{
		myIcon = calcIcon(debugProcess);
		myClassName = JVMNameUtil.getSourcePositionClassDisplayName(debugProcess, getSourcePosition());
		myPackageName = JVMNameUtil.getSourcePositionPackageDisplayName(debugProcess, getSourcePosition());
	}

	private Image calcIcon(@Nullable DebugProcessImpl debugProcess)
	{
		final boolean muted = debugProcess != null && isMuted(debugProcess);
		if(!isEnabled())
		{
			return getDisabledIcon(muted);
		}

		myInvalidMessage = "";

		if(!isValid())
		{
			return getInvalidIcon(muted);
		}

		if(debugProcess == null)
		{
			return getSetIcon(muted);
		}

		final RequestManagerImpl requestsManager = debugProcess.getRequestsManager();

		final boolean isVerified = myCachedVerifiedState || requestsManager.isVerified(this);

		final String warning = requestsManager.getWarning(this);
		if(warning != null)
		{
			myInvalidMessage = warning;
			if(!isVerified)
			{
				return getInvalidIcon(muted);
			}
			return getVerifiedWarningsIcon(muted);
		}

		if(isVerified)
		{
			return getVerifiedIcon(muted);
		}

		return getSetIcon(muted);
	}

	protected BreakpointWithHighlighter(@Nonnull Project project, XBreakpoint xBreakpoint)
	{
		//for persistency
		super(project, xBreakpoint);
		ApplicationManager.getApplication().runReadAction((Runnable) this::reload);
	}

	@Override
	public boolean isValid()
	{
		return isPositionValid(myXBreakpoint.getSourcePosition());
	}

	protected static boolean isPositionValid(@Nullable final XSourcePosition sourcePosition)
	{
		return ReadAction.compute(() -> sourcePosition != null && sourcePosition.getFile().isValid()).booleanValue();
	}

	@Nullable
	public SourcePosition getSourcePosition()
	{
		return mySourcePosition;
	}

	@SuppressWarnings("HardCodedStringLiteral")
	@Nonnull
	public String getDescription()
	{
		final StringBuilder buf = new StringBuilder();
		buf.append(getDisplayName());

		if(isCountFilterEnabled())
		{
			buf.append("&nbsp;<br>&nbsp;");
			buf.append(DebuggerBundle.message("breakpoint.property.name.pass.count")).append(": ");
			buf.append(getCountFilter());
		}
		if(isClassFiltersEnabled())
		{
			buf.append("&nbsp;<br>&nbsp;");
			buf.append(DebuggerBundle.message("breakpoint.property.name.class.filters")).append(": ");
			ClassFilter[] classFilters = getClassFilters();
			for(ClassFilter classFilter : classFilters)
			{
				buf.append(classFilter.getPattern()).append(" ");
			}
		}
		if(isInstanceFiltersEnabled())
		{
			buf.append("&nbsp;<br>&nbsp;");
			buf.append(DebuggerBundle.message("breakpoint.property.name.instance.filters"));
			InstanceFilter[] instanceFilters = getInstanceFilters();
			for(InstanceFilter instanceFilter : instanceFilters)
			{
				buf.append(Long.toString(instanceFilter.getId())).append(" ");
			}
		}
		return buf.toString();
	}

	@Override
	public void reload()
	{
		ApplicationManager.getApplication().assertReadAccessAllowed();
		mySourcePosition = DebuggerUtilsEx.toSourcePosition(myXBreakpoint.getSourcePosition(), myProject);
		if(mySourcePosition != null)
		{
			reload(mySourcePosition.getFile());
		}
	}

	static void createLocationBreakpointRequest(@Nonnull FilteredRequestor requestor, @Nullable Location location, @Nonnull DebugProcessImpl debugProcess)
	{
		if(location != null)
		{
			RequestManagerImpl requestsManager = debugProcess.getRequestsManager();
			requestsManager.enableRequest(requestsManager.createBreakpointRequest(requestor, location));
		}
	}

	@Override
	public void createRequest(@Nonnull DebugProcessImpl debugProcess)
	{
		DebuggerManagerThreadImpl.assertIsManagerThread();
		// check is this breakpoint is enabled, vm reference is valid and there're no requests created yet
		if(!shouldCreateRequest(debugProcess))
		{
			return;
		}

		if(!isValid())
		{
			return;
		}

		SourcePosition position = getSourcePosition();
		if(position != null)
		{
			createOrWaitPrepare(debugProcess, position);
		}
		else
		{
			LOG.error("Unable to create request for breakpoint with null position: " + toString() + " at " + myXBreakpoint.getSourcePosition());
		}
		updateUI();
	}

	protected boolean isMuted(@Nonnull final DebugProcessImpl debugProcess)
	{
		return debugProcess.areBreakpointsMuted();
	}

	@Override
	public void processClassPrepare(DebugProcess debugProcess, ReferenceType classType)
	{
		DebugProcessImpl process = (DebugProcessImpl) debugProcess;
		if(shouldCreateRequest(process, true))
		{
			createRequestForPreparedClass(process, classType);
			updateUI();
		}
	}

	/**
	 * updates the state of breakpoint and all the related UI widgets etc
	 */
	@Override
	public final void updateUI()
	{
		if(!isVisible() || ApplicationManager.getApplication().isUnitTestMode())
		{
			return;
		}
		DebuggerInvocationUtil.swingInvokeLater(myProject, () ->
		{
			if(!isValid())
			{
				return;
			}

			DebuggerContextImpl context = DebuggerManagerEx.getInstanceEx(myProject).getContext();
			DebugProcessImpl debugProcess = context.getDebugProcess();
			if(debugProcess == null || !debugProcess.isAttached())
			{
				updateCaches(null);
				updateGutter();
			}
			else
			{
				debugProcess.getManagerThread().invoke(new DebuggerCommandImpl()
				{
					@Override
					protected void action() throws Exception
					{
						ApplicationManager.getApplication().runReadAction(() ->
						{
							if(!myProject.isDisposed())
							{
								updateCaches(debugProcess);
							}
						});
						DebuggerInvocationUtil.swingInvokeLater(myProject, BreakpointWithHighlighter.this::updateGutter);
					}
				});
			}
		});
	}

	private void updateGutter()
	{
		if(isVisible() && isValid())
		{
			XDebuggerManager.getInstance(myProject).getBreakpointManager().updateBreakpointPresentation((XLineBreakpoint) myXBreakpoint, getIcon(), myInvalidMessage);
		}
	}

	public boolean isAt(@Nonnull Document document, int offset)
	{
		final VirtualFile file = FileDocumentManager.getInstance().getFile(document);
		int line = document.getLineNumber(offset);
		XSourcePosition position = myXBreakpoint.getSourcePosition();
		return position != null && position.getLine() == line && position.getFile().equals(file);
	}

	protected void reload(PsiFile psiFile)
	{
	}

	@Override
	public PsiClass getPsiClass()
	{
		final SourcePosition sourcePosition = getSourcePosition();
		return getPsiClassAt(sourcePosition);
	}

	protected static PsiClass getPsiClassAt(@Nullable final SourcePosition sourcePosition)
	{
		return ReadAction.compute(() -> JVMNameUtil.getClassAt(sourcePosition));
	}

	@Override
	public abstract Key<? extends BreakpointWithHighlighter> getCategory();

	protected boolean isVisible()
	{
		return myVisible;
	}

	public void setVisible(boolean visible)
	{
		myVisible = visible;
	}

	@Nullable
	public Document getDocument()
	{
		PsiFile file = DebuggerUtilsEx.getPsiFile(myXBreakpoint.getSourcePosition(), myProject);
		if(file != null)
		{
			return PsiDocumentManager.getInstance(myProject).getDocument(file);
		}
		return null;
	}

	public int getLineIndex()
	{
		XSourcePosition sourcePosition = myXBreakpoint.getSourcePosition();
		return sourcePosition != null ? sourcePosition.getLine() : -1;
	}

	protected String getFileName()
	{
		XSourcePosition sourcePosition = myXBreakpoint.getSourcePosition();
		return sourcePosition != null ? sourcePosition.getFile().getName() : "";
	}

	@Override
	public void readExternal(@Nonnull Element breakpointNode) throws InvalidDataException
	{
		super.readExternal(breakpointNode);
		//noinspection HardCodedStringLiteral
		//final String url = breakpointNode.getAttributeValue("url");

		//noinspection HardCodedStringLiteral
		final String className = breakpointNode.getAttributeValue("class");
		if(className != null)
		{
			myClassName = className;
		}

		//noinspection HardCodedStringLiteral
		final String packageName = breakpointNode.getAttributeValue("package");
		if(packageName != null)
		{
			myPackageName = packageName;
		}
	}

	public String toString()
	{
		return ReadAction.compute(() -> CommonXmlStrings.HTML_START + CommonXmlStrings.BODY_START + getDescription() + CommonXmlStrings.BODY_END + CommonXmlStrings.HTML_END);
	}
}
