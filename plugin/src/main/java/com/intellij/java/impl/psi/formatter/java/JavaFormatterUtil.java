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
package com.intellij.java.impl.psi.formatter.java;

import com.intellij.java.impl.psi.codeStyle.JavaCodeStyleSettings;
import com.intellij.java.impl.psi.formatter.java.wrap.ReservedWrapsProvider;
import com.intellij.java.language.impl.psi.impl.source.tree.ChildRole;
import com.intellij.java.language.impl.psi.impl.source.tree.ElementType;
import com.intellij.java.language.impl.psi.impl.source.tree.JavaElementType;
import com.intellij.java.language.psi.*;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenType;
import consulo.language.codeStyle.*;
import consulo.language.impl.ast.CompositeElement;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiWhiteSpace;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;

/**
 * @author Denis Zhdanov
 * @since 4/12/11 3:26 PM
 */
public class JavaFormatterUtil {
  private static final int CALL_EXPRESSION_DEPTH = 2500;

  /**
   * Holds type of AST elements that are considered to be assignments.
   */
  private static final Set<IElementType> ASSIGNMENT_ELEMENT_TYPES = new HashSet<>(asList(
    JavaElementType.ASSIGNMENT_EXPRESSION, JavaElementType.LOCAL_VARIABLE, JavaElementType.FIELD
  ));

  private JavaFormatterUtil() {
  }

  /**
   * Allows to answer if given node wraps assignment operation.
   *
   * @param node node to check
   * @return {@code true} if given node wraps assignment operation; {@code false} otherwise
   */
  public static boolean isAssignment(ASTNode node) {
    return ASSIGNMENT_ELEMENT_TYPES.contains(node.getElementType());
  }

  /**
   * Allows to check if given {@code AST} nodes refer to binary expressions which have the same priority.
   *
   * @param node1 node to check
   * @param node2 node to check
   * @return {@code true} if given nodes are binary expressions and have the same priority;
   * {@code false} otherwise
   */
  public static boolean areSamePriorityBinaryExpressions(ASTNode node1, ASTNode node2) {
    if (node1 == null || node2 == null) {
      return false;
    }

    if (!(node1 instanceof PsiPolyadicExpression) || !(node2 instanceof PsiPolyadicExpression)) {
      return false;
    }
    PsiPolyadicExpression expression1 = (PsiPolyadicExpression)node1;
    PsiPolyadicExpression expression2 = (PsiPolyadicExpression)node2;
    return expression1.getOperationTokenType() == expression2.getOperationTokenType();
  }

  @Nonnull
  public static WrapType getWrapType(int wrap) {
    switch (wrap) {
      case CommonCodeStyleSettings.WRAP_ALWAYS:
        return WrapType.ALWAYS;
      case CommonCodeStyleSettings.WRAP_AS_NEEDED:
        return WrapType.NORMAL;
      case CommonCodeStyleSettings.DO_NOT_WRAP:
        return WrapType.NONE;
      default:
        return WrapType.CHOP_DOWN_IF_LONG;
    }
  }

  /**
   * Check if the node is a start of call chunk.The call chunk is either {@code .call()} or {@code call().}. The dot is added to the end
   * if there's a line break after it and "keep line breaks" is on.
   *
   * @param settings The current settings
   * @param node     The node to check.
   * @return True for call chunk start.
   */
  static boolean isStartOfCallChunk(@Nonnull CommonCodeStyleSettings settings, @Nonnull ASTNode node) {
    if (node.getElementType() == JavaTokenType.DOT) {
      if (settings.KEEP_LINE_BREAKS) {
        ASTNode next = node.getTreeNext();
        if (next != null && next.getElementType() == JavaElementType.REFERENCE_PARAMETER_LIST && next.getTextLength() == 0) {
          next = next.getTreeNext();
        }
        return !(next != null && next.getPsi() instanceof PsiWhiteSpace && next.textContains('\n'));
      }
      return true;
    }
    else if (node.getElementType() == JavaTokenType.IDENTIFIER) {
      if (settings.KEEP_LINE_BREAKS) {
        ASTNode prev = node.getTreePrev();
        if (prev instanceof PsiWhiteSpace && prev.textContains('\n')) {
          prev = prev.getTreePrev();
          if (prev != null && prev.getElementType() == JavaElementType.REFERENCE_PARAMETER_LIST && prev.getTextLength() == 0) {
            prev = prev.getTreePrev();
          }
          return prev != null && prev.getElementType() == JavaTokenType.DOT;
        }
      }
      return false;
    }
    return false;
  }

  /**
   * Traverses the children of the node and collects nodes with type method calls or reference expressions to the list
   *
   * @param nodes List in which the method add nodes
   * @param node  Node to traverse
   */
  public static void collectCallExpressionNodes(@Nonnull List<? super ASTNode> nodes, @Nonnull ASTNode node) {
    ArrayDeque<ASTNode> stack = new ArrayDeque<>(CALL_EXPRESSION_DEPTH);
    stack.addLast(node.getFirstChildNode());
    while (!stack.isEmpty()) {
      if (stack.size() >= CALL_EXPRESSION_DEPTH) {
        throw new IllegalStateException("Too long call chain! " + node);
      }
      ASTNode currentNode = stack.removeLast();
      if (!FormatterUtil.containsWhiteSpacesOnly(currentNode)) {
        IElementType type = currentNode.getElementType();
        if (type == JavaElementType.METHOD_CALL_EXPRESSION ||
          type == JavaElementType.REFERENCE_EXPRESSION) {
          ASTNode firstChild = currentNode.getFirstChildNode();
          currentNode = FormatterUtil.getNextNonWhitespaceSibling(currentNode);
          ContainerUtil.addIfNotNull(stack, currentNode);
          ContainerUtil.addIfNotNull(stack, firstChild);
        }
        else {
          nodes.add(currentNode);
          currentNode = FormatterUtil.getNextNonWhitespaceSibling(currentNode);
          ContainerUtil.addIfNotNull(stack, currentNode);
        }
      }
      else {
        currentNode = FormatterUtil.getNextNonWhitespaceSibling(currentNode);
        ContainerUtil.addIfNotNull(stack, currentNode);
      }
    }
  }

  /**
   * Tries to define the wrap to use for the {@link Block block} for the given {@code 'child'} node. It's assumed that
   * given {@code 'child'} node is descendant (direct or indirect) of the given {@code 'parent'} node.
   * I.e. {@code 'parent'} node defines usage context for the {@code 'child'} node.
   *
   * @param child                 child node which {@link Wrap wrap} is to be defined
   * @param parent                direct or indirect parent of the given {@code 'child'} node. Defines usage context
   *                              of {@code 'child'} node processing
   * @param settings              code style settings to use during wrap definition
   * @param suggestedWrap         wrap suggested to use by clients of current class. I.e. those clients offer wrap to
   *                              use based on their information about current processing state. However, it's possible
   *                              that they don't know details of fine-grained wrap definition algorithm encapsulated
   *                              at the current class. Hence, this method takes suggested wrap into consideration but
   *                              is not required to use it all the time node based on the given parameters
   * @param reservedWrapsProvider reserved {@code 'element type -> wrap instance'} mappings provider. <b>Note:</b> this
   *                              argument is considered to be a part of legacy heritage and is intended to be removed as
   *                              soon as formatting code refactoring is done
   * @return wrap to use for the given {@code 'child'} node if it's possible to define the one;
   * {@code null} otherwise
   */
  @Nullable
  static Wrap arrangeChildWrap(ASTNode child,
                               ASTNode parent,
                               CommonCodeStyleSettings settings,
                               JavaCodeStyleSettings javaSettings,
                               Wrap suggestedWrap,
                               AbstractJavaBlock reservedWrapsProvider) {
    ASTNode directParent = child.getTreeParent();
    int role = ((CompositeElement)directParent).getChildRole(child);

    if (parent instanceof PsiPolyadicExpression) {
      if (role == ChildRole.OPERATION_SIGN && !settings.BINARY_OPERATION_SIGN_ON_NEXT_LINE) return null;
      boolean rOperand = ArrayUtil.indexOf(((PsiPolyadicExpression)parent).getOperands(), child.getPsi()) > 0;
      if (settings.BINARY_OPERATION_SIGN_ON_NEXT_LINE && rOperand) return null;
      return suggestedWrap;
    }

    IElementType nodeType = parent.getElementType();
    IElementType childType = child.getElementType();

    if (childType == JavaElementType.EXTENDS_LIST ||
      childType == JavaElementType.IMPLEMENTS_LIST ||
      childType == JavaElementType.PERMITS_LIST) {
      return Wrap.createWrap(settings.EXTENDS_KEYWORD_WRAP, true);
    }

    else if (childType == JavaElementType.THROWS_LIST) {
      return Wrap.createWrap(settings.THROWS_KEYWORD_WRAP, true);
    }

    else if (nodeType == JavaElementType.EXTENDS_LIST ||
      nodeType == JavaElementType.IMPLEMENTS_LIST ||
      nodeType == JavaElementType.PERMITS_LIST ||
      nodeType == JavaElementType.THROWS_LIST) {
      return role == ChildRole.REFERENCE_IN_LIST ? suggestedWrap : null;
    }

    else if (nodeType == JavaElementType.CONDITIONAL_EXPRESSION) {
      if (role == ChildRole.COLON && !settings.TERNARY_OPERATION_SIGNS_ON_NEXT_LINE) return null;
      if (role == ChildRole.QUEST && !settings.TERNARY_OPERATION_SIGNS_ON_NEXT_LINE) return null;
      if (role == ChildRole.THEN_EXPRESSION && settings.TERNARY_OPERATION_SIGNS_ON_NEXT_LINE) return null;
      if (role == ChildRole.ELSE_EXPRESSION && settings.TERNARY_OPERATION_SIGNS_ON_NEXT_LINE) return null;
      return suggestedWrap;
    }

    else if (isAssignment(parent) && role != ChildRole.TYPE) {
      if (role == ChildRole.INITIALIZER_EQ) return settings.PLACE_ASSIGNMENT_SIGN_ON_NEXT_LINE ? suggestedWrap : null;
      if (role == ChildRole.OPERATION_SIGN) return settings.PLACE_ASSIGNMENT_SIGN_ON_NEXT_LINE ? suggestedWrap : null;
      if (role == ChildRole.INITIALIZER) return settings.PLACE_ASSIGNMENT_SIGN_ON_NEXT_LINE ? null : suggestedWrap;
      if (role == ChildRole.ROPERAND) return settings.PLACE_ASSIGNMENT_SIGN_ON_NEXT_LINE ? null : suggestedWrap;
      if (role == ChildRole.CLOSING_SEMICOLON) return null;
      return suggestedWrap;
    }
    else if (nodeType == JavaElementType.REFERENCE_EXPRESSION) {
      if (role == ChildRole.DOT) {
        return reservedWrapsProvider.getReservedWrap(JavaElementType.REFERENCE_EXPRESSION);
      }
      else {
        return suggestedWrap;
      }
    }

    else if (nodeType == JavaElementType.FOR_STATEMENT) {
      if (role == ChildRole.FOR_INITIALIZATION || role == ChildRole.CONDITION || role == ChildRole.FOR_UPDATE) {
        return suggestedWrap;
      }
      if (role == ChildRole.LOOP_BODY) {
        final boolean dontWrap = (childType == JavaElementType.CODE_BLOCK || childType == JavaElementType.BLOCK_STATEMENT) &&
          settings.BRACE_STYLE == CommonCodeStyleSettings.END_OF_LINE;
        return Wrap.createWrap(dontWrap ? WrapType.NONE : WrapType.NORMAL, true);
      }
      else {
        return null;
      }
    }

    else if (parent.getPsi() instanceof PsiModifierListOwner) {
      ASTNode prev = FormatterUtil.getPreviousNonWhitespaceSibling(child);
      if (prev != null && prev.getElementType() == JavaElementType.MODIFIER_LIST) {
        ASTNode last = prev.getLastChildNode();
        if (last != null && last.getElementType() == JavaElementType.ANNOTATION) {
          if (javaSettings.DO_NOT_WRAP_AFTER_SINGLE_ANNOTATION && isModifierListWithSingleAnnotation(prev, JavaElementType.FIELD) ||
            javaSettings.DO_NOT_WRAP_AFTER_SINGLE_ANNOTATION_IN_PARAMETER && isModifierListWithSingleAnnotation(prev,
                                                                                                                JavaElementType.PARAMETER) ||
            isAnnotationAfterKeyword(last)
          ) {
            return Wrap.createWrap(WrapType.NONE, false);
          }
          else {
            return Wrap.createWrap(getWrapType(getAnnotationWrapType(parent, child, settings, javaSettings)), true);
          }
        }
      }

      return null;
    }

    else if (nodeType == JavaElementType.MODIFIER_LIST) {
      if (childType == JavaElementType.ANNOTATION) {
        ASTNode prev = FormatterUtil.getPreviousNonWhitespaceSibling(child);
        if (prev instanceof PsiKeyword) {
          return null;
        }
        else if (isAnnoInsideModifierListWithAtLeastOneKeyword(child, parent)) {
          return Wrap.createWrap(WrapType.NONE, false);
        }

        return Wrap.createWrap(getWrapType(getAnnotationWrapType(parent.getTreeParent(), child, settings, javaSettings)), true);
      }
      else if (childType == JavaTokenType.END_OF_LINE_COMMENT) {
        return Wrap.createWrap(WrapType.NORMAL, true);
      }

      ASTNode prev = FormatterUtil.getPreviousNonWhitespaceSibling(child);
      if (prev != null && prev.getElementType() == JavaElementType.ANNOTATION) {
        if (javaSettings.DO_NOT_WRAP_AFTER_SINGLE_ANNOTATION && isModifierListWithSingleAnnotation(parent, JavaElementType.FIELD)) {
          return Wrap.createWrap(WrapType.NONE, false);
        }
        Wrap wrap = Wrap.createWrap(getWrapType(getAnnotationWrapType(parent.getTreeParent(), child, settings, javaSettings)), true);
        putPreferredWrapInParentBlock(reservedWrapsProvider, wrap);
        return wrap;
      }

      return null;
    }

    else if (nodeType == JavaElementType.ASSERT_STATEMENT) {
      if (role == ChildRole.CONDITION) {
        return suggestedWrap;
      }
      if (role == ChildRole.ASSERT_DESCRIPTION && !settings.ASSERT_STATEMENT_COLON_ON_NEXT_LINE) {
        return suggestedWrap;
      }
      if (role == ChildRole.COLON && settings.ASSERT_STATEMENT_COLON_ON_NEXT_LINE) {
        return suggestedWrap;
      }
      return null;
    }

    else if (nodeType == JavaElementType.CODE_BLOCK) {
      if (child.getPsi() instanceof PsiStatement) {
        return suggestedWrap;
      }
      else {
        return null;
      }
    }

    else if (nodeType == JavaElementType.IF_STATEMENT) {
      if (childType == JavaElementType.IF_STATEMENT && role == ChildRole.ELSE_BRANCH && settings.SPECIAL_ELSE_IF_TREATMENT) {
        return Wrap.createWrap(WrapType.NONE, false);
      }
      if (role == ChildRole.THEN_BRANCH || role == ChildRole.ELSE_BRANCH) {
        if (childType == JavaElementType.BLOCK_STATEMENT) {
          return null;
        }
        else {
          return Wrap.createWrap(WrapType.NORMAL, true);
        }
      }
    }

    else if (nodeType == JavaElementType.FOREACH_STATEMENT || nodeType == JavaElementType.WHILE_STATEMENT) {
      if (role == ChildRole.LOOP_BODY) {
        if (childType == JavaElementType.BLOCK_STATEMENT) {
          return null;
        }
        else {
          return Wrap.createWrap(WrapType.NORMAL, true);
        }
      }
    }

    else if (nodeType == JavaElementType.DO_WHILE_STATEMENT) {
      if (role == ChildRole.LOOP_BODY ||
        role == ChildRole.WHILE_KEYWORD && isAfterNonBlockStatement(child)) {
        return Wrap.createWrap(WrapType.NORMAL, true);
      }
    }

    else if (nodeType == JavaElementType.ANNOTATION_ARRAY_INITIALIZER) {
      if (suggestedWrap != null) {
        return suggestedWrap;
      }
      if (role == ChildRole.ANNOTATION_VALUE) {
        return Wrap.createWrap(WrapType.NORMAL, true);
      }
    }

    else if (nodeType == JavaElementType.SWITCH_LABELED_RULE && childType == JavaElementType.BLOCK_STATEMENT) {
      boolean dontNeedBreak = settings.BRACE_STYLE == CommonCodeStyleSettings.END_OF_LINE;
      return Wrap.createWrap(dontNeedBreak ? WrapType.NONE : WrapType.NORMAL, true);
    }
    else if (nodeType == JavaElementType.SWITCH_LABELED_RULE && ElementType.JAVA_STATEMENT_BIT_SET.contains(childType)) {
      return Wrap.createWrap(WrapType.NORMAL, true);
    }

    return suggestedWrap;
  }

  /**
   * Check if annotation goes after a keyword (maybe even not directly).
   * <p>
   * Example: {@code private @Foo @Bar void method() {} }
   * Here both Foo and Bar are after keyword
   */
  private static boolean isAnnotationAfterKeyword(@Nonnull ASTNode annotation) {
    ASTNode current = annotation.getTreePrev();
    while (current != null) {
      if (current instanceof PsiKeyword) {
        return true;
      }
      current = current.getTreePrev();
    }
    return false;
  }

  private static boolean isAfterNonBlockStatement(@Nonnull ASTNode node) {
    ASTNode prev = node.getTreePrev();
    if (prev instanceof PsiWhiteSpace) prev = prev.getTreePrev();
    return prev != null && prev.getElementType() != JavaElementType.BLOCK_STATEMENT;
  }

  private static int getAnnotationWrapType(ASTNode parent,
                                           ASTNode child,
                                           CommonCodeStyleSettings settings,
                                           JavaCodeStyleSettings javaSettings) {
    IElementType nodeType = parent.getElementType();

    if (nodeType == JavaElementType.METHOD) {
      return settings.METHOD_ANNOTATION_WRAP;
    }

    if (nodeType == JavaElementType.CLASS) {
      if (child.getElementType() == JavaTokenType.END_OF_LINE_COMMENT) {
        return CommonCodeStyleSettings.DO_NOT_WRAP;
      }
      // There is a possible case that current document state is invalid from language syntax point of view, e.g. the user starts
      // typing field definition and re-formatting is triggered by 'auto insert javadoc' processing. Example:
      //     class Test {
      //         @NotNull Object
      //     }
      // Here '@NotNull' has a 'class' node as a parent but we want to use field annotation setting value.
      // Hence we check if subsequent parsed info is valid.
      for (ASTNode node = child.getTreeNext(); node != null; node = node.getTreeNext()) {
        if (node.getElementType() == TokenType.WHITE_SPACE || node instanceof PsiTypeElement) {
          continue;
        }
        if (node instanceof PsiErrorElement) {
          return settings.FIELD_ANNOTATION_WRAP;
        }
      }
      return settings.CLASS_ANNOTATION_WRAP;
    }

    if (nodeType == JavaElementType.FIELD) {
      return settings.FIELD_ANNOTATION_WRAP;
    }

    if (nodeType == JavaElementType.PARAMETER ||
      nodeType == JavaElementType.RECEIVER_PARAMETER ||
      nodeType == JavaElementType.RESOURCE_VARIABLE) {
      return settings.PARAMETER_ANNOTATION_WRAP;
    }

    if (nodeType == JavaElementType.LOCAL_VARIABLE) {
      return settings.VARIABLE_ANNOTATION_WRAP;
    }

    if (nodeType == JavaElementType.MODULE) {
      return settings.CLASS_ANNOTATION_WRAP;
    }

    if (nodeType == JavaElementType.ENUM_CONSTANT) {
      return javaSettings.ENUM_FIELD_ANNOTATION_WRAP;
    }

    return CommonCodeStyleSettings.DO_NOT_WRAP;
  }

  private static void putPreferredWrapInParentBlock(@Nonnull AbstractJavaBlock block, @Nonnull Wrap preferredWrap) {
    AbstractJavaBlock parentBlock = block.getParentBlock();
    if (parentBlock != null) {
      parentBlock.setReservedWrap(preferredWrap, JavaElementType.MODIFIER_LIST);
    }
  }

  private static boolean isModifierListWithSingleAnnotation(@Nonnull ASTNode elem, IElementType parentElementType) {
    ASTNode parent = elem.getTreeParent();
    if (parent != null && parent.getElementType() == parentElementType) {
      return isModifierListWithSingleAnnotation(elem);
    }
    return false;
  }

  private static boolean isModifierListWithSingleAnnotation(@Nonnull ASTNode elem) {
    if (elem.getPsi() instanceof PsiModifierList) {
      if (((PsiModifierList)elem.getPsi()).getAnnotations().length == 1) {
        return true;
      }
    }
    return false;
  }

  private static boolean isAnnoInsideModifierListWithAtLeastOneKeyword(@Nonnull ASTNode current, @Nonnull ASTNode parent) {
    if (current.getElementType() != JavaElementType.ANNOTATION || parent.getElementType() != JavaElementType.MODIFIER_LIST) return false;
    while (true) {
      current = FormatterUtil.getPreviousNonWhitespaceSibling(current);
      if (current instanceof PsiKeyword) {
        ASTNode grandParent = parent.getTreeParent();
        return grandParent != null && grandParent.getElementType() == JavaElementType.METHOD;
      }
      else if (current == null || current.getElementType() != JavaElementType.ANNOTATION) break;
    }
    return false;
  }

  /**
   * Creates {@link Wrap wrap} to be used with the children blocks of the given block.
   * <p>
   * It is important that single instance of wrap will be used for all children which use default wrap.
   *
   * @param block                 target block which sub-blocks should use wrap created by the current method
   * @param settings              code formatting settings to consider during wrap construction
   * @param reservedWrapsProvider reserved {@code 'element type -> wrap instance'} mappings provider. <b>Note:</b> this
   *                              argument is considered to be a part of legacy heritage and is intended to be removed as
   *                              soon as formatting code refactoring is done
   * @return wrap to use for the sub-blocks of the given block
   */
  @Nullable
  static Wrap createDefaultWrap(ASTBlock block,
                                CommonCodeStyleSettings settings,
                                JavaCodeStyleSettings javaSettings,
                                ReservedWrapsProvider reservedWrapsProvider) {
    ASTNode node = block.getNode();
    Wrap wrap = block.getWrap();
    if (node == null) return null;
    final IElementType nodeType = node.getElementType();
    if (nodeType == JavaElementType.EXTENDS_LIST ||
      nodeType == JavaElementType.IMPLEMENTS_LIST ||
      nodeType == JavaElementType.PERMITS_LIST) {
      return Wrap.createWrap(settings.EXTENDS_LIST_WRAP, false);
    }
    else if (node instanceof PsiPolyadicExpression) {
      Wrap actualWrap = wrap != null ? wrap : reservedWrapsProvider.getReservedWrap(JavaElementType.BINARY_EXPRESSION);
      if (actualWrap == null) {
        return Wrap.createWrap(settings.BINARY_OPERATION_WRAP, false);
      }
      else {
        if (areSamePriorityBinaryExpressions(node, node.getTreeParent())) {
          return actualWrap;
        }
        else {
          return Wrap.createChildWrap(actualWrap, WrapType.byLegacyRepresentation(settings.BINARY_OPERATION_WRAP), false);
        }
      }
    }
    else if (nodeType == JavaElementType.CONDITIONAL_EXPRESSION) {
      return Wrap.createWrap(settings.TERNARY_OPERATION_WRAP, false);
    }
    else if (nodeType == JavaElementType.ASSERT_STATEMENT) {
      return Wrap.createWrap(settings.ASSERT_STATEMENT_WRAP, false);
    }
    else if (nodeType == JavaElementType.FOR_STATEMENT) {
      return Wrap.createWrap(settings.FOR_STATEMENT_WRAP, false);
    }
    else if (nodeType == JavaElementType.THROWS_LIST) {
      return Wrap.createWrap(settings.THROWS_LIST_WRAP, true);
    }
    else if (nodeType == JavaElementType.CODE_BLOCK) {
      if (settings.KEEP_SIMPLE_METHODS_IN_ONE_LINE && node.getPsi().getParent() instanceof PsiMethod && !node.textContains('\n')) {
        return null;
      }
      return Wrap.createWrap(WrapType.NORMAL, false);
    }
    else if (isAssignment(node)) {
      return Wrap.createWrap(settings.ASSIGNMENT_WRAP, true);
    }
    else if (isTopLevelTypeInCatchSection(nodeType, node)) {
      return Wrap.createWrap(javaSettings.MULTI_CATCH_TYPES_WRAP, false);
    }
    else {
      return null;
    }
  }

  static boolean isTopLevelTypeInCatchSection(@Nonnull IElementType nodeType, ASTNode node) {
    if (nodeType != JavaElementType.TYPE) return false;
    ASTNode parent = node.getTreeParent();
    if (parent == null || parent.getElementType() != JavaElementType.PARAMETER) return false;
    ASTNode grandParent = parent.getTreeParent();
    return grandParent != null && grandParent.getElementType() == JavaElementType.CATCH_SECTION;
  }
}
