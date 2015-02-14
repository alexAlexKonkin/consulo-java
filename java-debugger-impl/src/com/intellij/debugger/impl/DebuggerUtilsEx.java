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

/*
 * Class DebuggerUtilsEx
 * @author Jeka
 */
package com.intellij.debugger.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.PatternSyntaxException;

import org.jdom.Attribute;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.debugger.DebuggerBundle;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.DebuggerManagerThreadImpl;
import com.intellij.debugger.engine.DebuggerUtils;
import com.intellij.debugger.engine.SuspendContextImpl;
import com.intellij.debugger.engine.evaluation.CodeFragmentFactory;
import com.intellij.debugger.engine.evaluation.CodeFragmentFactoryContextWrapper;
import com.intellij.debugger.engine.evaluation.CodeFragmentKind;
import com.intellij.debugger.engine.evaluation.DefaultCodeFragmentFactory;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluationContext;
import com.intellij.debugger.engine.evaluation.TextWithImports;
import com.intellij.debugger.engine.evaluation.TextWithImportsImpl;
import com.intellij.debugger.engine.evaluation.expression.EvaluatorBuilder;
import com.intellij.debugger.engine.requests.RequestManagerImpl;
import com.intellij.debugger.jdi.VirtualMachineProxyImpl;
import com.intellij.debugger.requests.Requestor;
import com.intellij.debugger.ui.CompletionEditor;
import com.intellij.debugger.ui.breakpoints.Breakpoint;
import com.intellij.debugger.ui.tree.DebuggerTreeNode;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizable;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLambdaExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.ui.classFilter.ClassFilter;
import com.intellij.util.SmartList;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.impl.XSourcePositionImpl;
import consulo.internal.com.sun.jdi.CharValue;
import consulo.internal.com.sun.jdi.ClassType;
import consulo.internal.com.sun.jdi.InterfaceType;
import consulo.internal.com.sun.jdi.Method;
import consulo.internal.com.sun.jdi.ReferenceType;
import consulo.internal.com.sun.jdi.StringReference;
import consulo.internal.com.sun.jdi.ThreadReference;
import consulo.internal.com.sun.jdi.Value;
import consulo.internal.com.sun.jdi.event.Event;
import consulo.internal.com.sun.jdi.event.EventSet;

public abstract class DebuggerUtilsEx extends DebuggerUtils
{
	private static final Logger LOG = Logger.getInstance("#com.intellij.debugger.impl.DebuggerUtilsEx");

	private static final int MAX_LABEL_SIZE = 255;

	/**
	 * @param context
	 * @return all CodeFragmentFactoryProviders that provide code fragment factories suitable in the context given
	 */
	public static List<CodeFragmentFactory> getCodeFragmentFactories(@Nullable PsiElement context)
	{
		final DefaultCodeFragmentFactory defaultFactory = DefaultCodeFragmentFactory.getInstance();
		final CodeFragmentFactory[] providers = ApplicationManager.getApplication().getExtensions(CodeFragmentFactory.EXTENSION_POINT_NAME);
		final List<CodeFragmentFactory> suitableFactories = new ArrayList<CodeFragmentFactory>(providers.length);
		if(providers.length > 0)
		{
			for(CodeFragmentFactory factory : providers)
			{
				if(factory != defaultFactory && factory.isContextAccepted(context))
				{
					suitableFactories.add(factory);
				}
			}
		}
		suitableFactories.add(defaultFactory); // let default factory be the last one
		return suitableFactories;
	}


	public static PsiMethod findPsiMethod(PsiFile file, int offset)
	{
		PsiElement element = null;

		while(offset >= 0)
		{
			element = file.findElementAt(offset);
			if(element != null)
			{
				break;
			}
			offset--;
		}

		for(; element != null; element = element.getParent())
		{
			if(element instanceof PsiClass || element instanceof PsiLambdaExpression)
			{
				return null;
			}
			if(element instanceof PsiMethod)
			{
				return (PsiMethod) element;
			}
		}
		return null;
	}


	public static boolean isAssignableFrom(final String baseQualifiedName, ReferenceType checkedType)
	{
		if(CommonClassNames.JAVA_LANG_OBJECT.equals(baseQualifiedName))
		{
			return true;
		}
		return getSuperClass(baseQualifiedName, checkedType) != null;
	}

	public static ReferenceType getSuperClass(final String baseQualifiedName, ReferenceType checkedType)
	{
		if(baseQualifiedName.equals(checkedType.name()))
		{
			return checkedType;
		}

		if(checkedType instanceof ClassType)
		{
			ClassType classType = (ClassType) checkedType;
			ClassType superClassType = classType.superclass();
			if(superClassType != null)
			{
				ReferenceType superClass = getSuperClass(baseQualifiedName, superClassType);
				if(superClass != null)
				{
					return superClass;
				}
			}
			List<InterfaceType> interfaces = classType.allInterfaces();
			for(InterfaceType iface : interfaces)
			{
				ReferenceType superClass = getSuperClass(baseQualifiedName, iface);
				if(superClass != null)
				{
					return superClass;
				}
			}
		}

		if(checkedType instanceof InterfaceType)
		{
			List<InterfaceType> list = ((InterfaceType) checkedType).superinterfaces();
			for(InterfaceType superInterface : list)
			{
				ReferenceType superClass = getSuperClass(baseQualifiedName, superInterface);
				if(superClass != null)
				{
					return superClass;
				}
			}
		}
		return null;
	}

	public static boolean valuesEqual(Value val1, Value val2)
	{
		if(val1 == null)
		{
			return val2 == null;
		}
		if(val2 == null)
		{
			return false;
		}
		if(val1 instanceof StringReference && val2 instanceof StringReference)
		{
			return ((StringReference) val1).value().equals(((StringReference) val2).value());
		}
		return val1.equals(val2);
	}

	public static String getValueOrErrorAsString(final EvaluationContext evaluationContext, Value value)
	{
		try
		{
			return getValueAsString(evaluationContext, value);
		}
		catch(EvaluateException e)
		{
			return e.getMessage();
		}
	}

	public static boolean isCharOrInteger(Value value)
	{
		return value instanceof CharValue || isInteger(value);
	}

	private static Set<String> myCharOrIntegers;

	@SuppressWarnings({"HardCodedStringLiteral"})
	public static boolean isCharOrIntegerArray(Value value)
	{
		if(value == null)
		{
			return false;
		}
		if(myCharOrIntegers == null)
		{
			myCharOrIntegers = new HashSet<String>();
			myCharOrIntegers.add("C");
			myCharOrIntegers.add("B");
			myCharOrIntegers.add("S");
			myCharOrIntegers.add("I");
			myCharOrIntegers.add("J");
		}

		String signature = value.type().signature();
		int i;
		for(i = 0; signature.charAt(i) == '['; i++)
		{
			;
		}
		if(i == 0)
		{
			return false;
		}
		signature = signature.substring(i, signature.length());
		return myCharOrIntegers.contains(signature);
	}

	public static ClassFilter create(Element element) throws InvalidDataException
	{
		ClassFilter filter = new ClassFilter();
		filter.readExternal(element);
		return filter;
	}

	private static boolean isFiltered(ClassFilter classFilter, String qName)
	{
		if(!classFilter.isEnabled())
		{
			return false;
		}
		try
		{
			if(classFilter.matches(qName))
			{
				return true;
			}
		}
		catch(PatternSyntaxException e)
		{
			LOG.debug(e);
		}
		return false;
	}

	public static boolean isFiltered(String qName, ClassFilter[] classFilters)
	{
		return isFiltered(qName, Arrays.asList(classFilters));
	}

	public static boolean isFiltered(String qName, List<ClassFilter> classFilters)
	{
		if(qName.indexOf('[') != -1)
		{
			return false; //is array
		}

		for(ClassFilter filter : classFilters)
		{
			if(isFiltered(filter, qName))
			{
				return true;
			}
		}
		return false;
	}

	public static int getEnabledNumber(ClassFilter[] classFilters)
	{
		int res = 0;
		for(ClassFilter filter : classFilters)
		{
			if(filter.isEnabled())
			{
				res++;
			}
		}
		return res;
	}

	public static ClassFilter[] readFilters(List children) throws InvalidDataException
	{
		if(children == null || children.size() == 0)
		{
			return ClassFilter.EMPTY_ARRAY;
		}
		List<ClassFilter> classFiltersList = new ArrayList<ClassFilter>(children.size());
		for(Object aChildren : children)
		{
			final ClassFilter classFilter = new ClassFilter();
			classFilter.readExternal((Element) aChildren);
			classFiltersList.add(classFilter);
		}
		return classFiltersList.toArray(new ClassFilter[classFiltersList.size()]);
	}

	public static void writeFilters(Element parentNode, @NonNls String tagName, ClassFilter[] filters) throws WriteExternalException
	{
		for(ClassFilter filter : filters)
		{
			Element element = new Element(tagName);
			parentNode.addContent(element);
			filter.writeExternal(element);
		}
	}

	public static boolean filterEquals(ClassFilter[] filters1, ClassFilter[] filters2)
	{
		if(filters1.length != filters2.length)
		{
			return false;
		}
		final Set<ClassFilter> f1 = new HashSet<ClassFilter>(Math.max((int) (filters1.length / .75f) + 1, 16));
		final Set<ClassFilter> f2 = new HashSet<ClassFilter>(Math.max((int) (filters2.length / .75f) + 1, 16));
		Collections.addAll(f1, filters1);
		Collections.addAll(f2, filters2);
		return f2.equals(f1);
	}

	private static boolean elementListsEqual(List<Element> l1, List<Element> l2)
	{
		if(l1 == null)
		{
			return l2 == null;
		}
		if(l2 == null)
		{
			return false;
		}

		if(l1.size() != l2.size())
		{
			return false;
		}

		Iterator<Element> i1 = l1.iterator();

		for(Element aL2 : l2)
		{
			Element elem1 = i1.next();

			if(!elementsEqual(elem1, aL2))
			{
				return false;
			}
		}
		return true;
	}

	private static boolean attributeListsEqual(List<Attribute> l1, List<Attribute> l2)
	{
		if(l1 == null)
		{
			return l2 == null;
		}
		if(l2 == null)
		{
			return false;
		}

		if(l1.size() != l2.size())
		{
			return false;
		}

		Iterator<Attribute> i1 = l1.iterator();

		for(Attribute aL2 : l2)
		{
			Attribute attr1 = i1.next();

			if(!Comparing.equal(attr1.getName(), aL2.getName()) || !Comparing.equal(attr1.getValue(), aL2.getValue()))
			{
				return false;
			}
		}
		return true;
	}

	public static boolean elementsEqual(Element e1, Element e2)
	{
		if(e1 == null)
		{
			return e2 == null;
		}
		if(!Comparing.equal(e1.getName(), e2.getName()))
		{
			return false;
		}
		if(!elementListsEqual(e1.getChildren(), e2.getChildren()))
		{
			return false;
		}
		if(!attributeListsEqual(e1.getAttributes(), e2.getAttributes()))
		{
			return false;
		}
		return true;
	}

	@SuppressWarnings({"HardCodedStringLiteral"})
	public static boolean externalizableEqual(JDOMExternalizable e1, JDOMExternalizable e2)
	{
		Element root1 = new Element("root");
		Element root2 = new Element("root");
		try
		{
			e1.writeExternal(root1);
		}
		catch(WriteExternalException e)
		{
			LOG.debug(e);
		}
		try
		{
			e2.writeExternal(root2);
		}
		catch(WriteExternalException e)
		{
			LOG.debug(e);
		}

		return elementsEqual(root1, root2);
	}

	@NotNull
	public static List<Pair<Breakpoint, Event>> getEventDescriptors(SuspendContextImpl suspendContext)
	{
		DebuggerManagerThreadImpl.assertIsManagerThread();
		if(suspendContext == null)
		{
			return Collections.emptyList();
		}
		final EventSet events = suspendContext.getEventSet();
		if(events == null)
		{
			return Collections.emptyList();
		}
		final List<Pair<Breakpoint, Event>> eventDescriptors = new SmartList<Pair<Breakpoint, Event>>();

		final RequestManagerImpl requestManager = suspendContext.getDebugProcess().getRequestsManager();
		for(final Event event : events)
		{
			final Requestor requestor = requestManager.findRequestor(event.request());
			if(requestor instanceof Breakpoint)
			{
				eventDescriptors.add(Pair.create((Breakpoint) requestor, event));
			}
		}
		return eventDescriptors;
	}

	public static TextWithImports getEditorText(final Editor editor)
	{
		if(editor == null)
		{
			return null;
		}
		final Project project = editor.getProject();
		if(project == null)
		{
			return null;
		}

		String defaultExpression = editor.getSelectionModel().getSelectedText();
		if(defaultExpression == null)
		{
			int offset = editor.getCaretModel().getOffset();
			PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
			if(psiFile != null)
			{
				PsiElement elementAtCursor = psiFile.findElementAt(offset);
				if(elementAtCursor != null)
				{
					final EditorTextProvider textProvider = EditorTextProvider.EP.forLanguage(elementAtCursor.getLanguage());
					if(textProvider != null)
					{
						final TextWithImports editorText = textProvider.getEditorText(elementAtCursor);
						if(editorText != null)
						{
							return editorText;
						}
					}
				}
			}
		}
		else
		{
			return new TextWithImportsImpl(CodeFragmentKind.EXPRESSION, defaultExpression);
		}
		return null;
	}

	public abstract DebuggerTreeNode getSelectedNode(DataContext context);

	public abstract EvaluatorBuilder getEvaluatorBuilder();

	public abstract CompletionEditor createEditor(Project project, PsiElement context, @NonNls String recentsId);

	@Nullable
	public static CodeFragmentFactory getEffectiveCodeFragmentFactory(final PsiElement psiContext)
	{
		final CodeFragmentFactory factory = ApplicationManager.getApplication().runReadAction(new Computable<CodeFragmentFactory>()
		{
			@Override
			public CodeFragmentFactory compute()
			{
				final List<CodeFragmentFactory> codeFragmentFactories = getCodeFragmentFactories(psiContext);
				// the list always contains at least DefaultCodeFragmentFactory
				return codeFragmentFactories.get(0);
			}
		});
		return factory != null ? new CodeFragmentFactoryContextWrapper(factory) : null;
	}

	private static class SigReader
	{
		final String buffer;
		int pos = 0;

		SigReader(String s)
		{
			buffer = s;
		}

		int get()
		{
			return buffer.charAt(pos++);
		}

		int peek()
		{
			return buffer.charAt(pos);
		}

		boolean eof()
		{
			return buffer.length() <= pos;
		}

		@NonNls
		String getSignature()
		{
			if(eof())
			{
				return "";
			}

			switch(get())
			{
				case 'Z':
					return "boolean";
				case 'B':
					return "byte";
				case 'C':
					return "char";
				case 'S':
					return "short";
				case 'I':
					return "int";
				case 'J':
					return "long";
				case 'F':
					return "float";
				case 'D':
					return "double";
				case 'V':
					return "void";
				case 'L':
					int start = pos;
					pos = buffer.indexOf(';', start) + 1;
					LOG.assertTrue(pos > 0);
					return buffer.substring(start, pos - 1).replace('/', '.');
				case '[':
					return getSignature() + "[]";
				case '(':
					StringBuilder result = new StringBuilder("(");
					String separator = "";
					while(peek() != ')')
					{
						result.append(separator);
						result.append(getSignature());
						separator = ", ";
					}
					get();
					result.append(")");
					return getSignature() + " " + getClassName() + "." + getMethodName() + " " + result;
				default:
					//          LOG.assertTrue(false, "unknown signature " + buffer);
					return null;
			}
		}

		String getMethodName()
		{
			return "";
		}

		String getClassName()
		{
			return "";
		}
	}

	public static String methodName(final Method m)
	{
		return methodName(signatureToName(m.declaringType().signature()), m.name(), m.signature());
	}

	public static String methodName(final String className, final String methodName, final String signature)
	{
		try
		{
			return new SigReader(signature)
			{
				@Override
				String getMethodName()
				{
					return methodName;
				}

				@Override
				String getClassName()
				{
					return className;
				}
			}.getSignature();
		}
		catch(Exception ignored)
		{
			if(LOG.isDebugEnabled())
			{
				LOG.debug("Internal error : unknown signature" + signature);
			}
			return className + "." + methodName;
		}
	}

	public static String signatureToName(String s)
	{
		return new SigReader(s).getSignature();
	}

	public static Value createValue(VirtualMachineProxyImpl vm, String expectedType, double value)
	{
		if(PsiType.DOUBLE.getPresentableText().equals(expectedType))
		{
			return vm.mirrorOf(value);
		}
		if(PsiType.FLOAT.getPresentableText().equals(expectedType))
		{
			return vm.mirrorOf((float) value);
		}
		return createValue(vm, expectedType, (long) value);
	}

	public static Value createValue(VirtualMachineProxyImpl vm, String expectedType, long value)
	{
		if(PsiType.LONG.getPresentableText().equals(expectedType))
		{
			return vm.mirrorOf(value);
		}
		if(PsiType.INT.getPresentableText().equals(expectedType))
		{
			return vm.mirrorOf((int) value);
		}
		if(PsiType.SHORT.getPresentableText().equals(expectedType))
		{
			return vm.mirrorOf((short) value);
		}
		if(PsiType.BYTE.getPresentableText().equals(expectedType))
		{
			return vm.mirrorOf((byte) value);
		}
		if(PsiType.CHAR.getPresentableText().equals(expectedType))
		{
			return vm.mirrorOf((char) value);
		}
		if(PsiType.DOUBLE.getPresentableText().equals(expectedType))
		{
			return vm.mirrorOf((double) value);
		}
		if(PsiType.FLOAT.getPresentableText().equals(expectedType))
		{
			return vm.mirrorOf((float) value);
		}
		return null;
	}

	public static Value createValue(VirtualMachineProxyImpl vm, String expectedType, boolean value)
	{
		if(PsiType.BOOLEAN.getPresentableText().equals(expectedType))
		{
			return vm.mirrorOf(value);
		}
		return null;
	}

	public static Value createValue(VirtualMachineProxyImpl vm, String expectedType, char value)
	{
		if(PsiType.CHAR.getPresentableText().equals(expectedType))
		{
			return vm.mirrorOf(value);
		}
		if(PsiType.LONG.getPresentableText().equals(expectedType))
		{
			return vm.mirrorOf((long) value);
		}
		if(PsiType.INT.getPresentableText().equals(expectedType))
		{
			return vm.mirrorOf((int) value);
		}
		if(PsiType.SHORT.getPresentableText().equals(expectedType))
		{
			return vm.mirrorOf((short) value);
		}
		if(PsiType.BYTE.getPresentableText().equals(expectedType))
		{
			return vm.mirrorOf((byte) value);
		}
		return null;
	}

	public static String truncateString(final String str)
	{
		if(str.length() > MAX_LABEL_SIZE)
		{
			return str.substring(0, MAX_LABEL_SIZE) + "...";
		}
		return str;
	}

	public static String getThreadStatusText(int statusId)
	{
		switch(statusId)
		{
			case ThreadReference.THREAD_STATUS_MONITOR:
				return DebuggerBundle.message("status.thread.monitor");
			case ThreadReference.THREAD_STATUS_NOT_STARTED:
				return DebuggerBundle.message("status.thread.not.started");
			case ThreadReference.THREAD_STATUS_RUNNING:
				return DebuggerBundle.message("status.thread.running");
			case ThreadReference.THREAD_STATUS_SLEEPING:
				return DebuggerBundle.message("status.thread.sleeping");
			case ThreadReference.THREAD_STATUS_UNKNOWN:
				return DebuggerBundle.message("status.thread.unknown");
			case ThreadReference.THREAD_STATUS_WAIT:
				return DebuggerBundle.message("status.thread.wait");
			case ThreadReference.THREAD_STATUS_ZOMBIE:
				return DebuggerBundle.message("status.thread.zombie");
			default:
				return DebuggerBundle.message("status.thread.undefined");
		}
	}

	public static String prepareValueText(String text, Project project)
	{
		text = StringUtil.unquoteString(text);
		text = StringUtil.unescapeStringCharacters(text);
		int tabSize = CodeStyleSettingsManager.getSettings(project).getTabSize(JavaFileType.INSTANCE);
		if(tabSize < 0)
		{
			tabSize = 0;
		}
		return text.replace("\t", StringUtil.repeat(" ", tabSize));
	}

	@Nullable
	public static XSourcePosition toXSourcePosition(@NotNull SourcePosition position)
	{
		return XSourcePositionImpl.create(position.getFile().getVirtualFile(), position.getLine());
	}
}
