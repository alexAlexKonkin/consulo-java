// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.java.language.impl.parser;

import com.intellij.java.language.JavaPsiBundle;
import com.intellij.java.language.impl.psi.impl.source.tree.JavaElementType;
import com.intellij.java.language.psi.JavaTokenType;
import com.intellij.java.language.psi.PsiKeyword;
import consulo.language.ast.TokenSet;
import consulo.language.parser.PsiBuilder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Contract;

import static com.intellij.java.language.impl.parser.JavaParserUtil.*;
import static consulo.language.parser.PsiBuilderUtil.expect;

public class PatternParser implements JavaElementType {
  private static final TokenSet PATTERN_MODIFIERS = TokenSet.create(JavaTokenType.FINAL_KEYWORD);

  private final JavaParser myParser;

  public PatternParser(@Nonnull JavaParser javaParser) {
    myParser = javaParser;
  }

  /**
   * Checks whether given token sequence can be parsed as a pattern.
   * The result of the method makes sense only for places where pattern is expected (case label and instanceof expression).
   */
  @Contract(pure = true)
  public boolean isPattern(final PsiBuilder builder) {
    PsiBuilder.Marker patternStart = preParsePattern(builder, true);
    if (patternStart == null) {
      return false;
    }
    patternStart.rollbackTo();
    return true;
  }

  private boolean parseUnnamedPattern(final PsiBuilder builder) {
    PsiBuilder.Marker patternStart = builder.mark();
    if (builder.getTokenType() == JavaTokenType.IDENTIFIER &&
      "_".equals(builder.getTokenText())) {
      emptyElement(builder, TYPE);
      builder.advanceLexer();
      done(patternStart, UNNAMED_PATTERN);
      return true;
    }
    patternStart.rollbackTo();
    return false;
  }


  /**
   * @return null when not pattern
   */
  @Nullable
  PsiBuilder.Marker preParsePattern(final PsiBuilder builder, boolean parensAllowed) {
    PsiBuilder.Marker patternStart = builder.mark();
    if (parensAllowed) {
      while (builder.getTokenType() == JavaTokenType.LPARENTH) {
        builder.advanceLexer();
      }
    }
    Boolean hasNoModifier = myParser.getDeclarationParser().parseModifierList(builder, PATTERN_MODIFIERS).second;
    PsiBuilder.Marker type =
      myParser.getReferenceParser().parseType(builder, ReferenceParser.EAT_LAST_DOT | ReferenceParser.WILDCARD);
    boolean isPattern = type != null && (builder.getTokenType() == JavaTokenType.IDENTIFIER ||
      (builder.getTokenType() == JavaTokenType.LPARENTH && hasNoModifier));
    if (!isPattern) {
      patternStart.rollbackTo();
      return null;
    }
    return patternStart;
  }

  /**
   * Must be called only if isPattern returned true
   */
  public PsiBuilder.Marker parsePattern(final PsiBuilder builder) {
    return parsePattern(builder, false);
  }

  private PsiBuilder.Marker parsePattern(final PsiBuilder builder, boolean expectVar) {
    return parsePrimaryPattern(builder, expectVar);
  }

  PsiBuilder.Marker parsePrimaryPattern(final PsiBuilder builder, boolean expectVar) {
    if (builder.getTokenType() == JavaTokenType.LPARENTH) {
      PsiBuilder.Marker parenPattern = builder.mark();
      builder.advanceLexer();
      parsePattern(builder);
      if (!expect(builder, JavaTokenType.RPARENTH)) {
        error(builder, JavaPsiBundle.message("expected.rparen"));
      }
      done(parenPattern, PARENTHESIZED_PATTERN);
      return parenPattern;
    }
    return parseTypeOrRecordPattern(builder, expectVar);
  }

  private void parseRecordStructurePattern(final PsiBuilder builder) {
    PsiBuilder.Marker recordStructure = builder.mark();
    boolean hasLparen = expect(builder, JavaTokenType.LPARENTH);
    assert hasLparen;

    boolean isFirst = true;
    while (builder.getTokenType() != JavaTokenType.RPARENTH) {
      if (!isFirst) {
        expectOrError(builder, JavaTokenType.COMMA, "expected.comma");
      }

      if (builder.getTokenType() == null) {
        break;
      }

      if (isPattern(builder)) {
        parsePattern(builder, true);
        isFirst = false;
      }
      else if (parseUnnamedPattern(builder)) {
        isFirst = false;
      }
      else {
        int flags = ReferenceParser.EAT_LAST_DOT | ReferenceParser.WILDCARD | ReferenceParser.VAR_TYPE;
        myParser.getReferenceParser().parseType(builder, flags);
        error(builder, JavaPsiBundle.message("expected.pattern"));
        if (builder.getTokenType() == JavaTokenType.RPARENTH) {
          break;
        }
        builder.advanceLexer();
      }
    }
    if (!expect(builder, JavaTokenType.RPARENTH)) {
      builder.error(JavaPsiBundle.message("expected.rparen"));
    }
    recordStructure.done(DECONSTRUCTION_LIST);
  }

  private PsiBuilder.Marker parseTypeOrRecordPattern(final PsiBuilder builder, boolean expectVar) {
    PsiBuilder.Marker pattern = builder.mark();
    PsiBuilder.Marker patternVariable = builder.mark();
    Boolean hasNoModifiers = myParser.getDeclarationParser().parseModifierList(builder, PATTERN_MODIFIERS).second;

    int flags = ReferenceParser.EAT_LAST_DOT | ReferenceParser.WILDCARD;
    if (expectVar) {
      flags |= ReferenceParser.VAR_TYPE;
    }
    PsiBuilder.Marker type = myParser.getReferenceParser().parseType(builder, flags);
    assert type != null; // guarded by isPattern
    boolean isRecord = false;
    if (builder.getTokenType() == JavaTokenType.LPARENTH && hasNoModifiers) {
      parseRecordStructurePattern(builder);
      isRecord = true;
    }

    final boolean hasIdentifier;
    if (builder.getTokenType() == JavaTokenType.IDENTIFIER &&
      (!PsiKeyword.WHEN.equals(builder.getTokenText()) || isWhenAsIdentifier(isRecord))) {
      // pattern variable after the record structure pattern
      if (isRecord) {
        PsiBuilder.Marker variable = builder.mark();
        builder.advanceLexer();
        variable.done(DECONSTRUCTION_PATTERN_VARIABLE);
      }
      else {
        builder.advanceLexer();
      }
      hasIdentifier = true;
    }
    else {
      hasIdentifier = false;
    }

    if (isRecord) {
      patternVariable.drop();
      done(pattern, DECONSTRUCTION_PATTERN);
    }
    else {
      if (hasIdentifier) {
        done(patternVariable, PATTERN_VARIABLE);
      }
      else {
        patternVariable.drop();
      }
      done(pattern, TYPE_TEST_PATTERN);
    }
    return pattern;
  }

  // There may be valid code samples:
  // Rec(int i) when  when     when.foo() -> {} //now it is unsupported, let's skip it
  //            ^name ^keyword ^guard expr
  //case When when -> {}
  //            ^name
  //case When(when) when              when ->{}
  //                  ^keyword         ^guard expr
  private static boolean isWhenAsIdentifier(boolean previousIsRecord) {
    if (previousIsRecord) return false;
    return true;
  }
}
