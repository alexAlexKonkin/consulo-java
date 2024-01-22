// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.java.language.impl.psi.impl.cache;

import com.intellij.java.language.impl.psi.impl.java.stubs.PsiClassStub;
import com.intellij.java.language.impl.psi.impl.source.tree.JavaElementType;
import com.intellij.java.language.psi.CommonClassNames;
import com.intellij.java.language.psi.JavaTokenType;
import com.intellij.java.language.psi.PsiNameHelper;
import consulo.language.ast.LightTreeUtil;
import consulo.language.ast.IElementType;
import consulo.language.ast.LighterAST;
import consulo.language.ast.LighterASTNode;
import consulo.language.psi.stub.StubElement;
import consulo.language.psi.stub.StubInputStream;
import consulo.language.psi.stub.StubOutputStream;
import consulo.util.collection.primitive.objects.ObjectIntMap;
import consulo.util.collection.primitive.objects.ObjectMaps;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static consulo.util.lang.BitUtil.isSet;

public class TypeInfo {
  private static final int FREQUENT_INDEX_MASK = 0x01F;
  private static final int HAS_TYPE_ANNOTATIONS = 0x20;
  private static final int HAS_ARRAY_COUNT = 0x40;
  private static final int HAS_ELLIPSIS = 0x80;

  public static final TypeInfo[] EMPTY_ARRAY = {};

  private static final String[] ourIndexFrequentType;
  private static final ObjectIntMap<String> ourFrequentTypeIndex;

  static {
    ourIndexFrequentType = new String[]{
        "",
        "boolean",
        "byte",
        "char",
        "double",
        "float",
        "int",
        "long",
        "null",
        "short",
        "void",
        CommonClassNames.JAVA_LANG_OBJECT_SHORT,
        CommonClassNames.JAVA_LANG_OBJECT,
        CommonClassNames.JAVA_LANG_STRING_SHORT,
        CommonClassNames.JAVA_LANG_STRING
    };

    ourFrequentTypeIndex = ObjectMaps.newObjectIntHashMap();
    for (int i = 0; i < ourIndexFrequentType.length; i++) {
      ourFrequentTypeIndex.putInt(ourIndexFrequentType[i], i);
    }
    assert ourFrequentTypeIndex.size() == ourIndexFrequentType.length;
    assert ourFrequentTypeIndex.size() < FREQUENT_INDEX_MASK;
  }

  private static final TypeInfo NULL = new TypeInfo(null);

  public final String text;
  public final byte arrayCount;
  public final boolean isEllipsis;
  private TypeAnnotationContainer myTypeAnnotations;

  /**
   * Creates a non-array type info
   *
   * @param text type text (not array)
   */
  public TypeInfo(String text) {
    this(text, (byte) 0, false);
  }

  /**
   * @param text       type text (not array)
   * @param arrayCount number of array components (including vararg component, if any)
   * @param ellipsis   if true, the last array component should be interpreted as vararg
   */
  public TypeInfo(String text, byte arrayCount, boolean ellipsis) {
    this.text = text == null ? null : internFrequentType(text);
    this.arrayCount = arrayCount;
    isEllipsis = ellipsis;
  }

  /**
   * @param typeAnnotations set type annotations. Could be called only once.
   */
  public void setTypeAnnotations(@jakarta.annotation.Nonnull TypeAnnotationContainer typeAnnotations) {
    if (myTypeAnnotations != null) {
      throw new IllegalStateException();
    }
    myTypeAnnotations = typeAnnotations;
  }

  /**
   * @return type annotations associated with this type.
   */
  public
  @Nonnull
  TypeAnnotationContainer getTypeAnnotations() {
    return myTypeAnnotations == null ? TypeAnnotationContainer.EMPTY : myTypeAnnotations;
  }

  @Nonnull
  public String getShortTypeText() {
    if (text == null) {
      return "";
    }
    String name = PsiNameHelper.getShortClassName(text);
    if (arrayCount > 0) {
      name += StringUtil.repeat("[]", arrayCount);
    }
    return name;
  }

  @Override
  public String toString() {
    String text = createTypeText(this);
    return text != null ? text : "null";
  }

  /* factories and serialization */

  @Nonnull
  public static TypeInfo createConstructorType() {
    return NULL;
  }

  @Nonnull
  public static TypeInfo create(@Nonnull LighterAST tree, @Nonnull LighterASTNode element, StubElement parentStub) {
    String text;
    byte arrayCount = 0;
    boolean isEllipsis = false;

    boolean hasAnnotation = false;
    LighterASTNode typeElement = null;

    if (element.getTokenType() == JavaElementType.ENUM_CONSTANT) {
      text = ((PsiClassStub<?>) parentStub).getName();
    } else {

      for (final LighterASTNode child : tree.getChildren(element)) {
        IElementType type = child.getTokenType();
        if (type == JavaElementType.TYPE) {
          typeElement = child;
        } else if (type == JavaTokenType.LBRACKET) {
          arrayCount++;  // C-style array
        }
      }

      if (typeElement == null && element.getTokenType() == JavaElementType.FIELD) {
        LighterASTNode parent = tree.getParent(element);
        assert parent != null : element;
        List<LighterASTNode> fields = LightTreeUtil.getChildrenOfType(tree, parent, JavaElementType.FIELD);
        int idx = fields.indexOf(element);
        for (int i = idx - 1; i >= 0 && typeElement == null; i--) {  // int i, j
          typeElement = LightTreeUtil.firstChildOfType(tree, fields.get(i), JavaElementType.TYPE);
        }
      }

      assert typeElement != null : element + " in " + parentStub;

      LighterASTNode nested = LightTreeUtil.firstChildOfType(tree, typeElement, JavaElementType.TYPE);

      if (nested != null) {
        // Java-style array
        for (LighterASTNode child : tree.getChildren(typeElement)) {
          IElementType tokenType = child.getTokenType();
          if (tokenType == JavaTokenType.LBRACKET) {
            arrayCount++;
          } else if (tokenType == JavaTokenType.ELLIPSIS) {
            arrayCount++;
            isEllipsis = true;
          } else if (tokenType == JavaElementType.ANNOTATION) {
            hasAnnotation = true;
          }
        }
        text = LightTreeUtil.toFilteredString(tree, nested, null);
      } else {
        text = LightTreeUtil.toFilteredString(tree, typeElement, null);
      }
    }

    TypeInfo info = new TypeInfo(text, arrayCount, isEllipsis);
    if (hasAnnotation) {
      // TODO: support bounds, generics and enclosing types
      TypeAnnotationContainer.Collector collector = new TypeAnnotationContainer.Collector(info);
      int nestingLevel = arrayCount;
      for (LighterASTNode child : tree.getChildren(typeElement)) {
        IElementType tokenType = child.getTokenType();
        if (tokenType == JavaElementType.TYPE) {
          nestingLevel = 0;
        } else if (tokenType == JavaTokenType.LBRACKET) {
          nestingLevel++;
        } else if (tokenType == JavaElementType.ANNOTATION) {
          String anno = LightTreeUtil.toFilteredString(tree, child, null);
          byte[] typePath = new byte[nestingLevel];
          Arrays.fill(typePath, TypeAnnotationContainer.Collector.ARRAY_ELEMENT);
          collector.add(typePath, anno);
        }
      }

      collector.install();
    }
    return info;
  }

  @Nonnull
  public static TypeInfo fromString(@jakarta.annotation.Nonnull String typeText, boolean isEllipsis) {
    assert !typeText.endsWith("...") : typeText;

    byte arrayCount = 0;
    while (typeText.endsWith("[]")) {
      arrayCount++;
      typeText = typeText.substring(0, typeText.length() - 2);
    }

    return new TypeInfo(typeText, arrayCount, isEllipsis);
  }

  @jakarta.annotation.Nonnull
  public static TypeInfo fromString(@Nonnull String typeText) {
    boolean isEllipsis = false;
    if (typeText.endsWith("...")) {
      isEllipsis = true;
      typeText = typeText.substring(0, typeText.length() - 3);
    }

    return fromString(typeText, isEllipsis);
  }

  @Nonnull
  public static TypeInfo readTYPE(@Nonnull StubInputStream record) throws IOException {
    int flags = record.readByte() & 0xFF;
    if (flags == FREQUENT_INDEX_MASK) {
      return NULL;
    }

    byte arrayCount = isSet(flags, HAS_ARRAY_COUNT) ? record.readByte() : 0;
    boolean hasEllipsis = isSet(flags, HAS_ELLIPSIS);
    boolean hasTypeAnnotations = isSet(flags, HAS_TYPE_ANNOTATIONS);

    int frequentIndex = FREQUENT_INDEX_MASK & flags;
    String text = frequentIndex == 0 ? record.readNameString() : ourIndexFrequentType[frequentIndex];

    TypeInfo info = new TypeInfo(text, arrayCount, hasEllipsis);
    info.setTypeAnnotations(hasTypeAnnotations ? TypeAnnotationContainer.readTypeAnnotations(record) : TypeAnnotationContainer.EMPTY);
    return info;
  }

  public static void writeTYPE(@Nonnull StubOutputStream dataStream, @Nonnull TypeInfo typeInfo) throws IOException {
    if (typeInfo == NULL) {
      dataStream.writeByte(FREQUENT_INDEX_MASK);
      return;
    }

    String text = typeInfo.text;
    byte arrayCount = typeInfo.arrayCount;
    int frequentIndex = ourFrequentTypeIndex.getInt(text);
    boolean hasTypeAnnotations = typeInfo.myTypeAnnotations != null && !typeInfo.myTypeAnnotations.isEmpty();
    int flags = (typeInfo.isEllipsis ? HAS_ELLIPSIS : 0) | (arrayCount != 0 ? HAS_ARRAY_COUNT : 0) |
        (hasTypeAnnotations ? HAS_TYPE_ANNOTATIONS : 0) | frequentIndex;
    dataStream.writeByte(flags);

    if (arrayCount != 0) {
      dataStream.writeByte(arrayCount);
    }

    if (frequentIndex == 0) {
      dataStream.writeName(text);
    }
    if (hasTypeAnnotations) {
      TypeAnnotationContainer.writeTypeAnnotations(dataStream, typeInfo.myTypeAnnotations);
    }
  }

  /**
   * @param typeInfo
   * @return type text without annotations
   */
  @Nullable
  public static String createTypeText(@Nonnull TypeInfo typeInfo) {
    if (typeInfo == NULL || typeInfo.text == null) {
      return null;
    }
    if (typeInfo.arrayCount == 0) {
      return typeInfo.text;
    }

    StringBuilder buf = new StringBuilder();

    buf.append(typeInfo.text);

    int arrayCount = typeInfo.isEllipsis ? typeInfo.arrayCount - 1 : typeInfo.arrayCount;
    for (int i = 0; i < arrayCount; i++) {
      buf.append("[]");
    }
    if (typeInfo.isEllipsis) {
      buf.append("...");
    }

    return internFrequentType(buf.toString());
  }

  @Nonnull
  public static String internFrequentType(@Nonnull String type) {
    int frequentIndex = ourFrequentTypeIndex.getInt(type);
    return frequentIndex == 0 ? StringUtil.internEmptyString(type) : ourIndexFrequentType[frequentIndex];
  }
}