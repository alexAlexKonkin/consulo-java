/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
 * @author max
 */
package org.jetbrains.java.generate.template;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import org.jetbrains.java.generate.element.FieldElement;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import consulo.java.module.util.JavaClassNames;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;

public abstract class TemplatesManager implements PersistentStateComponent<TemplatesState>
{
	public static final Key<Map<String, PsiType>> TEMPLATE_IMPLICITS = Key.create("TEMPLATE_IMPLICITS");

	private TemplatesState myState = new TemplatesState();

	public abstract TemplateResource[] getDefaultTemplates();

	/**
	 * Reads the content of the resource and return it as a String.
	 * <p/>Uses the class loader that loaded this class to find the resource in its classpath.
	 *
	 * @param resource the resource name. Will lookup using the classpath.
	 * @return the content if the resource
	 * @throws java.io.IOException error reading the file.
	 */
	protected static String readFile(String resource, Class<? extends TemplatesManager> templatesManagerClass) throws IOException
	{
		BufferedInputStream in = new BufferedInputStream(templatesManagerClass.getResourceAsStream(resource));
		return StringUtil.convertLineSeparators(FileUtil.loadTextAndClose(new InputStreamReader(in, CharsetToolkit.UTF8_CHARSET)));
	}

	@Override
	public TemplatesState getState()
	{
		return myState;
	}

	@Override
	public void loadState(TemplatesState state)
	{
		myState = state;
	}

	public void addTemplate(TemplateResource template)
	{
		myState.templates.add(template);
	}

	public void removeTemplate(TemplateResource template)
	{
		final Iterator<TemplateResource> it = myState.templates.iterator();
		while(it.hasNext())
		{
			TemplateResource resource = it.next();
			if(Comparing.equal(resource.getFileName(), template.getFileName()))
			{
				it.remove();
			}
		}
	}

	public Collection<TemplateResource> getAllTemplates()
	{
		HashSet<String> names = new HashSet<String>();
		Collection<TemplateResource> templates = new LinkedHashSet<TemplateResource>(Arrays.asList(getDefaultTemplates()));
		for(TemplateResource template : myState.templates)
		{
			if(names.add(template.getFileName()))
			{
				templates.add(template);
			}
		}
		return templates;
	}

	public TemplateResource getDefaultTemplate()
	{
		for(TemplateResource template : getAllTemplates())
		{
			if(Comparing.equal(template.getFileName(), myState.defaultTempalteName))
			{
				return template;
			}
		}

		return getAllTemplates().iterator().next();
	}


	public void setDefaultTemplate(TemplateResource res)
	{
		myState.defaultTempalteName = res.getFileName();
	}

	public void setTemplates(List<TemplateResource> items)
	{
		myState.templates.clear();
		for(TemplateResource item : items)
		{
			if(!item.isDefault())
			{
				myState.templates.add(item);
			}
		}
	}

	@Nonnull
	public static PsiType createFieldListElementType(Project project)
	{
		final PsiType classType = createElementType(project, FieldElement.class);
		final PsiClass listClass = JavaPsiFacade.getInstance(project).findClass(JavaClassNames.JAVA_UTIL_LIST, GlobalSearchScope.allScope(project));
		return listClass != null ? JavaPsiFacade.getElementFactory(project).createType(listClass, classType) : PsiType.NULL;
	}

	@Nonnull
	public static PsiType createElementType(Project project, Class<?> elementClass)
	{
		final List<String> methodNames = ContainerUtil.mapNotNull(elementClass.getMethods(), new Function<Method, String>()
		{
			@Override
			public String fun(Method method)
			{
				final String methodName = method.getName();
				if(methodName.startsWith("set"))
				{
					//hide setters from completion list
					return null;
				}
				return method.getGenericReturnType().toString() + " " + methodName + "();";
			}
		});
		final String text = "interface " + elementClass.getSimpleName() + " {\n" + StringUtil.join(methodNames, "\n") + "}";
		final PsiClass aClass = JavaPsiFacade.getElementFactory(project).createClassFromText(text, null).getInnerClasses()[0];
		return JavaPsiFacade.getElementFactory(project).createType(aClass);
	}
}