// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.java.language.impl.psi.impl.source.tree.java;

import com.intellij.java.language.impl.psi.impl.PsiImplUtil;
import com.intellij.java.language.impl.psi.impl.source.Constants;
import com.intellij.java.language.impl.psi.impl.source.tree.ChildRole;
import com.intellij.java.language.psi.PsiExpression;
import com.intellij.java.language.psi.PsiForeachStatementBase;
import com.intellij.java.language.psi.PsiJavaToken;
import com.intellij.java.language.psi.PsiStatement;
import consulo.language.ast.ASTNode;
import consulo.language.ast.ChildRoleBase;
import consulo.language.ast.IElementType;
import consulo.logging.Logger;
import jakarta.annotation.Nonnull;

import java.util.Objects;

public class PsiForeachStatementBaseImpl extends PsiLoopStatementImpl implements PsiForeachStatementBase, Constants {
  private static final Logger LOG = Logger.getInstance(PsiForeachStatementBaseImpl.class);

  public PsiForeachStatementBaseImpl(IElementType statementType) {
    super(statementType);
  }

  @Override
  public PsiExpression getIteratedValue() {
    return (PsiExpression)findChildByRoleAsPsiElement(ChildRole.FOR_ITERATED_VALUE);
  }

  @Override
  public PsiStatement getBody() {
    return (PsiStatement)findChildByRoleAsPsiElement(ChildRole.LOOP_BODY);
  }

  @Override
  @Nonnull
  public PsiJavaToken getLParenth() {
    return (PsiJavaToken)Objects.requireNonNull(findChildByRoleAsPsiElement(ChildRole.LPARENTH));
  }

  @Override
  public PsiJavaToken getRParenth() {
    return (PsiJavaToken)findChildByRoleAsPsiElement(ChildRole.RPARENTH);
  }

  @Override
  public ASTNode findChildByRole(int role) {
    LOG.assertTrue(ChildRole.isUnique(role));

    switch (role) {
      case ChildRole.LOOP_BODY:
        return PsiImplUtil.findStatementChild(this);

      case ChildRole.FOR_ITERATED_VALUE:
        return findChildByType(EXPRESSION_BIT_SET);

      case ChildRole.FOR_KEYWORD:
        return getFirstChildNode();

      case ChildRole.LPARENTH:
        return findChildByType(LPARENTH);

      case ChildRole.RPARENTH:
        return findChildByType(RPARENTH);

      case ChildRole.COLON:
        return findChildByType(COLON);

      case ChildRole.FOR_ITERATION_PARAMETER:
        return findChildByType(PARAMETER);

      default:
        return null;
    }
  }

  @Override
  public int getChildRole(@Nonnull ASTNode child) {
    LOG.assertTrue(child.getTreeParent() == this);

    IElementType i = child.getElementType();
    if (i == FOR_KEYWORD) {
      return ChildRole.FOR_KEYWORD;
    }
    else if (i == LPARENTH) {
      return ChildRole.LPARENTH;
    }
    else if (i == RPARENTH) {
      return ChildRole.RPARENTH;
    }
    else if (i == COLON) {
      return ChildRole.COLON;
    }
    else if (i == PARAMETER) {
      return ChildRole.FOR_ITERATION_PARAMETER;
    }
    else {
      if (EXPRESSION_BIT_SET.contains(child.getElementType())) {
        return ChildRole.FOR_ITERATED_VALUE;
      }
      else if (child.getPsi() instanceof PsiStatement) {
        return ChildRole.LOOP_BODY;
      }
      else {
        return ChildRoleBase.NONE;
      }
    }
  }
}
