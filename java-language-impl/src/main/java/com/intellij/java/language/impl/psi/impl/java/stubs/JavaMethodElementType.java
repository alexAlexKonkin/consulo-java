/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.java.language.impl.psi.impl.java.stubs;

import com.intellij.java.language.impl.psi.impl.cache.ModifierFlags;
import com.intellij.java.language.impl.psi.impl.cache.RecordUtil;
import com.intellij.java.language.impl.psi.impl.cache.TypeInfo;
import com.intellij.java.language.impl.psi.impl.java.stubs.impl.PsiMethodStubImpl;
import com.intellij.java.language.impl.psi.impl.java.stubs.index.JavaStubIndexKeys;
import com.intellij.java.language.impl.psi.impl.source.PsiAnnotationMethodImpl;
import com.intellij.java.language.impl.psi.impl.source.PsiMethodImpl;
import com.intellij.java.language.impl.psi.impl.source.tree.ElementType;
import com.intellij.java.language.impl.psi.impl.source.tree.JavaDocElementType;
import com.intellij.java.language.impl.psi.impl.source.tree.JavaElementType;
import com.intellij.java.language.impl.psi.impl.source.tree.java.AnnotationMethodElement;
import com.intellij.java.language.psi.*;
import com.intellij.java.language.psi.util.TypeConversionUtil;
import consulo.language.ast.*;
import consulo.language.psi.stub.IndexSink;
import consulo.language.psi.stub.StubElement;
import consulo.language.psi.stub.StubInputStream;
import consulo.language.psi.stub.StubOutputStream;
import consulo.util.lang.BitUtil;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author max
 */
abstract class JavaMethodElementType extends JavaStubElementType<PsiMethodStub, PsiMethod> {
  JavaMethodElementType(@NonNls final String name) {
    super(name);
  }

  @Override
  public PsiMethod createPsi(@Nonnull final PsiMethodStub stub) {
    return getPsiFactory(stub).createMethod(stub);
  }

  @Override
  public PsiMethod createPsi(@Nonnull final ASTNode node) {
    if (node instanceof AnnotationMethodElement) {
      return new PsiAnnotationMethodImpl(node);
    }
    return new PsiMethodImpl(node);
  }

  @Nonnull
  @Override
  public PsiMethodStub createStub(@Nonnull final LighterAST tree, @Nonnull final LighterASTNode node, @Nonnull final StubElement parentStub) {
    String name = null;
    boolean isConstructor = true;
    boolean isVarArgs = false;
    boolean isDeprecatedByComment = false;
    boolean hasDeprecatedAnnotation = false;
    boolean hasDocComment = false;
    String defValueText = null;

    boolean expectingDef = false;
    for (final LighterASTNode child : tree.getChildren(node)) {
      final IElementType type = child.getTokenType();
      if (type == JavaDocElementType.DOC_COMMENT) {
        hasDocComment = true;
        isDeprecatedByComment = RecordUtil.isDeprecatedByDocComment(tree, child);
      } else if (type == JavaElementType.MODIFIER_LIST) {
        hasDeprecatedAnnotation = RecordUtil.isDeprecatedByAnnotation(tree, child);
      } else if (type == JavaElementType.TYPE) {
        isConstructor = false;
      } else if (type == JavaTokenType.IDENTIFIER) {
        name = RecordUtil.intern(tree.getCharTable(), child);
      } else if (type == JavaElementType.PARAMETER_LIST) {
        final List<LighterASTNode> params = LightTreeUtil.getChildrenOfType(tree, child, JavaElementType.PARAMETER);
        if (!params.isEmpty()) {
          final LighterASTNode pType = LightTreeUtil.firstChildOfType(tree, params.get(params.size() - 1), JavaElementType.TYPE);
          if (pType != null) {
            isVarArgs = LightTreeUtil.firstChildOfType(tree, pType, JavaTokenType.ELLIPSIS) != null;
          }
        }
      } else if (type == JavaTokenType.DEFAULT_KEYWORD) {
        expectingDef = true;
      } else if (expectingDef && !ElementType.JAVA_COMMENT_OR_WHITESPACE_BIT_SET.contains(type) &&
          type != JavaTokenType.SEMICOLON && type != JavaElementType.CODE_BLOCK) {
        defValueText = LightTreeUtil.toFilteredString(tree, child, null);
        break;
      }
    }

    TypeInfo typeInfo = isConstructor ? TypeInfo.createConstructorType() : TypeInfo.create(tree, node, parentStub);
    boolean isAnno = node.getTokenType() == JavaElementType.ANNOTATION_METHOD;
    byte flags = PsiMethodStubImpl.packFlags(isConstructor, isAnno, isVarArgs, isDeprecatedByComment, hasDeprecatedAnnotation, hasDocComment);

    return new PsiMethodStubImpl(parentStub, name, typeInfo, flags, defValueText);
  }

  @Override
  public void serialize(@Nonnull final PsiMethodStub stub, @Nonnull final StubOutputStream dataStream) throws IOException {
    dataStream.writeName(stub.getName());
    TypeInfo.writeTYPE(dataStream, stub.getReturnTypeText());
    dataStream.writeByte(((PsiMethodStubImpl) stub).getFlags());
    if (stub.isAnnotationMethod()) {
      dataStream.writeName(stub.getDefaultValueText());
    }
  }

  @Nonnull
  @Override
  public PsiMethodStub deserialize(@Nonnull final StubInputStream dataStream, final StubElement parentStub) throws IOException {
    String name = dataStream.readNameString();
    final TypeInfo type = TypeInfo.readTYPE(dataStream);
    byte flags = dataStream.readByte();
    String defaultMethodValue = PsiMethodStubImpl.isAnnotationMethod(flags) ? dataStream.readNameString() : null;
    return new PsiMethodStubImpl(parentStub, name, type, flags, defaultMethodValue);
  }

  @Override
  public void indexStub(@Nonnull final PsiMethodStub stub, @Nonnull final IndexSink sink) {
    final String name = stub.getName();
    if (name != null) {
      sink.occurrence(JavaStubIndexKeys.METHODS, name);
      if (RecordUtil.isStaticNonPrivateMember(stub)) {
        sink.occurrence(JavaStubIndexKeys.JVM_STATIC_MEMBERS_NAMES, name);
        sink.occurrence(JavaStubIndexKeys.JVM_STATIC_MEMBERS_TYPES, stub.getReturnTypeText().getShortTypeText());
      }
    }

    Set<String> methodTypeParams = getVisibleTypeParameters(stub);
    for (StubElement<?> stubElement : stub.getChildrenStubs()) {
      if (stubElement instanceof PsiParameterListStub) {
        for (StubElement<?> paramStub : stubElement.getChildrenStubs()) {
          if (paramStub instanceof PsiParameterStub) {
            TypeInfo type = ((PsiParameterStub) paramStub).getType();
            String typeName = PsiNameHelper.getShortClassName(type.text);
            if (TypeConversionUtil.isPrimitive(typeName) || TypeConversionUtil.isPrimitiveWrapper(typeName))
              continue;
            if (!methodTypeParams.contains(typeName)) {
              sink.occurrence(JavaStubIndexKeys.METHOD_TYPES, typeName);
            }
          }
        }
        break;
      }
    }
  }

  @Nonnull
  private static Set<String> getVisibleTypeParameters(@Nonnull StubElement<?> stub) {
    Set<String> result = null;
    while (stub != null) {
      Set<String> names = getOwnTypeParameterNames(stub);
      if (!names.isEmpty()) {
        if (result == null)
          result = new HashSet<>();
        result.addAll(names);
      }

      if (isStatic(stub))
        break;

      stub = stub.getParentStub();
    }
    return result == null ? Collections.emptySet() : result;
  }

  private static boolean isStatic(@Nonnull StubElement<?> stub) {
    if (stub instanceof PsiMemberStub) {
      PsiModifierListStub modList = stub.findChildStubByType(JavaStubElementTypes.MODIFIER_LIST);
      if (modList != null) {
        return BitUtil.isSet(modList.getModifiersMask(), ModifierFlags.NAME_TO_MODIFIER_FLAG_MAP.getInt(PsiModifier.STATIC));
      }
    }
    return false;
  }

  private static Set<String> getOwnTypeParameterNames(StubElement<?> stubElement) {
    StubElement<PsiTypeParameterList> typeParamList = stubElement.findChildStubByType(JavaStubElementTypes.TYPE_PARAMETER_LIST);
    if (typeParamList == null)
      return Collections.emptySet();

    Set<String> methodTypeParams = null;
    for (Object tStub : typeParamList.getChildrenStubs()) {
      if (tStub instanceof PsiTypeParameterStub) {
        if (methodTypeParams == null)
          methodTypeParams = new HashSet<>();
        methodTypeParams.add(((PsiTypeParameterStub) tStub).getName());
      }
    }
    return methodTypeParams == null ? Collections.emptySet() : methodTypeParams;
  }
}
