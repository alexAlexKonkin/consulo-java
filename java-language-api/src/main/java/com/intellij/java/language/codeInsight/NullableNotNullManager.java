// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.java.language.codeInsight;

import com.intellij.java.language.psi.*;
import com.intellij.java.language.psi.util.TypeConversionUtil;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.RecursionManager;
import consulo.ide.ServiceManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiModificationTracker;
import consulo.language.psi.util.LanguageCachedValueUtil;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static com.intellij.java.language.codeInsight.AnnotationUtil.*;

/**
 * @author anna
 */
@ServiceAPI(ComponentScope.PROJECT)
public abstract class NullableNotNullManager {
  protected static final Logger LOG = Logger.getInstance(NullableNotNullManager.class);
  protected final Project myProject;

  protected static final String JAVAX_ANNOTATION_NULLABLE = "javax.annotation.Nullable";
  protected static final String JAVAX_ANNOTATION_NONNULL = "javax.annotation.Nonnull";

  public static final String[] DEFAULT_NULLABLES = {
      NULLABLE,
      JAVAX_ANNOTATION_NULLABLE,
      "javax.annotation.CheckForNull",
      "edu.umd.cs.findbugs.annotations.Nullable",
      "android.support.annotation.Nullable",
      "androidx.annotation.Nullable",
      "androidx.annotation.RecentlyNullable",
      "org.checkerframework.checker.nullness.qual.Nullable",
      "org.checkerframework.checker.nullness.compatqual.NullableDecl",
      "org.checkerframework.checker.nullness.compatqual.NullableType",
      "com.android.annotations.Nullable",
  };
  public static final String[] DEFAULT_NOT_NULLS = {
      NOT_NULL,
      JAVAX_ANNOTATION_NONNULL,
      "edu.umd.cs.findbugs.annotations.NonNull",
      "android.support.annotation.NonNull",
      "androidx.annotation.NonNull",
      "androidx.annotation.RecentlyNonNull",
      "org.checkerframework.checker.nullness.qual.NonNull",
      "org.checkerframework.checker.nullness.compatqual.NonNullDecl",
      "org.checkerframework.checker.nullness.compatqual.NonNullType",
      "com.android.annotations.NonNull",
  };
  private static final List<String> DEFAULT_ALL = Arrays.asList(
      ArrayUtil.append(ArrayUtil.mergeArrays(DEFAULT_NULLABLES, DEFAULT_NOT_NULLS),
          "org.checkerframework.checker.nullness.qual.MonotonicNonNull"));

  protected NullableNotNullManager(Project project) {
    myProject = project;
  }

  public static NullableNotNullManager getInstance(Project project) {
    return ServiceManager.getService(project, NullableNotNullManager.class);
  }

  /**
   * @return if owner has a @NotNull or @Nullable annotation, or is in scope of @ParametersAreNullableByDefault or ParametersAreNonnullByDefault
   */
  public boolean hasNullability(@Nonnull PsiModifierListOwner owner) {
    return isNullable(owner, false) || isNotNull(owner, false);
  }

  public abstract void setNotNulls(@Nonnull String... annotations);

  public abstract void setNullables(@Nonnull String... annotations);

  @Nonnull
  public abstract String getDefaultNullable();

  /**
   * Returns an annotation which marks given element as Nullable, if any. Usage of this method is discouraged.
   * Use {@link #findEffectiveNullabilityInfo(PsiModifierListOwner)} instead.
   */
  @Nullable
  public PsiAnnotation getNullableAnnotation(@Nonnull PsiModifierListOwner owner, boolean checkBases) {
    return findNullityAnnotationWithDefault(owner, checkBases, true);
  }

  public abstract void setDefaultNullable(@Nonnull String defaultNullable);

  @Nonnull
  public abstract String getDefaultNotNull();

  /**
   * Returns an annotation which marks given element as NotNull, if any. Usage of this method is discouraged.
   * Use {@link #findEffectiveNullabilityInfo(PsiModifierListOwner)} instead.
   */
  @Nullable
  public PsiAnnotation getNotNullAnnotation(@Nonnull PsiModifierListOwner owner, boolean checkBases) {
    return findNullityAnnotationWithDefault(owner, checkBases, false);
  }

  @Nullable
  public PsiAnnotation copyNotNullAnnotation(@Nonnull PsiModifierListOwner original, @Nonnull PsiModifierListOwner generated) {
    NullabilityAnnotationInfo info = findOwnNullabilityInfo(original);
    if (info == null || info.getNullability() != Nullability.NOT_NULL) {
      return null;
    }
    return copyAnnotation(info.getAnnotation(), generated);
  }

  @Nullable
  public PsiAnnotation copyNullableAnnotation(@Nonnull PsiModifierListOwner original, @Nonnull PsiModifierListOwner generated) {
    NullabilityAnnotationInfo info = findOwnNullabilityInfo(original);
    if (info == null || info.getNullability() != Nullability.NULLABLE) {
      return null;
    }
    return copyAnnotation(info.getAnnotation(), generated);
  }

  @Nullable
  public PsiAnnotation copyNullableOrNotNullAnnotation(@Nonnull PsiModifierListOwner original, @Nonnull PsiModifierListOwner generated) {
    NullabilityAnnotationInfo src = findOwnNullabilityInfo(original);
    if (src == null) {
      return null;
    }
    NullabilityAnnotationInfo effective = findEffectiveNullabilityInfo(generated);
    if (effective != null && effective.getNullability() == src.getNullability()) {
      return null;
    }
    return copyAnnotation(src.getAnnotation(), generated);
  }

  @Nullable
  private static PsiAnnotation copyAnnotation(@Nonnull PsiAnnotation annotation, @Nonnull PsiModifierListOwner target) {
    String qualifiedName = annotation.getQualifiedName();
    if (qualifiedName != null) {
      if (JavaPsiFacade.getInstance(annotation.getProject()).findClass(qualifiedName, target.getResolveScope()) == null) {
        return null;
      }

      // type annotations are part of target's type and should not to be copied explicitly to avoid duplication
      if (!AnnotationTargetUtil.isTypeAnnotation(annotation)) {

        PsiModifierList modifierList = target.getModifierList();
        if (modifierList != null && !modifierList.hasAnnotation(qualifiedName)) {
          return modifierList.addAnnotation(qualifiedName);
        }
      }
    }

    return null;
  }

  /**
   * @deprecated use {@link #copyNotNullAnnotation(PsiModifierListOwner, PsiModifierListOwner)}
   */
  @Deprecated
  //@ApiStatus.ScheduledForRemoval(inVersion = "2021.1")
  public PsiAnnotation copyNotNullAnnotation(@Nonnull PsiModifierListOwner owner) {
    NullabilityAnnotationInfo info = findOwnNullabilityInfo(owner);
    if (info == null || info.getNullability() != Nullability.NOT_NULL) {
      return null;
    }
    String qualifiedName = info.getAnnotation().getQualifiedName();
    return qualifiedName != null
        ? JavaPsiFacade.getElementFactory(owner.getProject()).createAnnotationFromText("@" + qualifiedName, owner)
        : null;
  }

  public abstract void setDefaultNotNull(@Nonnull String defaultNotNull);

  @Nullable
  private PsiAnnotation findNullityAnnotationWithDefault(@Nonnull PsiModifierListOwner owner, boolean checkBases, boolean nullable) {
    PsiAnnotation annotation = findPlainNullityAnnotation(owner, checkBases);
    if (annotation != null) {
      String qName = annotation.getQualifiedName();
      if (qName == null) {
        return null;
      }

      List<String> contradictory = nullable ? getNotNullsWithNickNames() : getNullablesWithNickNames();
      if (contradictory.contains(qName)) {
        return null;
      }

      return annotation;
    }

    PsiType type = getOwnerType(owner);
    if (type == null || TypeConversionUtil.isPrimitiveAndNotNull(type)) {
      return null;
    }

    // even if javax.annotation.Nullable is not configured, it should still take precedence over ByDefault annotations
    List<String> annotations = Arrays.asList(nullable ? DEFAULT_NOT_NULLS : DEFAULT_NULLABLES);
    int flags = (checkBases ? CHECK_HIERARCHY : 0) | CHECK_EXTERNAL | CHECK_INFERRED | CHECK_TYPE;
    if (isAnnotated(owner, annotations, flags)) {
      return null;
    }

    if (!nullable && hasHardcodedContracts(owner)) {
      return null;
    }

    if (owner instanceof PsiParameter && !nullable && checkBases) {
      List<PsiParameter> superParameters = getSuperAnnotationOwners((PsiParameter) owner);
      if (!superParameters.isEmpty()) {
        return takeAnnotationFromSuperParameters((PsiParameter) owner, superParameters);
      }
    }

    NullabilityAnnotationInfo nullityDefault = findNullityDefaultInHierarchy(owner);
    Nullability wantedNullability = nullable ? Nullability.NULLABLE : Nullability.NOT_NULL;
    return nullityDefault != null && nullityDefault.getNullability() == wantedNullability ? nullityDefault.getAnnotation() : null;
  }

  /**
   * Returns own nullability annotation info for given element. Returned annotation is not inherited and
   * not container annotation for class/package. Still it could be inferred or external.
   *
   * @param owner element to find a nullability info for
   * @return own nullability annotation info.
   */
  @Nullable
  public NullabilityAnnotationInfo findOwnNullabilityInfo(@Nonnull PsiModifierListOwner owner) {
    PsiType type = getOwnerType(owner);
    if (type == null || TypeConversionUtil.isPrimitiveAndNotNull(type)) {
      return null;
    }

    List<String> nullables = getNullablesWithNickNames();
    PsiAnnotation annotation = findPlainNullityAnnotation(owner, false);
    if (annotation != null) {
      return new NullabilityAnnotationInfo(annotation,
          nullables.contains(annotation.getQualifiedName()) ? Nullability.NULLABLE : Nullability.NOT_NULL,
          false);
    }
    return null;
  }

  /**
   * Returns information about explicit nullability annotation (without looking into external/inferred annotations,
   * but looking into container annotations). This method is rarely useful in client code, it's designed mostly
   * to aid the inference procedure.
   *
   * @param owner element to get the info about
   * @return the annotation info or null if no explicit annotation found
   */
  @Nullable
  public NullabilityAnnotationInfo findExplicitNullability(PsiModifierListOwner owner) {
    PsiAnnotation annotation = findAnnotation(owner, getAllNullabilityAnnotationsWithNickNames(), true);
    if (annotation != null) {
      Nullability nullability =
          getNullablesWithNickNames().contains(annotation.getQualifiedName()) ? Nullability.NULLABLE : Nullability.NOT_NULL;
      return new NullabilityAnnotationInfo(annotation, nullability, false);
    }
    return findNullityDefaultInHierarchy(owner);
  }

  /**
   * Returns nullability annotation info which has effect for given element.
   *
   * @param owner element to find an annotation for
   * @return effective nullability annotation info, or null if not found.
   */
  @Nullable
  public NullabilityAnnotationInfo findEffectiveNullabilityInfo(@Nonnull PsiModifierListOwner owner) {
    PsiType type = getOwnerType(owner);
    if (type == null || TypeConversionUtil.isPrimitiveAndNotNull(type)) {
      return null;
    }

    return LanguageCachedValueUtil.getCachedValue(owner, () -> CachedValueProvider.Result
        .create(doFindEffectiveNullabilityAnnotation(owner), PsiModificationTracker.MODIFICATION_COUNT));
  }

  @Nullable
  private NullabilityAnnotationInfo doFindEffectiveNullabilityAnnotation(@Nonnull PsiModifierListOwner owner) {
    Set<String> annotationNames = getAllNullabilityAnnotationsWithNickNames();
    Set<String> extraAnnotations = new HashSet<>(DEFAULT_ALL);
    extraAnnotations.addAll(annotationNames);

    PsiAnnotation annotation = findPlainAnnotation(owner, true, extraAnnotations);
    if (annotation != null) {
      if (!annotationNames.contains(annotation.getQualifiedName())) {
        // Deliberately excluded known standard annotation still has precedence over default class-level or package-level annotation:
        // return null in this case
        return null;
      }
      List<String> nullables = getNullablesWithNickNames();
      return new NullabilityAnnotationInfo(annotation,
          nullables.contains(annotation.getQualifiedName()) ? Nullability.NULLABLE : Nullability.NOT_NULL,
          false);
    }

    if (owner instanceof PsiParameter) {
      List<PsiParameter> superParameters = getSuperAnnotationOwners((PsiParameter) owner);
      if (!superParameters.isEmpty()) {
        for (PsiParameter parameter : superParameters) {
          PsiAnnotation plain = findPlainAnnotation(parameter, false, extraAnnotations);
          // Plain not null annotation is not inherited
          if (plain != null) {
            return null;
          }
          NullabilityAnnotationInfo defaultInfo = findNullityDefaultInHierarchy(parameter);
          if (defaultInfo != null) {
            return defaultInfo.getNullability() == Nullability.NOT_NULL ? defaultInfo : null;
          }
        }
        return null;
      }
    }

    NullabilityAnnotationInfo defaultInfo = findNullityDefaultInHierarchy(owner);
    if (defaultInfo != null && (defaultInfo.getNullability() == Nullability.NULLABLE || !hasHardcodedContracts(owner))) {
      return defaultInfo;
    }
    return null;
  }

  private PsiAnnotation takeAnnotationFromSuperParameters(@Nonnull PsiParameter owner, @Nonnull List<? extends PsiParameter> superOwners) {
    return RecursionManager.doPreventingRecursion(owner, true, () -> {
      for (PsiParameter superOwner : superOwners) {
        PsiAnnotation anno = findNullityAnnotationWithDefault(superOwner, false, false);
        if (anno != null) {
          return anno;
        }
      }
      return null;
    });
  }

  private PsiAnnotation findPlainNullityAnnotation(@Nonnull PsiModifierListOwner owner, boolean checkBases) {
    Set<String> qNames = getAllNullabilityAnnotationsWithNickNames();
    return findPlainAnnotation(owner, checkBases, qNames);
  }

  /**
   * @return an annotation (if any) with the given nullability semantics on the given declaration or its type. In case of conflicts,
   * type annotations are preferred.
   */
  @Nullable
  public PsiAnnotation findExplicitNullabilityAnnotation(@Nonnull PsiModifierListOwner owner, @Nonnull Nullability nullability) {
    if (nullability == Nullability.UNKNOWN) {
      return null;
    }
    List<String> names = nullability == Nullability.NULLABLE ? getNullablesWithNickNames() : getNotNullsWithNickNames();
    return findPlainAnnotation(owner, false, new HashSet<>(names));
  }

  @Nullable
  private static PsiAnnotation findPlainAnnotation(@Nonnull PsiModifierListOwner owner,
                                                   boolean checkBases,
                                                   @Nonnull Set<String> qualifiedNames) {
    PsiAnnotation memberAnno = checkBases && owner instanceof PsiMethod
        ? findAnnotationInHierarchy(owner, qualifiedNames)
        : findAnnotation(owner, qualifiedNames);
    PsiType type = getOwnerType(owner);
    if (memberAnno != null) {
      PsiAnnotation annotation = preferTypeAnnotation(memberAnno, type);
      if (annotation != memberAnno && !qualifiedNames.contains(annotation.getQualifiedName())) {
        return null;
      }
      return annotation;
    }
    if (type != null) {
      return ContainerUtil.find(type.getAnnotations(), a -> qualifiedNames.contains(a.getQualifiedName()));
    }
    return null;
  }

  @Nonnull
  private static PsiAnnotation preferTypeAnnotation(@Nonnull PsiAnnotation memberAnno, @Nullable PsiType type) {
    if (type != null) {
      for (PsiAnnotation typeAnno : type.getApplicableAnnotations()) {
        if (areDifferentNullityAnnotations(memberAnno, typeAnno)) {
          return typeAnno;
        }
      }
    }
    return memberAnno;
  }

  private static boolean areDifferentNullityAnnotations(@Nonnull PsiAnnotation memberAnno, @Nonnull PsiAnnotation typeAnno) {
    NullableNotNullManager manager = getInstance(memberAnno.getProject());
    List<String> notNulls = manager.getNotNullsWithNickNames();
    List<String> nullables = manager.getNullablesWithNickNames();
    return nullables.contains(typeAnno.getQualifiedName()) && notNulls.contains(memberAnno.getQualifiedName()) ||
        nullables.contains(memberAnno.getQualifiedName()) && notNulls.contains(typeAnno.getQualifiedName());
  }

  @Nonnull
  protected List<String> getNullablesWithNickNames() {
    return getNullables();
  }

  @Nonnull
  protected List<String> getNotNullsWithNickNames() {
    return getNotNulls();
  }

  @Nonnull
  protected Set<String> getAllNullabilityAnnotationsWithNickNames() {
    Set<String> qNames = new HashSet<>(getNullablesWithNickNames());
    qNames.addAll(getNotNullsWithNickNames());
    return Collections.unmodifiableSet(qNames);
  }

  protected boolean hasHardcodedContracts(@Nonnull PsiElement element) {
    return false;
  }

  @Nullable
  private static PsiType getOwnerType(@Nonnull PsiModifierListOwner owner) {
    if (owner instanceof PsiVariable) {
      return ((PsiVariable) owner).getType();
    }
    if (owner instanceof PsiMethod) {
      return ((PsiMethod) owner).getReturnType();
    }
    return null;
  }

  public boolean isNullable(@Nonnull PsiModifierListOwner owner, boolean checkBases) {
    return findNullityAnnotationWithDefault(owner, checkBases, true) != null;
  }

  public boolean isNotNull(@Nonnull PsiModifierListOwner owner, boolean checkBases) {
    return findNullityAnnotationWithDefault(owner, checkBases, false) != null;
  }

  /**
   * @param context place in PSI tree
   * @return default nullability for type-use elements at given place
   */
  @Nullable
  public NullabilityAnnotationInfo findDefaultTypeUseNullability(@Nullable PsiElement context) {
    if (context == null) {
      return null;
    }
    if (context.getParent() instanceof PsiTypeElement && context.getParent().getParent() instanceof PsiLocalVariable) {
      return null;
    }
    return findNullabilityDefault(context, PsiAnnotation.TargetType.TYPE_USE);
  }

  private
  @Nullable
  NullabilityAnnotationInfo findNullabilityDefault(@Nonnull PsiElement place,
                                                   @Nonnull PsiAnnotation.TargetType... placeTargetTypes) {
    PsiElement element = place.getParent();
    while (element != null) {
      if (element instanceof PsiModifierListOwner) {
        NullabilityAnnotationInfo result = getNullityDefault((PsiModifierListOwner) element, placeTargetTypes, place, false);
        if (result != null) {
          return result;
        }
      }

      if (element instanceof PsiClassOwner) {
        String packageName = ((PsiClassOwner) element).getPackageName();
        return findNullityDefaultOnPackage(placeTargetTypes, JavaPsiFacade.getInstance(element.getProject()).findPackage(packageName),
            place);
      }

      element = element.getContext();
    }
    return null;
  }

  @Nullable
  NullabilityAnnotationInfo findNullityDefaultInHierarchy(@Nonnull PsiModifierListOwner owner) {
    PsiAnnotation.TargetType[] placeTargetTypes = AnnotationTargetUtil.getTargetsForLocation(owner.getModifierList());

    PsiElement element = owner.getParent();
    while (element != null) {
      if (element instanceof PsiModifierListOwner) {
        NullabilityAnnotationInfo result = getNullityDefault((PsiModifierListOwner) element, placeTargetTypes, owner, false);
        if (result != null) {
          return result;
        }
      }

      if (element instanceof PsiClassOwner) {
        String packageName = ((PsiClassOwner) element).getPackageName();
        return findNullityDefaultOnPackage(placeTargetTypes, JavaPsiFacade.getInstance(element.getProject()).findPackage(packageName),
            owner);
      }

      element = element.getContext();
    }
    return null;
  }

  @Nullable
  private NullabilityAnnotationInfo findNullityDefaultOnPackage(@Nonnull PsiAnnotation.TargetType[] placeTargetTypes,
                                                                @Nullable PsiJavaPackage psiPackage,
                                                                PsiElement context) {
    boolean superPackage = false;
    while (psiPackage != null) {
      NullabilityAnnotationInfo onPkg = getNullityDefault(psiPackage, placeTargetTypes, context, superPackage);
      if (onPkg != null) {
        return onPkg;
      }
      superPackage = true;
      psiPackage = psiPackage.getParentPackage();
    }
    return null;
  }

  @Nullable
  protected abstract NullabilityAnnotationInfo getNullityDefault(@Nonnull PsiModifierListOwner container,
                                                       @Nonnull PsiAnnotation.TargetType[] placeTargetTypes,
                                                       PsiElement context, boolean superPackage);

  @Nonnull
  public abstract List<String> getNullables();

  @Nonnull
  public abstract List<String> getNotNulls();

  /**
   * Returns true if given element is known to be nullable
   *
   * @param owner element to check
   * @return true if given element is known to be nullable
   */
  public static boolean isNullable(@Nonnull PsiModifierListOwner owner) {
    return getNullability(owner) == Nullability.NULLABLE;
  }

  /**
   * Returns true if given element is known to be non-nullable
   *
   * @param owner element to check
   * @return true if given element is known to be non-nullable
   */
  public static boolean isNotNull(@Nonnull PsiModifierListOwner owner) {
    return getNullability(owner) == Nullability.NOT_NULL;
  }

  /**
   * Returns nullability of given element defined by annotations.
   *
   * @param owner element to find nullability for
   * @return found nullability; {@link Nullability#UNKNOWN} if not specified or non-applicable
   */
  @Nonnull
  public static Nullability getNullability(@Nonnull PsiModifierListOwner owner) {
    NullabilityAnnotationInfo info = getInstance(owner.getProject()).findEffectiveNullabilityInfo(owner);
    return info == null ? Nullability.UNKNOWN : info.getNullability();
  }

  @Nonnull
  public abstract List<String> getInstrumentedNotNulls();

  public abstract void setInstrumentedNotNulls(@Nonnull List<String> names);

  /**
   * Checks if given annotation specifies the nullability (either nullable or not-null)
   *
   * @param annotation annotation to check
   * @return true if given annotation specifies nullability
   */
  public static boolean isNullabilityAnnotation(@Nonnull PsiAnnotation annotation) {
    return getInstance(annotation.getProject()).getAllNullabilityAnnotationsWithNickNames().contains(annotation.getQualifiedName());
  }
}