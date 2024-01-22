/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.java.language.impl.psi.impl.source.tree.java;

import com.intellij.java.language.impl.psi.impl.PsiImplUtil;
import com.intellij.java.language.impl.psi.impl.source.Constants;
import com.intellij.java.language.impl.psi.impl.source.tree.ChildRole;
import com.intellij.java.language.impl.psi.scope.ElementClassHint;
import com.intellij.java.language.psi.*;
import consulo.language.ast.ASTNode;
import consulo.language.ast.ChildRoleBase;
import consulo.language.ast.IElementType;
import consulo.language.impl.ast.TreeUtil;
import consulo.language.impl.psi.CompositePsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.resolve.PsiScopeProcessor;
import consulo.language.psi.resolve.ResolveState;
import consulo.logging.Logger;
import jakarta.annotation.Nonnull;

public class PsiDoWhileStatementImpl extends CompositePsiElement implements PsiDoWhileStatement, Constants {
  private static final Logger LOG = Logger.getInstance(PsiDoWhileStatementImpl.class);

  public PsiDoWhileStatementImpl() {
    super(DO_WHILE_STATEMENT);
  }

  @Override
  public PsiExpression getCondition() {
    return (PsiExpression) findChildByRoleAsPsiElement(ChildRole.CONDITION);
  }

  @Override
  public PsiStatement getBody() {
    return (PsiStatement) findChildByRoleAsPsiElement(ChildRole.LOOP_BODY);
  }

  @Override
  public PsiKeyword getWhileKeyword() {
    return (PsiKeyword) findChildByRoleAsPsiElement(ChildRole.WHILE_KEYWORD);
  }

  @Override
  public PsiJavaToken getLParenth() {
    return (PsiJavaToken) findChildByRoleAsPsiElement(ChildRole.LPARENTH);
  }

  @Override
  public PsiJavaToken getRParenth() {
    return (PsiJavaToken) findChildByRoleAsPsiElement(ChildRole.RPARENTH);
  }

  @Override
  public ASTNode findChildByRole(int role) {
    LOG.assertTrue(ChildRole.isUnique(role));
    switch (role) {
      default:
        return null;

      case ChildRole.DO_KEYWORD:
        return findChildByType(DO_KEYWORD);

      case ChildRole.LOOP_BODY:
        return PsiImplUtil.findStatementChild(this);

      case ChildRole.WHILE_KEYWORD:
        return findChildByType(WHILE_KEYWORD);

      case ChildRole.LPARENTH:
        return findChildByType(LPARENTH);

      case ChildRole.CONDITION:
        return findChildByType(EXPRESSION_BIT_SET);

      case ChildRole.RPARENTH:
        return findChildByType(RPARENTH);

      case ChildRole.CLOSING_SEMICOLON:
        return TreeUtil.findChildBackward(this, SEMICOLON);
    }
  }

  @Override
  public int getChildRole(ASTNode child) {
    LOG.assertTrue(child.getTreeParent() == this);
    IElementType i = child.getElementType();
    if (i == DO_KEYWORD) {
      return ChildRole.DO_KEYWORD;
    } else if (i == WHILE_KEYWORD) {
      return ChildRole.WHILE_KEYWORD;
    } else if (i == LPARENTH) {
      return ChildRole.LPARENTH;
    } else if (i == RPARENTH) {
      return ChildRole.RPARENTH;
    } else if (i == SEMICOLON) {
      return ChildRole.CLOSING_SEMICOLON;
    } else {
      if (EXPRESSION_BIT_SET.contains(child.getElementType())) {
        return ChildRole.CONDITION;
      } else if (child.getPsi() instanceof PsiStatement) {
        return ChildRole.LOOP_BODY;
      } else {
        return ChildRoleBase.NONE;
      }
    }
  }

  @Override
  public boolean processDeclarations(@jakarta.annotation.Nonnull PsiScopeProcessor processor,
                                     @Nonnull ResolveState state,
                                     PsiElement lastParent,
                                     @Nonnull PsiElement place) {
    if (lastParent != null) return true;
    ElementClassHint elementClassHint = processor.getHint(ElementClassHint.KEY);
    if (elementClassHint != null && !elementClassHint.shouldProcess(ElementClassHint.DeclarationKind.VARIABLE)) return true;
    return PsiWhileStatementImpl.processDeclarationsInLoopCondition(processor, state, place, this);
  }

  @Override
  public void accept(@Nonnull PsiElementVisitor visitor) {
    if (visitor instanceof JavaElementVisitor) {
      ((JavaElementVisitor) visitor).visitDoWhileStatement(this);
    } else {
      visitor.visitElement(this);
    }
  }

  public String toString() {
    return "PsiDoWhileStatement";
  }
}
