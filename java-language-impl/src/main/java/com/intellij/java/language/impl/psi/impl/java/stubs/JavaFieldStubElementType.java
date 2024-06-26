// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.java.language.impl.psi.impl.java.stubs;

import com.intellij.java.language.impl.psi.impl.cache.RecordUtil;
import com.intellij.java.language.impl.psi.impl.cache.TypeInfo;
import com.intellij.java.language.impl.psi.impl.java.stubs.impl.PsiFieldStubImpl;
import com.intellij.java.language.impl.psi.impl.java.stubs.index.JavaStubIndexKeys;
import com.intellij.java.language.impl.psi.impl.source.PsiEnumConstantImpl;
import com.intellij.java.language.impl.psi.impl.source.PsiFieldImpl;
import com.intellij.java.language.impl.psi.impl.source.tree.ElementType;
import com.intellij.java.language.impl.psi.impl.source.tree.JavaDocElementType;
import com.intellij.java.language.impl.psi.impl.source.tree.JavaElementType;
import com.intellij.java.language.impl.psi.impl.source.tree.java.EnumConstantElement;
import com.intellij.java.language.psi.JavaTokenType;
import com.intellij.java.language.psi.PsiField;
import consulo.language.ast.LightTreeUtil;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.ast.LighterAST;
import consulo.language.ast.LighterASTNode;
import consulo.language.psi.stub.IndexSink;
import consulo.language.psi.stub.StubElement;
import consulo.language.psi.stub.StubInputStream;
import consulo.language.psi.stub.StubOutputStream;

import jakarta.annotation.Nonnull;
import java.io.IOException;

public abstract class JavaFieldStubElementType extends JavaStubElementType<PsiFieldStub, PsiField> {
  private static final int INITIALIZER_LENGTH_LIMIT = 1000;

  public JavaFieldStubElementType(@Nonnull String id) {
    super(id);
  }

  @Override
  public PsiField createPsi(@Nonnull final PsiFieldStub stub) {
    return getPsiFactory(stub).createField(stub);
  }

  @Override
  public PsiField createPsi(@Nonnull final ASTNode node) {
    if (node instanceof EnumConstantElement) {
      return new PsiEnumConstantImpl(node);
    } else {
      return new PsiFieldImpl(node);
    }
  }

  @Nonnull
  @Override
  public PsiFieldStub createStub(@Nonnull final LighterAST tree, @Nonnull final LighterASTNode node, @Nonnull final StubElement parentStub) {
    final TypeInfo typeInfo = TypeInfo.create(tree, node, parentStub);

    boolean isDeprecatedByComment = false;
    boolean hasDeprecatedAnnotation = false;
    boolean hasDocComment = false;
    String name = null;
    String initializer = null;

    boolean expectingInit = false;
    for (final LighterASTNode child : tree.getChildren(node)) {
      final IElementType type = child.getTokenType();
      if (type == JavaDocElementType.DOC_COMMENT) {
        hasDocComment = true;
        isDeprecatedByComment = RecordUtil.isDeprecatedByDocComment(tree, child);
      } else if (type == JavaElementType.MODIFIER_LIST) {
        hasDeprecatedAnnotation = RecordUtil.isDeprecatedByAnnotation(tree, child);
      } else if (type == JavaTokenType.IDENTIFIER) {
        name = RecordUtil.intern(tree.getCharTable(), child);
      } else if (type == JavaTokenType.EQ) {
        expectingInit = true;
      } else if (expectingInit && !ElementType.JAVA_COMMENT_OR_WHITESPACE_BIT_SET.contains(type) && type != JavaTokenType.SEMICOLON) {
        initializer = encodeInitializer(tree, child);
        break;
      }
    }

    final boolean isEnumConst = node.getTokenType() == JavaElementType.ENUM_CONSTANT;
    final byte flags = PsiFieldStubImpl.packFlags(isEnumConst, isDeprecatedByComment, hasDeprecatedAnnotation, hasDocComment);

    return new PsiFieldStubImpl(parentStub, name, typeInfo, initializer, flags);
  }

  private static String encodeInitializer(final LighterAST tree, final LighterASTNode initializer) {
    final IElementType type = initializer.getTokenType();
    if (type == JavaElementType.NEW_EXPRESSION || type == JavaElementType.METHOD_CALL_EXPRESSION) {
      return PsiFieldStub.INITIALIZER_NOT_STORED;
    }

    if (initializer.getEndOffset() - initializer.getStartOffset() > INITIALIZER_LENGTH_LIMIT) {
      return PsiFieldStub.INITIALIZER_TOO_LONG;
    }

    return LightTreeUtil.toFilteredString(tree, initializer, null);
  }

  @Override
  public void serialize(@Nonnull PsiFieldStub stub, @Nonnull StubOutputStream dataStream) throws IOException {
    dataStream.writeName(stub.getName());
    TypeInfo.writeTYPE(dataStream, stub.getType());
    dataStream.writeName(stub.getInitializerText());
    dataStream.writeByte(((PsiFieldStubImpl) stub).getFlags());
  }

  @Nonnull
  @Override
  public PsiFieldStub deserialize(@Nonnull StubInputStream dataStream, StubElement parentStub) throws IOException {
    String name = dataStream.readNameString();
    TypeInfo type = TypeInfo.readTYPE(dataStream);
    String initializerText = dataStream.readNameString();
    byte flags = dataStream.readByte();
    return new PsiFieldStubImpl(parentStub, name, type, initializerText, flags);
  }

  @Override
  public void indexStub(@Nonnull PsiFieldStub stub, @Nonnull IndexSink sink) {
    String name = stub.getName();
    if (name != null) {
      sink.occurrence(JavaStubIndexKeys.FIELDS, name);
      if (RecordUtil.isStaticNonPrivateMember(stub)) {
        sink.occurrence(JavaStubIndexKeys.JVM_STATIC_MEMBERS_NAMES, name);
        sink.occurrence(JavaStubIndexKeys.JVM_STATIC_MEMBERS_TYPES, stub.getType().getShortTypeText());
      }
    }
  }
}