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

/*
 * Class MethodBreakpoint
 * @author Jeka
 */
package com.intellij.debugger.ui.breakpoints;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.Icon;

import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jetbrains.java.debugger.breakpoints.properties.JavaMethodBreakpointProperties;
import org.jetbrains.org.objectweb.asm.Label;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Opcodes;
import com.intellij.debugger.DebuggerBundle;
import com.intellij.debugger.DebuggerManagerEx;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.DebuggerManagerThreadImpl;
import com.intellij.debugger.engine.JVMName;
import com.intellij.debugger.engine.JVMNameUtil;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl;
import com.intellij.debugger.engine.requests.RequestManagerImpl;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.impl.PositionUtil;
import com.intellij.debugger.jdi.MethodBytecodeUtil;
import com.intellij.debugger.requests.Requestor;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.ProgressWindow;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointListener;
import consulo.internal.com.sun.jdi.AbsentInformationException;
import consulo.internal.com.sun.jdi.ClassType;
import consulo.internal.com.sun.jdi.InterfaceType;
import consulo.internal.com.sun.jdi.Location;
import consulo.internal.com.sun.jdi.Method;
import consulo.internal.com.sun.jdi.ReferenceType;
import consulo.internal.com.sun.jdi.event.LocatableEvent;
import consulo.internal.com.sun.jdi.event.MethodEntryEvent;
import consulo.internal.com.sun.jdi.event.MethodExitEvent;
import consulo.internal.com.sun.jdi.request.ClassPrepareRequest;
import consulo.internal.com.sun.jdi.request.EventRequest;
import consulo.internal.com.sun.jdi.request.MethodEntryRequest;
import consulo.internal.com.sun.jdi.request.MethodExitRequest;

public class MethodBreakpoint extends BreakpointWithHighlighter<JavaMethodBreakpointProperties> implements MethodBreakpointBase
{
	private static final Logger LOG = Logger.getInstance("#com.intellij.debugger.ui.breakpoints.MethodBreakpoint");
	@Nullable
	private JVMName mySignature;
	private boolean myIsStatic;

	public static final
	@NonNls
	Key<MethodBreakpoint> CATEGORY = BreakpointCategory.lookup("method_breakpoints");

	protected MethodBreakpoint(@Nonnull Project project, XBreakpoint breakpoint)
	{
		super(project, breakpoint);
	}

	public boolean isStatic()
	{
		return myIsStatic;
	}

	@Override
	@Nonnull
	public Key<MethodBreakpoint> getCategory()
	{
		return CATEGORY;
	}

	@Override
	public boolean isValid()
	{
		return super.isValid() && getMethodName() != null;
	}

	@Override
	protected void reload(@Nonnull PsiFile psiFile)
	{
		setMethodName(null);
		mySignature = null;

		MethodDescriptor descriptor = getMethodDescriptor(myProject, psiFile, getSourcePosition());
		if(descriptor != null)
		{
			setMethodName(descriptor.methodName);
			mySignature = descriptor.methodSignature;
			myIsStatic = descriptor.isStatic;
		}
		PsiClass psiClass = getPsiClass();
		if(psiClass != null)
		{
			getProperties().myClassPattern = psiClass.getQualifiedName();
		}
		if(myIsStatic)
		{
			setInstanceFiltersEnabled(false);
		}
	}

	private static void createRequestForSubClasses(@Nonnull MethodBreakpointBase breakpoint, @Nonnull DebugProcessImpl debugProcess, @Nonnull ReferenceType baseType)
	{
		DebuggerManagerThreadImpl.assertIsManagerThread();
		RequestManagerImpl requestsManager = debugProcess.getRequestsManager();
		ClassPrepareRequest request = requestsManager.createClassPrepareRequest((debuggerProcess, referenceType) ->
		{
			if(instanceOf(referenceType, baseType))
			{
				createRequestForPreparedClassEmulated(breakpoint, debugProcess, referenceType, false);
			}
		}, null);
		if(request != null)
		{
			requestsManager.registerRequest(breakpoint, request);
			request.enable();
			debugProcess.getVirtualMachineProxy().clearCaches(); // to force reload classes available so far
		}

		AtomicReference<ProgressWindow> indicatorRef = new AtomicReference<>();
		ApplicationManager.getApplication().invokeAndWait(() ->
		{
			ProgressWindow progress = new ProgressWindow(true, false, debugProcess.getProject(), "Cancel emulation");
			progress.setDelayInMillis(2000);
			indicatorRef.set(progress);
		});
		ProgressWindow indicator = indicatorRef.get();

		AtomicBoolean changed = new AtomicBoolean();
		XBreakpointListener<XBreakpoint<?>> listener = new XBreakpointListener<XBreakpoint<?>>()
		{
			void changed(@Nonnull XBreakpoint b)
			{
				if(b == breakpoint.getXBreakpoint())
				{
					changed.set(true);
					indicator.cancel();
				}
			}

			@Override
			public void breakpointRemoved(@Nonnull XBreakpoint b)
			{
				changed(b);
			}

			@Override
			public void breakpointChanged(@Nonnull XBreakpoint b)
			{
				changed(b);
			}
		};

		XDebuggerManager.getInstance(debugProcess.getProject()).getBreakpointManager().addBreakpointListener(listener, indicator);
		ProgressManager.getInstance().executeProcessUnderProgress(() -> processPreparedSubTypes(baseType, subType -> createRequestForPreparedClassEmulated(breakpoint, debugProcess, subType, false),
				indicator), indicator);
		if(indicator.isCanceled() && !changed.get())
		{
			breakpoint.disableEmulation();
		}
	}

	@Override
	public void disableEmulation()
	{
		MethodBreakpointBase.disableEmulation(this);
	}

	static void createRequestForPreparedClassEmulated(@Nonnull MethodBreakpointBase breakpoint, @Nonnull DebugProcessImpl debugProcess, @Nonnull ReferenceType classType, boolean base)
	{
		if(!base && !shouldCreateRequest(breakpoint, breakpoint.getXBreakpoint(), debugProcess, true))
		{
			return;
		}
		try
		{
			Method lambdaMethod = MethodBytecodeUtil.getLambdaMethod(classType, debugProcess.getVirtualMachineProxy());
			List<Method> methods = lambdaMethod != null ? Collections.singletonList(lambdaMethod) : breakpoint.matchingMethods((classType.methods().stream().filter(m -> base || !m.isAbstract())
					.collect(Collectors.toList())), debugProcess);
			boolean found = false;
			for(Method method : methods)
			{
				found = true;
				if(base && method.isNative())
				{
					breakpoint.disableEmulation();
					return;
				}
				Method target = MethodBytecodeUtil.getBridgeTargetMethod(method, debugProcess.getVirtualMachineProxy());
				if(target != null && !DebuggerUtilsEx.allLineLocations(target).isEmpty())
				{
					method = target;
				}

				List<Location> allLineLocations = DebuggerUtilsEx.allLineLocations(method);
				if(!allLineLocations.isEmpty())
				{
					if(breakpoint.isWatchEntry())
					{
						createLocationBreakpointRequest(breakpoint, ContainerUtil.getFirstItem(allLineLocations), debugProcess);
					}
					if(breakpoint.isWatchExit())
					{
						MethodBytecodeUtil.visit(method, new MethodVisitor(Opcodes.API_VERSION)
						{
							int myLastLine = 0;

							@Override
							public void visitLineNumber(int line, Label start)
							{
								myLastLine = line;
							}

							@Override
							public void visitInsn(int opcode)
							{
								switch(opcode)
								{
									case Opcodes.RETURN:
									case Opcodes.IRETURN:
									case Opcodes.FRETURN:
									case Opcodes.ARETURN:
									case Opcodes.LRETURN:
									case Opcodes.DRETURN:
										//case Opcodes.ATHROW:
										allLineLocations.stream().filter(l -> l.lineNumber() == myLastLine).findFirst().ifPresent(location -> createLocationBreakpointRequest(breakpoint, location,
												debugProcess));
								}
							}
						}, true);
					}
				}
			}
			if(base && found)
			{
				// desired class found - now also track all new classes
				createRequestForSubClasses(breakpoint, debugProcess, classType);
			}

		}
		catch(Exception e)
		{
			LOG.debug(e);
		}
	}

	@Override
	protected void createRequestForPreparedClass(@Nonnull DebugProcessImpl debugProcess, @Nonnull ReferenceType classType)
	{
		if(isEmulated())
		{
			createRequestForPreparedClassEmulated(this, debugProcess, classType, true);
		}
		else
		{
			createRequestForPreparedClassOriginal(debugProcess, classType);
		}
	}

	private void createRequestForPreparedClassOriginal(@Nonnull DebugProcessImpl debugProcess, @Nonnull ReferenceType classType)
	{
		try
		{
			boolean hasMethod = false;
			for(Method method : classType.allMethods())
			{
				String signature = method.signature();
				String name = method.name();

				if(getMethodName().equals(name) && mySignature.getName(debugProcess).equals(signature))
				{
					hasMethod = true;
					break;
				}
			}

			if(!hasMethod)
			{
				debugProcess.getRequestsManager().setInvalid(this, DebuggerBundle.message("error.invalid.breakpoint.method.not.found", classType.name()));
				return;
			}

			RequestManagerImpl requestManager = debugProcess.getRequestsManager();
			if(isWatchEntry())
			{
				MethodEntryRequest entryRequest = findRequest(debugProcess, MethodEntryRequest.class, this);
				if(entryRequest == null)
				{
					entryRequest = requestManager.createMethodEntryRequest(this);
				}
				else
				{
					entryRequest.disable();
				}
				//entryRequest.addClassFilter(myClassQualifiedName);
				// use addClassFilter(ReferenceType) in order to stop on subclasses also!
				entryRequest.addClassFilter(classType);
				debugProcess.getRequestsManager().enableRequest(entryRequest);
			}
			if(isWatchExit())
			{
				MethodExitRequest exitRequest = findRequest(debugProcess, MethodExitRequest.class, this);
				if(exitRequest == null)
				{
					exitRequest = requestManager.createMethodExitRequest(this);
				}
				else
				{
					exitRequest.disable();
				}
				//exitRequest.addClassFilter(myClassQualifiedName);
				exitRequest.addClassFilter(classType);
				debugProcess.getRequestsManager().enableRequest(exitRequest);
			}
		}
		catch(Exception e)
		{
			LOG.debug(e);
		}
	}


	@Override
	public String getEventMessage(@Nonnull LocatableEvent event)
	{
		final Location location = event.location();
		final String locationQName = DebuggerUtilsEx.getLocationMethodQName(location);
		String locationFileName;
		try
		{
			locationFileName = location.sourceName();
		}
		catch(AbsentInformationException e)
		{
			locationFileName = getFileName();
		}
		final int locationLine = location.lineNumber();
		if(event instanceof MethodEntryEvent)
		{
			MethodEntryEvent entryEvent = (MethodEntryEvent) event;
			final Method method = entryEvent.method();
			return DebuggerBundle.message("status.method.entry.breakpoint.reached", method.declaringType().name() + "." + method.name() + "()", locationQName, locationFileName, locationLine);
		}
		if(event instanceof MethodExitEvent)
		{
			MethodExitEvent exitEvent = (MethodExitEvent) event;
			final Method method = exitEvent.method();
			return DebuggerBundle.message("status.method.exit.breakpoint.reached", method.declaringType().name() + "." + method.name() + "()", locationQName, locationFileName, locationLine);
		}
		return "";
	}

	@Override
	public PsiElement getEvaluationElement()
	{
		return getPsiClass();
	}

	@Override
	protected Icon getDisabledIcon(boolean isMuted)
	{
		final Breakpoint master = DebuggerManagerEx.getInstanceEx(myProject).getBreakpointManager().findMasterBreakpoint(this);
		if(master != null)
		{
			return isMuted ? AllIcons.Debugger.Db_muted_dep_method_breakpoint : AllIcons.Debugger.Db_dep_method_breakpoint;
		}
		return null;
	}

	@Override
	@Nonnull
	protected Icon getInvalidIcon(boolean isMuted)
	{
		return isMuted ? AllIcons.Debugger.Db_muted_invalid_method_breakpoint : AllIcons.Debugger.Db_invalid_method_breakpoint;
	}

	@Override
	@Nonnull
	protected Icon getVerifiedIcon(boolean isMuted)
	{
		return isMuted ? AllIcons.Debugger.Db_muted_verified_method_breakpoint : AllIcons.Debugger.Db_verified_method_breakpoint;
	}

	@Override
	@Nonnull
	protected Icon getVerifiedWarningsIcon(boolean isMuted)
	{
		return isMuted ? AllIcons.Debugger.Db_muted_method_warning_breakpoint : AllIcons.Debugger.Db_method_warning_breakpoint;
	}

	@Override
	public String getDisplayName()
	{
		final StringBuilder buffer = new StringBuilder();
		if(isValid())
		{
			final String className = getClassName();
			final boolean classNameExists = className != null && className.length() > 0;
			if(classNameExists)
			{
				buffer.append(className);
			}
			if(getMethodName() != null)
			{
				if(classNameExists)
				{
					buffer.append(".");
				}
				buffer.append(getMethodName());
			}
		}
		else
		{
			buffer.append(DebuggerBundle.message("status.breakpoint.invalid"));
		}
		return buffer.toString();
	}

	@Override
	public boolean evaluateCondition(@Nonnull EvaluationContextImpl context, @Nonnull LocatableEvent event) throws EvaluateException
	{
		if(!matchesEvent(event, context.getDebugProcess()))
		{
			return false;
		}
		return super.evaluateCondition(context, event);
	}

	public boolean matchesEvent(@Nonnull final LocatableEvent event, final DebugProcessImpl process) throws EvaluateException
	{
		if(isEmulated())
		{
			return true;
		}
		if(getMethodName() == null || mySignature == null)
		{
			return false;
		}
		final Method method = event.location().method();
		return method != null && method.name().equals(getMethodName()) && method.signature().equals(mySignature.getName(process));
	}

	@Nullable
	public static MethodBreakpoint create(@Nonnull Project project, XBreakpoint xBreakpoint)
	{
		final MethodBreakpoint breakpoint = new MethodBreakpoint(project, xBreakpoint);
		return (MethodBreakpoint) breakpoint.init();
	}


	//public boolean canMoveTo(final SourcePosition position) {
	//  return super.canMoveTo(position) && PositionUtil.getPsiElementAt(getProject(), PsiMethod.class, position) != null;
	//}

	/**
	 * finds FQ method's class name and method's signature
	 */
	@javax.annotation.Nullable
	private static MethodDescriptor getMethodDescriptor(@Nonnull final Project project, @Nonnull final PsiFile psiJavaFile, @Nonnull final SourcePosition sourcePosition)
	{
		final PsiDocumentManager docManager = PsiDocumentManager.getInstance(project);
		final Document document = docManager.getDocument(psiJavaFile);
		if(document == null)
		{
			return null;
		}
		//final int endOffset = document.getLineEndOffset(sourcePosition);
		//final MethodDescriptor descriptor = docManager.commitAndRunReadAction(new Computable<MethodDescriptor>() {
		// conflicts with readAction on initial breakpoints creation
		final MethodDescriptor descriptor = ReadAction.compute(() ->
		{
			//PsiMethod method = DebuggerUtilsEx.findPsiMethod(psiJavaFile, endOffset);
			PsiMethod method = PositionUtil.getPsiElementAt(project, PsiMethod.class, sourcePosition);
			if(method == null)
			{
				return null;
			}
			final int methodOffset = method.getTextOffset();
			if(methodOffset < 0)
			{
				return null;
			}
			if(document.getLineNumber(methodOffset) < sourcePosition.getLine())
			{
				return null;
			}

			final PsiIdentifier identifier = method.getNameIdentifier();
			int methodNameOffset = identifier != null ? identifier.getTextOffset() : methodOffset;
			final MethodDescriptor res = new MethodDescriptor();
			res.methodName = JVMNameUtil.getJVMMethodName(method);
			try
			{
				res.methodSignature = JVMNameUtil.getJVMSignature(method);
				res.isStatic = method.hasModifierProperty(PsiModifier.STATIC);
			}
			catch(IndexNotReadyException ignored)
			{
				return null;
			}
			res.methodLine = document.getLineNumber(methodNameOffset);
			return res;
		});
		if(descriptor == null || descriptor.methodName == null || descriptor.methodSignature == null)
		{
			return null;
		}
		return descriptor;
	}

	@javax.annotation.Nullable
	@SuppressWarnings("unchecked")
	static <T extends EventRequest> T findRequest(@Nonnull DebugProcessImpl debugProcess, Class<T> requestClass, Requestor requestor)
	{
		return (T) debugProcess.getRequestsManager().findRequests(requestor).stream().filter(eventRequest -> requestClass.isInstance(eventRequest)).findFirst().orElse(null);
	}

	@Override
	public void readExternal(@Nonnull Element breakpointNode) throws InvalidDataException
	{
		super.readExternal(breakpointNode);
		try
		{
			getProperties().WATCH_ENTRY = Boolean.valueOf(JDOMExternalizerUtil.readField(breakpointNode, "WATCH_ENTRY"));
		}
		catch(Exception ignored)
		{
		}
		try
		{
			getProperties().WATCH_EXIT = Boolean.valueOf(JDOMExternalizerUtil.readField(breakpointNode, "WATCH_EXIT"));
		}
		catch(Exception ignored)
		{
		}
	}

	public boolean isEmulated()
	{
		return getProperties().EMULATED;
	}

	@Override
	public boolean isWatchEntry()
	{
		return getProperties().WATCH_ENTRY;
	}

	@Override
	public boolean isWatchExit()
	{
		return getProperties().WATCH_EXIT;
	}

	@Override
	public List<Method> matchingMethods(List<Method> methods, DebugProcessImpl debugProcess)
	{
		try
		{
			String methodName = getMethodName();
			String signature = mySignature != null ? mySignature.getName(debugProcess) : null;
			return methods.stream().filter(m -> Comparing.equal(methodName, m.name()) && Comparing.equal(signature, m.signature())).limit(1).collect(Collectors.toList());
		}
		catch(EvaluateException e)
		{
			LOG.warn(e);
		}
		return Collections.emptyList();
	}

	@Nullable
	private String getMethodName()
	{
		return getProperties().myMethodName;
	}

	private void setMethodName(@Nullable String methodName)
	{
		getProperties().myMethodName = methodName;
	}

	private static final class MethodDescriptor
	{
		String methodName;
		JVMName methodSignature;
		boolean isStatic;
		int methodLine;
	}

	private static boolean instanceOf(@javax.annotation.Nullable ReferenceType type, @Nonnull ReferenceType superType)
	{
		if(type == null)
		{
			return false;
		}
		if(superType.equals(type))
		{
			return true;
		}
		return supertypes(type).anyMatch(t -> instanceOf(t, superType));
	}

	private static Stream<? extends ReferenceType> supertypes(ReferenceType type)
	{
		if(type instanceof InterfaceType)
		{
			return ((InterfaceType) type).superinterfaces().stream();
		}
		else if(type instanceof ClassType)
		{
			ClassType superclass = ((ClassType) type).superclass();
			List<InterfaceType> interfaces = ((ClassType) type).interfaces();
			if(superclass != null)
			{
				List<ReferenceType> list = new ArrayList<>(interfaces.size() + 1);
				list.addAll(interfaces);
				list.add(superclass);
				return list.stream();
			}
			return interfaces.stream();
		}
		return Stream.empty();
	}

	private static void processPreparedSubTypes(ReferenceType classType, Consumer<ReferenceType> consumer, ProgressIndicator progressIndicator)
	{
		long start = 0;
		if(LOG.isDebugEnabled())
		{
			start = System.currentTimeMillis();
		}
		progressIndicator.start();
		progressIndicator.setText(DebuggerBundle.message("label.method.breakpoints.processing.classes"));
		try
		{
			MultiMap<ReferenceType, ReferenceType> inheritance = new MultiMap<>();
			List<ReferenceType> allTypes = classType.virtualMachine().allClasses();
			for(int i = 0; i < allTypes.size(); i++)
			{
				if(progressIndicator.isCanceled())
				{
					return;
				}
				ReferenceType type = allTypes.get(i);
				if(type.isPrepared())
				{
					supertypes(type).forEach(st -> inheritance.putValue(st, type));
				}
				progressIndicator.setText2(i + "/" + allTypes.size());
				progressIndicator.setFraction((double) i / allTypes.size());
			}
			List<ReferenceType> types = inheritance.get(classType).stream().skip(1).collect(Collectors.toList());

			progressIndicator.setText(DebuggerBundle.message("label.method.breakpoints.setting.breakpoints"));

			for(int i = 0; i < types.size(); i++)
			{
				if(progressIndicator.isCanceled())
				{
					return;
				}
				consumer.accept(types.get(i));

				progressIndicator.setText2(i + "/" + types.size());
				progressIndicator.setFraction((double) i / types.size());
			}

			if(LOG.isDebugEnabled())
			{
				LOG.debug("Processed " + types.size() + " classes in " + String.valueOf(System.currentTimeMillis() - start) + "ms");
			}
		}
		finally
		{
			progressIndicator.stop();
		}
	}
}
