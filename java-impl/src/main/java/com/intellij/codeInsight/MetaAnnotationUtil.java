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
package com.intellij.codeInsight;

import gnu.trove.THashSet;
import gnu.trove.TObjectHashingStrategy;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.java.module.util.JavaClassNames;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.psi.search.searches.DirectClassInheritorsSearch;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.containers.ConcurrentFactoryMap;
import com.intellij.util.containers.ContainerUtil;

/**
 * @since 2016.3
 */
public class MetaAnnotationUtil
{
	private static final TObjectHashingStrategy<PsiClass> HASHING_STRATEGY = new TObjectHashingStrategy<PsiClass>()
	{
		public int computeHashCode(final PsiClass object)
		{
			final String qualifiedName = object.getQualifiedName();
			return qualifiedName == null ? 0 : qualifiedName.hashCode();
		}

		public boolean equals(final PsiClass o1, final PsiClass o2)
		{
			return Comparing.equal(o1.getQualifiedName(), o2.getQualifiedName());
		}
	};

	public static Collection<PsiClass> getAnnotationTypesWithChildren(@Nonnull final Module module, final String annotationName, final boolean includeTests)
	{
		final Project project = module.getProject();

		Map<Pair<String, Boolean>, Collection<PsiClass>> map = CachedValuesManager.getManager(project).getCachedValue(module, () ->
		{
			Map<Pair<String, Boolean>, Collection<PsiClass>> factoryMap = ConcurrentFactoryMap.createMap(key ->
			{
				GlobalSearchScope moduleScope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, key.getSecond());

				PsiClass annotationClass = JavaPsiFacade.getInstance(project).findClass(key.getFirst(), moduleScope);
				if(annotationClass == null || !annotationClass.isAnnotationType())
				{
					return Collections.emptyList();
				}

				// limit search to files containing annotations
				GlobalSearchScope effectiveSearchScope = getAllAnnotationFilesScope(project).intersectWith(moduleScope);
				return getAnnotationTypesWithChildren(annotationClass, effectiveSearchScope);
			});
			return CachedValueProvider.Result.create(factoryMap, PsiModificationTracker.JAVA_STRUCTURE_MODIFICATION_COUNT);
		});

		return map.get(Pair.create(annotationName, includeTests));
	}

	public static Set<PsiClass> getChildren(final PsiClass psiClass, final GlobalSearchScope scope)
	{
		if(AnnotationTargetUtil.findAnnotationTarget(psiClass, PsiAnnotation.TargetType.ANNOTATION_TYPE, PsiAnnotation.TargetType.TYPE) == null)
		{
			return Collections.emptySet();
		}

		final String name = psiClass.getQualifiedName();
		if(name == null)
		{
			return Collections.emptySet();
		}

		final Set<PsiClass> result = new THashSet<>(HASHING_STRATEGY);

		AnnotatedElementsSearch.searchPsiClasses(psiClass, scope).forEach(processorResult ->
		{
			if(processorResult.isAnnotationType())
			{
				result.add(processorResult);
			}
			return true;
		});

		return result;
	}

	public static Collection<PsiClass> getAnnotatedTypes(final Module module, final Key<CachedValue<Collection<PsiClass>>> key, final String annotationName)
	{
		return CachedValuesManager.getManager(module.getProject()).getCachedValue(module, key, () ->
		{
			final GlobalSearchScope scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false);
			final PsiClass psiClass = JavaPsiFacade.getInstance(module.getProject()).findClass(annotationName, scope);

			final Collection<PsiClass> classes;
			if(psiClass == null || !psiClass.isAnnotationType())
			{
				classes = Collections.emptyList();
			}
			else
			{
				classes = getChildren(psiClass, scope);
			}
			return new CachedValueProvider.Result<>(classes, PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT);
		}, false);
	}

	@Nonnull
	private static Collection<PsiClass> getAnnotationTypesWithChildren(PsiClass annotationClass, GlobalSearchScope scope)
	{
		final Set<PsiClass> classes = new THashSet<>(HASHING_STRATEGY);

		collectClassWithChildren(annotationClass, classes, scope);

		return classes;
	}

	private static GlobalSearchScope getAllAnnotationFilesScope(Project project)
	{
		return CachedValuesManager.getManager(project).getCachedValue(project, () ->
		{
			GlobalSearchScope scope = GlobalSearchScope.allScope(project);
			Set<VirtualFile> allAnnotationFiles = new HashSet<>();
			for(PsiClass javaLangAnnotation : JavaPsiFacade.getInstance(project).findClasses(JavaClassNames.JAVA_LANG_ANNOTATION_ANNOTATION, scope))
			{
				DirectClassInheritorsSearch.search(javaLangAnnotation, scope, false).forEach(annotationClass ->
				{
					ContainerUtil.addIfNotNull(allAnnotationFiles, PsiUtilCore.getVirtualFile(annotationClass));
					return true;
				});
			}
			return CachedValueProvider.Result.createSingleDependency(GlobalSearchScope.filesWithLibrariesScope(project, allAnnotationFiles), PsiModificationTracker.JAVA_STRUCTURE_MODIFICATION_COUNT);
		});
	}

	private static void collectClassWithChildren(final PsiClass psiClass, final Set<PsiClass> classes, final GlobalSearchScope scope)
	{
		classes.add(psiClass);

		for(PsiClass aClass : getChildren(psiClass, scope))
		{
			if(!classes.contains(aClass))
			{
				collectClassWithChildren(aClass, classes, scope);
			}
		}
	}

	/**
	 * Check if listOwner is annotated with annotations or listOwner's annotations contain given annotations
	 */
	public static boolean isMetaAnnotated(@Nonnull PsiModifierListOwner listOwner, @Nonnull final Collection<String> annotations)
	{
		if(AnnotationUtil.isAnnotated(listOwner, annotations, false))
		{
			return true;
		}

		final List<PsiClass> resolvedAnnotations = getResolvedClassesInAnnotationsList(listOwner);
		for(String annotationFQN : annotations)
		{
			for(PsiClass resolvedAnnotation : resolvedAnnotations)
			{
				if(metaAnnotationCached(resolvedAnnotation, annotationFQN) != null)
				{
					return true;
				}
			}
		}

		return false;
	}

	@javax.annotation.Nullable
	private static PsiAnnotation metaAnnotationCached(PsiClass subjectAnnotation, String annotationToFind)
	{
		ConcurrentMap<String, PsiAnnotation> cachedValue = CachedValuesManager.getCachedValue(subjectAnnotation, () ->
		{
			ConcurrentMap<String, PsiAnnotation> metaAnnotationsMap = ConcurrentFactoryMap.createMap(anno -> findMetaAnnotation(subjectAnnotation, anno, new HashSet<>()));
			return new CachedValueProvider.Result<>(metaAnnotationsMap, PsiModificationTracker.JAVA_STRUCTURE_MODIFICATION_COUNT);
		});
		return cachedValue.get(annotationToFind);
	}

	@javax.annotation.Nullable
	private static PsiAnnotation findMetaAnnotation(PsiClass aClass, final String annotation, final Set<PsiClass> visited)
	{
		PsiAnnotation directAnnotation = AnnotationUtil.findAnnotation(aClass, annotation);
		if(directAnnotation != null)
		{
			return directAnnotation;
		}
		List<PsiClass> resolvedAnnotations = getResolvedClassesInAnnotationsList(aClass);
		for(PsiClass resolvedAnnotation : resolvedAnnotations)
		{
			if(visited.add(resolvedAnnotation))
			{
				PsiAnnotation annotated = findMetaAnnotation(resolvedAnnotation, annotation, visited);
				if(annotated != null)
				{
					return annotated;
				}
			}
		}

		return null;
	}


	@Nonnull
	public static Stream<PsiAnnotation> findMetaAnnotations(@Nonnull PsiModifierListOwner listOwner, @Nonnull final Collection<String> annotations)
	{
		Stream<PsiAnnotation> directAnnotations = Stream.of(AnnotationUtil.findAnnotations(listOwner, annotations));

		Stream<PsiClass> lazyResolvedAnnotations = Stream.generate(() -> getResolvedClassesInAnnotationsList(listOwner)).limit(1).flatMap(it -> it.stream());

		Stream<PsiAnnotation> metaAnnotations = lazyResolvedAnnotations.flatMap(psiClass -> annotations.stream().map(annotationFQN -> metaAnnotationCached(psiClass, annotationFQN))).filter
				(Objects::nonNull);

		return Stream.concat(directAnnotations, metaAnnotations);
	}


	private static List<PsiClass> getResolvedClassesInAnnotationsList(PsiModifierListOwner owner)
	{
		PsiModifierList modifierList = owner.getModifierList();
		if(modifierList != null)
		{
			return ContainerUtil.mapNotNull(modifierList.getApplicableAnnotations(), psiAnnotation ->
			{
				PsiJavaCodeReferenceElement nameReferenceElement = psiAnnotation.getNameReferenceElement();
				PsiElement resolve = nameReferenceElement != null ? nameReferenceElement.resolve() : null;
				return resolve instanceof PsiClass && ((PsiClass) resolve).isAnnotationType() ? (PsiClass) resolve : null;
			});
		}
		return Collections.emptyList();
	}
}
