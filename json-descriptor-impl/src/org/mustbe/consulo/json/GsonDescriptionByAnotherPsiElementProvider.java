/*
 * Copyright 2013-2015 must-be.org
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

package org.mustbe.consulo.json;

import java.util.Collection;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.RequiredDispatchThread;
import org.mustbe.consulo.RequiredReadAction;
import org.mustbe.consulo.java.module.extension.JavaModuleExtension;
import org.mustbe.consulo.java.util.JavaClassNames;
import org.mustbe.consulo.json.validation.NativeArray;
import org.mustbe.consulo.json.validation.descriptionByAnotherPsiElement.DescriptionByAnotherPsiElementProvider;
import org.mustbe.consulo.json.validation.descriptor.JsonObjectDescriptor;
import org.mustbe.consulo.json.validation.descriptor.JsonPropertyDescriptor;
import org.mustbe.consulo.module.extension.ModuleExtensionHelper;
import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.ide.util.TreeClassChooser;
import com.intellij.ide.util.TreeClassChooserFactory;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiImmediateClassType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.TypeConversionUtil;
import com.intellij.util.containers.ContainerUtil;

/**
 * @author VISTALL
 * @since 12.11.2015
 */
public class GsonDescriptionByAnotherPsiElementProvider implements DescriptionByAnotherPsiElementProvider<PsiClass>
{
	public static class PropertyType
	{
		private final boolean myNullable;
		private final Object myValue;

		public PropertyType(Object value)
		{
			this(true, value);
		}

		public PropertyType(boolean nullable, Object value)
		{
			myNullable = nullable;
			myValue = value;
		}
	}

	@NotNull
	@Override
	public String getId()
	{
		return "GSON";
	}

	@NotNull
	@Override
	public String getPsiElementName()
	{
		return "Class";
	}

	@RequiredReadAction
	@NotNull
	@Override
	public String getIdFromPsiElement(@NotNull PsiClass psiClass)
	{
		return psiClass.getQualifiedName();
	}

	@RequiredReadAction
	@Nullable
	@Override
	public PsiClass getPsiElementById(@NotNull String s, @NotNull Project project)
	{
		return JavaPsiFacade.getInstance(project).findClass(s, GlobalSearchScope.allScope(project));
	}

	@RequiredDispatchThread
	@Nullable
	@Override
	public PsiClass chooseElement(@NotNull Project project)
	{
		TreeClassChooser classChooser = TreeClassChooserFactory.getInstance(project).createAllProjectScopeChooser("Choose class");
		classChooser.showDialog();
		return classChooser.getSelected();
	}

	@RequiredReadAction
	@Override
	public boolean isAvailable(@NotNull Project project)
	{
		return ModuleExtensionHelper.getInstance(project).hasModuleExtension(JavaModuleExtension.class) && getPsiElementById("com.google.gson.Gson", project) != null;
	}

	@Override
	public void fillRootObject(@NotNull PsiClass psiClass, @NotNull JsonObjectDescriptor jsonObjectDescriptor)
	{
		PropertyType type = toType(psiClass.getProject(), new PsiImmediateClassType(psiClass, PsiSubstitutor.EMPTY));

		if(type != null && type.myValue instanceof JsonObjectDescriptor)
		{
			for(Map.Entry<String, JsonPropertyDescriptor> entry : ((JsonObjectDescriptor) type.myValue).getProperties().entrySet())
			{
				jsonObjectDescriptor.getProperties().put(entry.getKey(), entry.getValue());
			}
		}
	}

	@Nullable
	private static PropertyType toType(@NotNull Project project, @NotNull PsiType type)
	{
		if(PsiType.BYTE.equals(type))
		{
			return new PropertyType(false, Number.class);
		}
		else if(PsiType.SHORT.equals(type))
		{
			return new PropertyType(false, Number.class);
		}
		else if(PsiType.INT.equals(type))
		{
			return new PropertyType(false, Number.class);
		}
		else if(PsiType.LONG.equals(type))
		{
			return new PropertyType(false, Number.class);
		}
		else if(PsiType.FLOAT.equals(type))
		{
			return new PropertyType(false, Number.class);
		}
		else if(PsiType.DOUBLE.equals(type))
		{
			return new PropertyType(false, Number.class);
		}
		else if(PsiType.BOOLEAN.equals(type))
		{
			return new PropertyType(false, Boolean.class);
		}
		else if(type instanceof PsiClassType)
		{
			PsiClassType.ClassResolveResult classResolveResult = ((PsiClassType) type).resolveGenerics();
			PsiClass psiClass = classResolveResult.getElement();
			if(psiClass != null)
			{
				String qualifiedName = psiClass.getQualifiedName();
				if(JavaClassNames.JAVA_LANG_STRING.equals(qualifiedName))
				{
					return new PropertyType(String.class);
				}
				else if(JavaClassNames.JAVA_LANG_BOOLEAN.equals(qualifiedName) || "java.util.concurrent.atomic.AtomicBoolean".equals(qualifiedName))
				{
					return new PropertyType(Boolean.class);
				}
				else if(JavaClassNames.JAVA_LANG_BYTE.equals(qualifiedName))
				{
					return new PropertyType(Number.class);
				}
				else if(JavaClassNames.JAVA_LANG_SHORT.equals(qualifiedName))
				{
					return new PropertyType(Number.class);
				}
				else if(JavaClassNames.JAVA_LANG_INTEGER.equals(qualifiedName) || "java.util.concurrent.atomic.AtomicInteger".equals(qualifiedName))
				{
					return new PropertyType(Number.class);
				}
				else if(JavaClassNames.JAVA_LANG_LONG.equals(qualifiedName) || "java.util.concurrent.atomic.AtomicLong".equals(qualifiedName))
				{
					return new PropertyType(Number.class);
				}
				else if(JavaClassNames.JAVA_LANG_FLOAT.equals(qualifiedName))
				{
					return new PropertyType(Number.class);
				}
				else if(JavaClassNames.JAVA_LANG_DOUBLE.equals(qualifiedName) || "java.util.concurrent.atomic.AtomicDouble".equals(qualifiedName))
				{
					return new PropertyType(Number.class);
				}
				else if("java.util.concurrent.atomic.AtomicIntegerArray".equals(qualifiedName))
				{
					return new PropertyType(new NativeArray(Number.class));
				}
				else if("java.util.concurrent.atomic.AtomicLongArray".equals(qualifiedName))
				{
					return new PropertyType(new NativeArray(Number.class));
				}
				else if("java.util.concurrent.atomic.AtomicDoubleArray".equals(qualifiedName))
				{
					return new PropertyType(new NativeArray(Number.class));
				}

				PsiClass collectionClass = JavaPsiFacade.getInstance(project).findClass(JavaClassNames.JAVA_UTIL_COLLECTION, GlobalSearchScope.allScope(project));
				if(collectionClass != null)
				{
					if(InheritanceUtil.isInheritorOrSelf(psiClass, collectionClass, true))
					{
						PsiSubstitutor superClassSubstitutor = TypeConversionUtil.getSuperClassSubstitutor(collectionClass, psiClass, classResolveResult.getSubstitutor());
						Collection<PsiType> values = superClassSubstitutor.getSubstitutionMap().values();
						if(!values.isEmpty())
						{
							PsiType firstItem = ContainerUtil.getFirstItem(values);
							assert firstItem != null;
							return toType(project, new PsiArrayType(firstItem));
						}

						return new PropertyType(new NativeArray(Object.class));
					}
				}

				PsiClass mapClass = JavaPsiFacade.getInstance(project).findClass(JavaClassNames.JAVA_UTIL_MAP, GlobalSearchScope.allScope(project));
				if(mapClass != null)
				{
					if(InheritanceUtil.isInheritorOrSelf(psiClass, mapClass, true))
					{
						PsiSubstitutor superClassSubstitutor = TypeConversionUtil.getSuperClassSubstitutor(mapClass, psiClass, classResolveResult.getSubstitutor());
						Collection<PsiType> values = superClassSubstitutor.getSubstitutionMap().values();
						if(values.size() == 2)
						{
							PsiTypeParameter psiTypeParameter = mapClass.getTypeParameters()[1];
							PsiType valueType = superClassSubstitutor.substitute(psiTypeParameter);
							assert valueType != null;

							JsonObjectDescriptor objectDescriptor = new JsonObjectDescriptor();
							PropertyType valueJsonType = toType(project, valueType);
							addIfNotNull(objectDescriptor, valueJsonType, null);
							return new PropertyType(objectDescriptor);
						}

						return new PropertyType(new NativeArray(Object.class));
					}
				}

				JsonObjectDescriptor objectDescriptor = new JsonObjectDescriptor();
				PsiField[] allFields = psiClass.getAllFields();
				for(PsiField psiField : allFields)
				{
					if(psiField.hasModifierProperty(PsiModifier.STATIC))
					{
						continue;
					}
					PropertyType classType = toType(project, psiField.getType());

					addIfNotNull(objectDescriptor, classType, psiField);
				}

				return new PropertyType(objectDescriptor);
			}
		}
		else if(type instanceof PsiArrayType)
		{
			PsiType componentType = ((PsiArrayType) type).getComponentType();

			PropertyType propertyType = toType(project, componentType);
			if(propertyType == null)
			{
				return null;
			}
			return new PropertyType(new NativeArray(propertyType.myValue));
		}
		return null;
	}

	private static void addIfNotNull(@NotNull JsonObjectDescriptor objectDescriptor, @Nullable PropertyType propertyType, @Nullable PsiField navElement)
	{
		if(propertyType == null)
		{
			return;
		}

		String propertyName = navElement == null ? null : getPropertyNameFromField(navElement);
		JsonPropertyDescriptor propertyDescriptor = null;

		Object classType = propertyType.myValue;
		if(classType instanceof Class)
		{
			propertyDescriptor = objectDescriptor.addProperty(propertyName, (Class<?>) classType);
		}
		else if(classType instanceof NativeArray)
		{
			propertyDescriptor = objectDescriptor.addProperty(propertyName, (NativeArray) classType);
		}
		else if(classType instanceof JsonObjectDescriptor)
		{
			propertyDescriptor = objectDescriptor.addProperty(propertyName, (JsonObjectDescriptor) classType);
		}

		if(propertyDescriptor != null && navElement != null)
		{
			propertyDescriptor.setNavigationElement(navElement);
			if(navElement.isDeprecated())
			{
				propertyDescriptor.deprecated();
			}
			if(!propertyType.myNullable)
			{
				propertyDescriptor.notNull();
			}
		}
	}

	@NotNull
	private static String getPropertyNameFromField(@NotNull PsiField field)
	{
		PsiAnnotation annotation = AnnotationUtil.findAnnotation(field, "com.google.gson.annotations.SerializedName");
		if(annotation != null)
		{
			String value = AnnotationUtil.getStringAttributeValue(annotation, PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME);
			if(value != null)
			{
				return value;
			}
		}
		return field.getName();
	}
}
