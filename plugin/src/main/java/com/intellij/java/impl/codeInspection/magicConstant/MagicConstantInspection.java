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
package com.intellij.java.impl.codeInspection.magicConstant;

import consulo.language.editor.scope.AnalysisScope;
import com.intellij.java.language.codeInsight.AnnotationUtil;
import com.intellij.java.language.codeInsight.ExternalAnnotationsManager;
import com.intellij.java.analysis.codeInspection.GroupNames;
import consulo.project.ui.view.tree.AbstractTreeNode;
import com.intellij.java.analysis.impl.codeInspection.BaseJavaLocalInspectionTool;
import com.intellij.java.language.psi.*;
import com.intellij.java.language.psi.util.PropertyUtil;
import com.intellij.java.language.psi.util.PsiFormatUtil;
import com.intellij.java.language.psi.util.PsiUtil;
import com.intellij.java.language.psi.util.TypeConversionUtil;
import consulo.application.ApplicationManager;
import consulo.project.Project;
import com.intellij.java.language.projectRoots.JavaSdk;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkModificator;
import com.intellij.java.impl.openapi.projectRoots.impl.JavaSdkImpl;
import consulo.module.content.layer.orderEntry.ModuleExtensionWithSdkOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.module.content.ProjectRootManager;
import consulo.util.lang.Comparing;
import consulo.ide.impl.idea.openapi.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import com.intellij.psi.*;
import com.intellij.java.language.psi.javadoc.PsiDocComment;
import com.intellij.java.language.psi.javadoc.PsiDocTag;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.scope.LocalSearchScope;
import consulo.language.ast.IElementType;
import com.intellij.psi.util.*;
import com.intellij.java.impl.slicer.DuplicateMap;
import com.intellij.java.impl.slicer.SliceAnalysisParams;
import com.intellij.java.impl.slicer.SliceRootNode;
import com.intellij.java.impl.slicer.SliceUsage;
import consulo.ide.impl.idea.util.Function;
import consulo.application.util.function.Processor;
import consulo.util.collection.ContainerUtil;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class MagicConstantInspection extends BaseJavaLocalInspectionTool
{
	public static final Key<Boolean> NO_ANNOTATIONS_FOUND = Key.create("REPORTED_NO_ANNOTATIONS_FOUND");

	@Nls
	@Nonnull
	@Override
	public String getGroupDisplayName()
	{
		return GroupNames.BUGS_GROUP_NAME;
	}

	@Nls
	@Nonnull
	@Override
	public String getDisplayName()
	{
		return "Magic Constant";
	}

	@Nonnull
	@Override
	public String getShortName()
	{
		return "MagicConstant";
	}

	@Nonnull
	@Override
	public PsiElementVisitor buildVisitor(@Nonnull final ProblemsHolder holder, boolean isOnTheFly, @Nonnull LocalInspectionToolSession session)
	{
		return new JavaElementVisitor()
		{
			@Override
			public void visitJavaFile(PsiJavaFile file)
			{
				checkAnnotationsJarAttached(file, holder);
			}

			@Override
			public void visitCallExpression(PsiCallExpression callExpression)
			{
				checkCall(callExpression, holder);
			}

			@Override
			public void visitAssignmentExpression(PsiAssignmentExpression expression)
			{
				PsiExpression r = expression.getRExpression();
				if(r == null)
				{
					return;
				}
				PsiExpression l = expression.getLExpression();
				if(!(l instanceof PsiReferenceExpression))
				{
					return;
				}
				PsiElement resolved = ((PsiReferenceExpression) l).resolve();
				if(!(resolved instanceof PsiModifierListOwner))
				{
					return;
				}
				PsiModifierListOwner owner = (PsiModifierListOwner) resolved;
				PsiType type = expression.getType();
				checkExpression(r, owner, type, holder);
			}

			@Override
			public void visitReturnStatement(PsiReturnStatement statement)
			{
				PsiExpression value = statement.getReturnValue();
				if(value == null)
				{
					return;
				}
				PsiElement element = PsiTreeUtil.getParentOfType(statement, PsiMethod.class, PsiLambdaExpression.class);
				PsiMethod method = element instanceof PsiMethod ? (PsiMethod) element : LambdaUtil.getFunctionalInterfaceMethod(element);
				if(method == null)
				{
					return;
				}
				checkExpression(value, method, value.getType(), holder);
			}

			@Override
			public void visitNameValuePair(PsiNameValuePair pair)
			{
				PsiAnnotationMemberValue value = pair.getValue();
				if(!(value instanceof PsiExpression))
				{
					return;
				}
				PsiReference ref = pair.getReference();
				if(ref == null)
				{
					return;
				}
				PsiMethod method = (PsiMethod) ref.resolve();
				if(method == null)
				{
					return;
				}
				checkExpression((PsiExpression) value, method, method.getReturnType(), holder);
			}

			@Override
			public void visitBinaryExpression(PsiBinaryExpression expression)
			{
				IElementType tokenType = expression.getOperationTokenType();
				if(tokenType != JavaTokenType.EQEQ && tokenType != JavaTokenType.NE)
				{
					return;
				}
				PsiExpression l = expression.getLOperand();
				PsiExpression r = expression.getROperand();
				if(r == null)
				{
					return;
				}
				checkBinary(l, r);
				checkBinary(r, l);
			}

			private void checkBinary(PsiExpression l, PsiExpression r)
			{
				if(l instanceof PsiReference)
				{
					PsiElement resolved = ((PsiReference) l).resolve();
					if(resolved instanceof PsiModifierListOwner)
					{
						checkExpression(r, (PsiModifierListOwner) resolved, getType((PsiModifierListOwner) resolved), holder);
					}
				}
				else if(l instanceof PsiMethodCallExpression)
				{
					PsiMethod method = ((PsiMethodCallExpression) l).resolveMethod();
					if(method != null)
					{
						checkExpression(r, method, method.getReturnType(), holder);
					}
				}
			}
		};
	}

	@Override
	public void cleanup(Project project)
	{
		super.cleanup(project);
		project.putUserData(NO_ANNOTATIONS_FOUND, null);
	}

	private static void checkAnnotationsJarAttached(@Nonnull PsiFile file, @Nonnull ProblemsHolder holder)
	{
		final Project project = file.getProject();
		if(!holder.isOnTheFly())
		{
			final Boolean found = project.getUserData(NO_ANNOTATIONS_FOUND);
			if(found != null)
			{
				return;
			}
		}

		PsiClass event = JavaPsiFacade.getInstance(project).findClass("java.awt.event.InputEvent", GlobalSearchScope.allScope(project));
		if(event == null)
		{
			return; // no jdk to attach
		}
		PsiMethod[] methods = event.findMethodsByName("getModifiers", false);
		if(methods.length != 1)
		{
			return; // no jdk to attach
		}
		PsiMethod getModifiers = methods[0];
		PsiAnnotation annotation = ExternalAnnotationsManager.getInstance(project).findExternalAnnotation(getModifiers, MagicConstant.class.getName());
		if(annotation != null)
		{
			return;
		}
		final VirtualFile virtualFile = PsiUtilCore.getVirtualFile(getModifiers);
		if(virtualFile == null)
		{
			return; // no jdk to attach
		}
		final List<OrderEntry> entries = ProjectRootManager.getInstance(project).getFileIndex().getOrderEntriesForFile(virtualFile);
		Sdk jdk = null;
		for(OrderEntry orderEntry : entries)
		{
			if(orderEntry instanceof ModuleExtensionWithSdkOrderEntry)
			{
				Sdk temp = ((ModuleExtensionWithSdkOrderEntry) orderEntry).getSdk();
				if(temp != null && temp.getSdkType() == JavaSdk.getInstance())
				{
					jdk = temp;
					break;
				}
			}
		}
		if(jdk == null)
		{
			return; // no jdk to attach
		}

		if(!holder.isOnTheFly())
		{
			project.putUserData(NO_ANNOTATIONS_FOUND, Boolean.TRUE);
		}

		final Sdk finalJdk = jdk;

		String path = finalJdk.getHomePath();
		String text = "No external annotations attached to the JDK " + finalJdk.getName() + (path == null ? "" : " (" + FileUtil.toSystemDependentName(path) + ")") + ", some issues will not be found";
		holder.registerProblem(file, text, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, new LocalQuickFix()
		{
			@Nonnull
			@Override
			public String getName()
			{
				return "Attach annotations";
			}

			@Nonnull
			@Override
			public String getFamilyName()
			{
				return getName();
			}

			@Override
			@RequiredUIAccess
			public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor)
			{
				ApplicationManager.getApplication().runWriteAction(new Runnable()
				{
					@Override
					public void run()
					{
						SdkModificator modificator = finalJdk.getSdkModificator();
						JavaSdkImpl.attachJdkAnnotations(modificator);
						modificator.commitChanges();
					}
				});
			}
		});
	}

	private static void checkExpression(PsiExpression expression, PsiModifierListOwner owner, PsiType type, ProblemsHolder holder)
	{
		AllowedValues allowed = getAllowedValues(owner, type, null);
		if(allowed == null)
		{
			return;
		}
		PsiElement scope = PsiUtil.getTopLevelEnclosingCodeBlock(expression, null);
		if(scope == null)
		{
			scope = expression;
		}
		if(!isAllowed(scope, expression, allowed, expression.getManager(), null))
		{
			registerProblem(expression, allowed, holder);
		}
	}

	private static void checkCall(@Nonnull PsiCallExpression methodCall, @Nonnull ProblemsHolder holder)
	{
		PsiMethod method = methodCall.resolveMethod();
		if(method == null)
		{
			return;
		}
		PsiParameter[] parameters = method.getParameterList().getParameters();
		PsiExpression[] arguments = methodCall.getArgumentList().getExpressions();
		for(int i = 0; i < parameters.length; i++)
		{
			PsiParameter parameter = parameters[i];
			AllowedValues values = getAllowedValues(parameter, parameter.getType(), null);
			if(values == null)
			{
				continue;
			}
			if(i >= arguments.length)
			{
				break;
			}
			PsiExpression argument = arguments[i];
			argument = PsiUtil.deparenthesizeExpression(argument);
			if(argument == null)
			{
				continue;
			}

			checkMagicParameterArgument(parameter, argument, values, holder);
		}
	}

	static class AllowedValues
	{
		final PsiAnnotationMemberValue[] values;
		final boolean canBeOred;

		private AllowedValues(@Nonnull PsiAnnotationMemberValue[] values, boolean canBeOred)
		{
			this.values = values;
			this.canBeOred = canBeOred;
		}

		@Override
		public boolean equals(Object o)
		{
			if(this == o)
			{
				return true;
			}
			if(o == null || getClass() != o.getClass())
			{
				return false;
			}

			AllowedValues a2 = (AllowedValues) o;
			if(canBeOred != a2.canBeOred)
			{
				return false;
			}
			Set<PsiAnnotationMemberValue> v1 = new HashSet<PsiAnnotationMemberValue>(Arrays.asList(values));
			Set<PsiAnnotationMemberValue> v2 = new HashSet<PsiAnnotationMemberValue>(Arrays.asList(a2.values));
			if(v1.size() != v2.size())
			{
				return false;
			}
			for(PsiAnnotationMemberValue value : v1)
			{
				for(PsiAnnotationMemberValue value2 : v2)
				{
					if(same(value, value2, value.getManager()))
					{
						v2.remove(value2);
						break;
					}
				}
			}
			return v2.isEmpty();
		}

		@Override
		public int hashCode()
		{
			int result = Arrays.hashCode(values);
			result = 31 * result + (canBeOred ? 1 : 0);
			return result;
		}

		public boolean isSubsetOf(@Nonnull AllowedValues other, @Nonnull PsiManager manager)
		{
			for(PsiAnnotationMemberValue value : values)
			{
				boolean found = false;
				for(PsiAnnotationMemberValue otherValue : other.values)
				{
					if(same(value, otherValue, manager))
					{
						found = true;
						break;
					}
				}
				if(!found)
				{
					return false;
				}
			}
			return true;
		}
	}

	private static AllowedValues getAllowedValuesFromMagic(@Nonnull PsiModifierListOwner element, @Nonnull PsiType type, @Nonnull PsiAnnotation magic, @Nonnull PsiManager manager)
	{
		PsiAnnotationMemberValue[] allowedValues;
		final boolean canBeOred;
		if(TypeConversionUtil.getTypeRank(type) <= TypeConversionUtil.LONG_RANK)
		{
			PsiAnnotationMemberValue intValues = magic.findAttributeValue("intValues");
			allowedValues = intValues instanceof PsiArrayInitializerMemberValue ? ((PsiArrayInitializerMemberValue) intValues).getInitializers() : PsiAnnotationMemberValue.EMPTY_ARRAY;
			if(allowedValues.length == 0)
			{
				PsiAnnotationMemberValue orValue = magic.findAttributeValue("flags");
				allowedValues = orValue instanceof PsiArrayInitializerMemberValue ? ((PsiArrayInitializerMemberValue) orValue).getInitializers() : PsiAnnotationMemberValue.EMPTY_ARRAY;
				canBeOred = true;
			}
			else
			{
				canBeOred = false;
			}
		}
		else if(type.equals(PsiType.getJavaLangString(manager, GlobalSearchScope.allScope(manager.getProject()))))
		{
			PsiAnnotationMemberValue strValuesAttr = magic.findAttributeValue("stringValues");
			allowedValues = strValuesAttr instanceof PsiArrayInitializerMemberValue ? ((PsiArrayInitializerMemberValue) strValuesAttr).getInitializers() : PsiAnnotationMemberValue.EMPTY_ARRAY;
			canBeOred = false;
		}
		else
		{
			return null; //other types not supported
		}

		if(allowedValues.length != 0)
		{
			return new AllowedValues(allowedValues, canBeOred);
		}

		// last resort: try valuesFromClass
		PsiAnnotationMemberValue[] values = readFromClass("valuesFromClass", magic, type, manager);
		boolean ored = false;
		if(values == null)
		{
			values = readFromClass("flagsFromClass", magic, type, manager);
			ored = true;
		}
		if(values == null)
		{
			return null;
		}
		return new AllowedValues(values, ored);
	}

	private static PsiAnnotationMemberValue[] readFromClass(@NonNls @Nonnull String attributeName, @Nonnull PsiAnnotation magic, @Nonnull PsiType type, @Nonnull PsiManager manager)
	{
		PsiAnnotationMemberValue fromClassAttr = magic.findAttributeValue(attributeName);
		PsiType fromClassType = fromClassAttr instanceof PsiClassObjectAccessExpression ? ((PsiClassObjectAccessExpression) fromClassAttr).getOperand().getType() : null;
		PsiClass fromClass = fromClassType instanceof PsiClassType ? ((PsiClassType) fromClassType).resolve() : null;
		if(fromClass == null)
		{
			return null;
		}
		String fqn = fromClass.getQualifiedName();
		if(fqn == null)
		{
			return null;
		}
		List<PsiAnnotationMemberValue> constants = new ArrayList<PsiAnnotationMemberValue>();
		for(PsiField field : fromClass.getFields())
		{
			if(!field.hasModifierProperty(PsiModifier.PUBLIC) || !field.hasModifierProperty(PsiModifier.STATIC) || !field.hasModifierProperty(PsiModifier.FINAL))
			{
				continue;
			}
			PsiType fieldType = field.getType();
			if(!Comparing.equal(fieldType, type))
			{
				continue;
			}
			PsiAssignmentExpression e = (PsiAssignmentExpression) JavaPsiFacade.getElementFactory(manager.getProject()).createExpressionFromText("x=" + fqn + "." + field.getName(), field);
			PsiReferenceExpression refToField = (PsiReferenceExpression) e.getRExpression();
			constants.add(refToField);
		}
		if(constants.isEmpty())
		{
			return null;
		}

		return constants.toArray(new PsiAnnotationMemberValue[constants.size()]);
	}

	static AllowedValues getAllowedValues(@Nonnull PsiModifierListOwner element, PsiType type, Set<PsiClass> visited)
	{
		PsiAnnotation[] annotations = getAllAnnotations(element);
		PsiManager manager = element.getManager();
		for(PsiAnnotation annotation : annotations)
		{
			AllowedValues values;
			if(type != null && MagicConstant.class.getName().equals(annotation.getQualifiedName()))
			{
				//PsiAnnotation magic = AnnotationUtil.findAnnotationInHierarchy(element, Collections.singleton(MagicConstant.class.getName()));
				values = getAllowedValuesFromMagic(element, type, annotation, manager);
				if(values != null)
				{
					return values;
				}
			}

			PsiJavaCodeReferenceElement ref = annotation.getNameReferenceElement();
			PsiElement resolved = ref == null ? null : ref.resolve();
			if(!(resolved instanceof PsiClass) || !((PsiClass) resolved).isAnnotationType())
			{
				continue;
			}
			PsiClass aClass = (PsiClass) resolved;
			if(visited == null)
			{
				visited = new HashSet<PsiClass>();
			}
			if(!visited.add(aClass))
			{
				continue;
			}
			values = getAllowedValues(aClass, type, visited);
			if(values != null)
			{
				return values;
			}
		}

		return parseBeanInfo(element, manager);
	}

	private static PsiAnnotation[] getAllAnnotations(final PsiModifierListOwner element)
	{
		return CachedValuesManager.getCachedValue(element, new CachedValueProvider<PsiAnnotation[]>()
		{
			@Nullable
			@Override
			public Result<PsiAnnotation[]> compute()
			{
				return Result.create(AnnotationUtil.getAllAnnotations(element, true, null), PsiModificationTracker.MODIFICATION_COUNT);
			}
		});
	}

	private static AllowedValues parseBeanInfo(@Nonnull PsiModifierListOwner owner, @Nonnull PsiManager manager)
	{
		PsiMethod method = null;
		if(owner instanceof PsiParameter)
		{
			PsiParameter parameter = (PsiParameter) owner;
			PsiElement scope = parameter.getDeclarationScope();
			if(!(scope instanceof PsiMethod))
			{
				return null;
			}
			PsiElement nav = scope.getNavigationElement();
			if(!(nav instanceof PsiMethod))
			{
				return null;
			}
			method = (PsiMethod) nav;
			if(method.isConstructor())
			{
				// not a property, try the @ConstructorProperties({"prop"})
				PsiAnnotation annotation = AnnotationUtil.findAnnotation(method, "java.beans.ConstructorProperties");
				if(annotation == null)
				{
					return null;
				}
				PsiAnnotationMemberValue value = annotation.findAttributeValue("value");
				if(!(value instanceof PsiArrayInitializerMemberValue))
				{
					return null;
				}
				PsiAnnotationMemberValue[] initializers = ((PsiArrayInitializerMemberValue) value).getInitializers();
				PsiElement parent = parameter.getParent();
				if(!(parent instanceof PsiParameterList))
				{
					return null;
				}
				int index = ((PsiParameterList) parent).getParameterIndex(parameter);
				if(index >= initializers.length)
				{
					return null;
				}
				PsiAnnotationMemberValue initializer = initializers[index];
				if(!(initializer instanceof PsiLiteralExpression))
				{
					return null;
				}
				Object val = ((PsiLiteralExpression) initializer).getValue();
				if(!(val instanceof String))
				{
					return null;
				}
				PsiMethod setter = PropertyUtil.findPropertySetter(method.getContainingClass(), (String) val, false, false);
				if(setter == null)
				{
					return null;
				}
				// try the @beaninfo of the corresponding setter
				PsiElement navigationElement = setter.getNavigationElement();
				if(!(navigationElement instanceof PsiMethod))
				{
					return null;
				}
				method = (PsiMethod) navigationElement;
			}
		}
		else if(owner instanceof PsiMethod)
		{
			PsiElement nav = owner.getNavigationElement();
			if(!(nav instanceof PsiMethod))
			{
				return null;
			}
			method = (PsiMethod) nav;
		}
		if(method == null)
		{
			return null;
		}

		PsiClass aClass = method.getContainingClass();
		if(aClass == null)
		{
			return null;
		}
		if(PropertyUtil.isSimplePropertyGetter(method))
		{
			List<PsiMethod> setters = PropertyUtil.getSetters(aClass, PropertyUtil.getPropertyNameByGetter(method));
			if(setters.size() != 1)
			{
				return null;
			}
			method = setters.get(0);
		}
		if(!PropertyUtil.isSimplePropertySetter(method))
		{
			return null;
		}
		PsiDocComment doc = method.getDocComment();
		if(doc == null)
		{
			return null;
		}
		PsiDocTag beaninfo = doc.findTagByName("beaninfo");
		if(beaninfo == null)
		{
			return null;
		}
		String data = StringUtil.join(beaninfo.getDataElements(), new Function<PsiElement, String>()
		{
			@Override
			public String fun(PsiElement element)
			{
				return element.getText();
			}
		}, "\n");
		int enumIndex = StringUtil.indexOfSubstringEnd(data, "enum:");
		if(enumIndex == -1)
		{
			return null;
		}
		data = data.substring(enumIndex);
		int colon = data.indexOf(":");
		int last = colon == -1 ? data.length() : data.substring(0, colon).lastIndexOf("\n");
		data = data.substring(0, last);

		List<PsiAnnotationMemberValue> values = new ArrayList<PsiAnnotationMemberValue>();
		for(String line : StringUtil.splitByLines(data))
		{
			List<String> words = StringUtil.split(line, " ", true, true);
			if(words.size() != 2)
			{
				continue;
			}
			String ref = words.get(1);
			PsiExpression constRef = JavaPsiFacade.getElementFactory(manager.getProject()).createExpressionFromText(ref, aClass);
			if(!(constRef instanceof PsiReferenceExpression))
			{
				continue;
			}
			PsiReferenceExpression expr = (PsiReferenceExpression) constRef;
			values.add(expr);
		}
		if(values.isEmpty())
		{
			return null;
		}
		PsiAnnotationMemberValue[] array = values.toArray(new PsiAnnotationMemberValue[values.size()]);
		return new AllowedValues(array, false);
	}

	private static PsiType getType(@Nonnull PsiModifierListOwner element)
	{
		return element instanceof PsiVariable ? ((PsiVariable) element).getType() : element instanceof PsiMethod ? ((PsiMethod) element).getReturnType() : null;
	}

	private static void checkMagicParameterArgument(@Nonnull PsiParameter parameter, @Nonnull PsiExpression argument, @Nonnull AllowedValues allowedValues, @Nonnull ProblemsHolder holder)
	{
		final PsiManager manager = PsiManager.getInstance(holder.getProject());

		if(!argument.getTextRange().isEmpty() && !isAllowed(parameter.getDeclarationScope(), argument, allowedValues, manager, null))
		{
			registerProblem(argument, allowedValues, holder);
		}
	}

	private static void registerProblem(@Nonnull PsiExpression argument, @Nonnull AllowedValues allowedValues, @Nonnull ProblemsHolder holder)
	{
		String values = StringUtil.join(allowedValues.values, new Function<PsiAnnotationMemberValue, String>()
		{
			@Override
			public String fun(PsiAnnotationMemberValue value)
			{
				if(value instanceof PsiReferenceExpression)
				{
					PsiElement resolved = ((PsiReferenceExpression) value).resolve();
					if(resolved instanceof PsiVariable)
					{
						return PsiFormatUtil.formatVariable((PsiVariable) resolved, PsiFormatUtilBase.SHOW_NAME | PsiFormatUtilBase.SHOW_CONTAINING_CLASS, PsiSubstitutor.EMPTY);
					}
				}
				return value.getText();
			}
		}, ", ");
		holder.registerProblem(argument, "Must be one of: " + values);
	}

	private static boolean isAllowed(@Nonnull final PsiElement scope,
			@Nonnull final PsiExpression argument,
			@Nonnull final AllowedValues allowedValues,
			@Nonnull final PsiManager manager,
			final Set<PsiExpression> visited)
	{
		if(isGoodExpression(argument, allowedValues, scope, manager, visited))
		{
			return true;
		}

		return processValuesFlownTo(argument, scope, manager, new Processor<PsiExpression>()
		{
			@Override
			public boolean process(PsiExpression expression)
			{
				return isGoodExpression(expression, allowedValues, scope, manager, visited);
			}
		});
	}

	private static boolean isGoodExpression(@Nonnull PsiExpression e,
			@Nonnull AllowedValues allowedValues,
			@Nonnull PsiElement scope,
			@Nonnull PsiManager manager,
			@Nullable Set<PsiExpression> visited)
	{
		PsiExpression expression = PsiUtil.deparenthesizeExpression(e);
		if(expression == null)
		{
			return true;
		}
		if(visited == null)
		{
			visited = new HashSet<PsiExpression>();
		}
		if(!visited.add(expression))
		{
			return true;
		}
		if(expression instanceof PsiConditionalExpression)
		{
			PsiExpression thenExpression = ((PsiConditionalExpression) expression).getThenExpression();
			boolean thenAllowed = thenExpression == null || isAllowed(scope, thenExpression, allowedValues, manager, visited);
			if(!thenAllowed)
			{
				return false;
			}
			PsiExpression elseExpression = ((PsiConditionalExpression) expression).getElseExpression();
			return elseExpression == null || isAllowed(scope, elseExpression, allowedValues, manager, visited);
		}

		if(isOneOf(expression, allowedValues, manager))
		{
			return true;
		}

		if(allowedValues.canBeOred)
		{
			PsiExpression zero = getLiteralExpression(expression, manager, "0");
			if(same(expression, zero, manager))
			{
				return true;
			}
			PsiExpression mOne = getLiteralExpression(expression, manager, "-1");
			if(same(expression, mOne, manager))
			{
				return true;
			}
			if(expression instanceof PsiPolyadicExpression)
			{
				IElementType tokenType = ((PsiPolyadicExpression) expression).getOperationTokenType();
				if(JavaTokenType.OR.equals(tokenType) || JavaTokenType.AND.equals(tokenType) || JavaTokenType.PLUS.equals(tokenType))
				{
					for(PsiExpression operand : ((PsiPolyadicExpression) expression).getOperands())
					{
						if(!isAllowed(scope, operand, allowedValues, manager, visited))
						{
							return false;
						}
					}
					return true;
				}
			}
			if(expression instanceof PsiPrefixExpression && JavaTokenType.TILDE.equals(((PsiPrefixExpression) expression).getOperationTokenType()))
			{
				PsiExpression operand = ((PsiPrefixExpression) expression).getOperand();
				return operand == null || isAllowed(scope, operand, allowedValues, manager, visited);
			}
		}

		PsiElement resolved = null;
		if(expression instanceof PsiReference)
		{
			resolved = ((PsiReference) expression).resolve();
		}
		else if(expression instanceof PsiCallExpression)
		{
			resolved = ((PsiCallExpression) expression).resolveMethod();
		}

		AllowedValues allowedForRef;
		if(resolved instanceof PsiModifierListOwner &&
				(allowedForRef = getAllowedValues((PsiModifierListOwner) resolved, getType((PsiModifierListOwner) resolved), null)) != null &&
				allowedForRef.isSubsetOf(allowedValues, manager))
		{
			return true;
		}

		return PsiType.NULL.equals(expression.getType());
	}

	private static final Key<Map<String, PsiExpression>> LITERAL_EXPRESSION_CACHE = Key.create("LITERAL_EXPRESSION_CACHE");

	private static PsiExpression getLiteralExpression(@Nonnull PsiExpression context, @Nonnull PsiManager manager, @Nonnull String text)
	{
		Map<String, PsiExpression> cache = LITERAL_EXPRESSION_CACHE.get(manager);
		if(cache == null)
		{
			cache = ContainerUtil.createConcurrentSoftValueMap();
			cache = manager.putUserDataIfAbsent(LITERAL_EXPRESSION_CACHE, cache);
		}
		PsiExpression expression = cache.get(text);
		if(expression == null)
		{
			expression = JavaPsiFacade.getElementFactory(manager.getProject()).createExpressionFromText(text, context);
			cache.put(text, expression);
		}
		return expression;
	}

	private static boolean isOneOf(@Nonnull PsiExpression expression, @Nonnull AllowedValues allowedValues, @Nonnull PsiManager manager)
	{
		for(PsiAnnotationMemberValue allowedValue : allowedValues.values)
		{
			if(same(allowedValue, expression, manager))
			{
				return true;
			}
		}
		return false;
	}

	private static boolean same(PsiElement e1, PsiElement e2, @Nonnull PsiManager manager)
	{
		if(e1 instanceof PsiLiteralExpression && e2 instanceof PsiLiteralExpression)
		{
			return Comparing.equal(((PsiLiteralExpression) e1).getValue(), ((PsiLiteralExpression) e2).getValue());
		}
		if(e1 instanceof PsiPrefixExpression && e2 instanceof PsiPrefixExpression && ((PsiPrefixExpression) e1).getOperationTokenType() == ((PsiPrefixExpression) e2).getOperationTokenType())
		{
			return same(((PsiPrefixExpression) e1).getOperand(), ((PsiPrefixExpression) e2).getOperand(), manager);
		}
		if(e1 instanceof PsiReference && e2 instanceof PsiReference)
		{
			e1 = ((PsiReference) e1).resolve();
			e2 = ((PsiReference) e2).resolve();
		}
		return manager.areElementsEquivalent(e2, e1);
	}

	private static boolean processValuesFlownTo(@Nonnull final PsiExpression argument, @Nonnull PsiElement scope, @Nonnull PsiManager manager, @Nonnull final Processor<PsiExpression> processor)
	{
		SliceAnalysisParams params = new SliceAnalysisParams();
		params.dataFlowToThis = true;
		params.scope = new AnalysisScope(new LocalSearchScope(scope), manager.getProject());

		SliceRootNode rootNode = new SliceRootNode(manager.getProject(), new DuplicateMap(), SliceUsage.createRootUsage(argument, params));

		Collection<? extends AbstractTreeNode> children = rootNode.getChildren().iterator().next().getChildren();
		for(AbstractTreeNode child : children)
		{
			SliceUsage usage = (SliceUsage) child.getValue();
			PsiElement element = usage.getElement();
			if(element instanceof PsiExpression && !processor.process((PsiExpression) element))
			{
				return false;
			}
		}

		return !children.isEmpty();
	}
}
