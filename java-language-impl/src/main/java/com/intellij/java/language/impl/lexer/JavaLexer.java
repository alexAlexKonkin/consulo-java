// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.java.language.impl.lexer;

import com.intellij.java.language.LanguageLevel;
import com.intellij.java.language.impl.psi.impl.source.tree.JavaDocElementType;
import com.intellij.java.language.psi.JavaTokenType;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenType;
import consulo.language.lexer.LexerBase;
import consulo.util.collection.CharSequenceHashingStrategy;
import consulo.util.collection.Sets;
import consulo.util.lang.CharArrayUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.intellij.java.language.psi.PsiKeyword.*;

public final class JavaLexer extends LexerBase {
  private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
    ABSTRACT, BOOLEAN, BREAK, BYTE, CASE, CATCH, CHAR, CLASS, CONST, CONTINUE, DEFAULT, DO, DOUBLE, ELSE, EXTENDS, FINAL, FINALLY,
    FLOAT, FOR, GOTO, IF, IMPLEMENTS, IMPORT, INSTANCEOF, INT, INTERFACE, LONG, NATIVE, NEW, PACKAGE, PRIVATE, PROTECTED, PUBLIC,
    RETURN, SHORT, STATIC, STRICTFP, SUPER, SWITCH, SYNCHRONIZED, THIS, THROW, THROWS, TRANSIENT, TRY, VOID, VOLATILE, WHILE,
    TRUE, FALSE, NULL, NON_SEALED));

  private static final Set<CharSequence> JAVA9_KEYWORDS = Sets.newHashSet(CharSequenceHashingStrategy.CASE_SENSITIVE);

  static {
    JAVA9_KEYWORDS.addAll(Set.of(OPEN, MODULE, REQUIRES, EXPORTS, OPENS, USES, PROVIDES, TRANSITIVE, TO, WITH));
  }

  public static boolean isKeyword(String id, @Nonnull LanguageLevel level) {
    return KEYWORDS.contains(id) ||
      level.isAtLeast(LanguageLevel.JDK_1_4) && ASSERT.equals(id) ||
      level.isAtLeast(LanguageLevel.JDK_1_5) && ENUM.equals(id);
  }

  public static boolean isSoftKeyword(CharSequence id, @Nonnull LanguageLevel level) {
    return level.isAtLeast(LanguageLevel.JDK_1_9) && JAVA9_KEYWORDS.contains(id) ||
      level.isAtLeast(LanguageLevel.JDK_10) && VAR.contentEquals(id) ||
      level.isAtLeast(LanguageLevel.JDK_16) && RECORD.contentEquals(id) ||
      level.isAtLeast(LanguageLevel.JDK_14) && YIELD.contentEquals(id) ||
      level.isAtLeast(LanguageLevel.JDK_17) && (SEALED.contentEquals(id) || PERMITS.contentEquals(id)) ||
      level.isAtLeast(LanguageLevel.JDK_20) && WHEN.contentEquals(id);
  }

  private final _JavaLexer myFlexLexer;
  private CharSequence myBuffer;
  @Nullable
  private char[] myBufferArray;
  private int myBufferIndex;
  private int myBufferEndOffset;
  private int myTokenEndOffset;  // positioned after the last symbol of the current token
  private IElementType myTokenType;

  public JavaLexer(@Nonnull LanguageLevel level) {
    myFlexLexer = new _JavaLexer(level);
  }

  @Override
  public void start(@Nonnull CharSequence buffer, int startOffset, int endOffset, int initialState) {
    myBuffer = buffer;
    myBufferArray = CharArrayUtil.fromSequenceWithoutCopying(buffer);
    myBufferIndex = startOffset;
    myBufferEndOffset = endOffset;
    myTokenType = null;
    myTokenEndOffset = startOffset;
    myFlexLexer.reset(myBuffer, startOffset, endOffset, 0);
  }

  @Override
  public int getState() {
    return 0;
  }

  @Override
  public IElementType getTokenType() {
    locateToken();
    return myTokenType;
  }

  @Override
  public int getTokenStart() {
    return myBufferIndex;
  }

  @Override
  public int getTokenEnd() {
    locateToken();
    return myTokenEndOffset;
  }

  @Override
  public void advance() {
    locateToken();
    myTokenType = null;
  }

  private void locateToken() {
    if (myTokenType != null) {
      return;
    }

    if (myTokenEndOffset == myBufferEndOffset) {
      myBufferIndex = myBufferEndOffset;
      return;
    }

    myBufferIndex = myTokenEndOffset;

    char c = charAt(myBufferIndex);
    switch (c) {
      case ' ':
      case '\t':
      case '\n':
      case '\r':
      case '\f':
        myTokenType = TokenType.WHITE_SPACE;
        myTokenEndOffset = getWhitespaces(myBufferIndex + 1);
        break;

      case '/':
        if (myBufferIndex + 1 >= myBufferEndOffset) {
          myTokenType = JavaTokenType.DIV;
          myTokenEndOffset = myBufferEndOffset;
        }
        else {
          char nextChar = charAt(myBufferIndex + 1);
          if (nextChar == '/') {
            myTokenType = JavaTokenType.END_OF_LINE_COMMENT;
            myTokenEndOffset = getLineTerminator(myBufferIndex + 2);
          }
          else if (nextChar == '*') {
            if (myBufferIndex + 2 >= myBufferEndOffset ||
              (charAt(myBufferIndex + 2)) != '*' ||
              (myBufferIndex + 3 < myBufferEndOffset &&
                (charAt(myBufferIndex + 3)) == '/')) {
              myTokenType = JavaTokenType.C_STYLE_COMMENT;
              myTokenEndOffset = getClosingComment(myBufferIndex + 2);
            }
            else {
              myTokenType = JavaDocElementType.DOC_COMMENT;
              myTokenEndOffset = getClosingComment(myBufferIndex + 3);
            }
          }
          else {
            flexLocateToken();
          }
        }
        break;

      case '#':
        if (myBufferIndex == 0 && myBufferIndex + 1 < myBufferEndOffset && charAt(myBufferIndex + 1) == '!') {
          myTokenType = JavaTokenType.END_OF_LINE_COMMENT;
          myTokenEndOffset = getLineTerminator(myBufferIndex + 2);
        }
        else {
          flexLocateToken();
        }
        break;
      case '\'':
        myTokenType = JavaTokenType.CHARACTER_LITERAL;
        myTokenEndOffset = getClosingQuote(myBufferIndex + 1, c);
        break;

      case '"':
        if (myBufferIndex + 2 < myBufferEndOffset && charAt(myBufferIndex + 2) == '"' && charAt(myBufferIndex + 1) == '"') {
          myTokenType = JavaTokenType.TEXT_BLOCK_LITERAL;
          myTokenEndOffset = getTextBlockEnd(myBufferIndex + 2);
        }
        else {
          myTokenType = JavaTokenType.STRING_LITERAL;
          myTokenEndOffset = getClosingQuote(myBufferIndex + 1, c);
        }
        break;

      default:
        flexLocateToken();
    }

    if (myTokenEndOffset > myBufferEndOffset) {
      myTokenEndOffset = myBufferEndOffset;
    }
  }

  private int getWhitespaces(int offset) {
    if (offset >= myBufferEndOffset) {
      return myBufferEndOffset;
    }

    int pos = offset;
    char c = charAt(pos);

    while (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f') {
      pos++;
      if (pos == myBufferEndOffset) {
        return pos;
      }
      c = charAt(pos);
    }

    return pos;
  }

  private void flexLocateToken() {
    try {
      myFlexLexer.goTo(myBufferIndex);
      myTokenType = myFlexLexer.advance();
      myTokenEndOffset = myFlexLexer.getTokenEnd();
    }
    catch (IOException e) { /* impossible */ }
  }

  private int getClosingQuote(int offset, char quoteChar) {
    if (offset >= myBufferEndOffset) {
      return myBufferEndOffset;
    }

    int pos = offset;
    char c = charAt(pos);

    while (true) {
      while (c != quoteChar && c != '\n' && c != '\r' && c != '\\') {
        pos++;
        if (pos >= myBufferEndOffset) {
          return myBufferEndOffset;
        }
        c = charAt(pos);
      }

      if (c == '\\') {
        pos++;
        if (pos >= myBufferEndOffset) {
          return myBufferEndOffset;
        }
        c = charAt(pos);
        if (c == '\n' || c == '\r') {
          continue;
        }
        if (c == 'u') {
          do {
            pos++;
          }
          while (pos < myBufferEndOffset && charAt(pos) == 'u');
          if (pos + 3 >= myBufferEndOffset) {
            return myBufferEndOffset;
          }
          boolean isBackSlash = charAt(pos) == '0' && charAt(pos + 1) == '0' && charAt(pos + 2) == '5' && charAt(pos + 3) == 'c';
          // on encoded backslash we also need to skip escaped symbol (e.g. \\u005c" is translated to \")
          pos += (isBackSlash ? 5 : 4);
        }
        else {
          pos++;
        }
        if (pos >= myBufferEndOffset) {
          return myBufferEndOffset;
        }
        c = charAt(pos);
      }
      else if (c == quoteChar) {
        break;
      }
      else {
        pos--;
        break;
      }
    }

    return pos + 1;
  }

  private int getClosingComment(int offset) {
    int pos = offset;

    while (pos < myBufferEndOffset - 1) {
      char c = charAt(pos);
      if (c == '*' && (charAt(pos + 1)) == '/') {
        break;
      }
      pos++;
    }

    return pos + 2;
  }

  private int getLineTerminator(int offset) {
    int pos = offset;

    while (pos < myBufferEndOffset) {
      char c = charAt(pos);
      if (c == '\r' || c == '\n') {
        break;
      }
      pos++;
    }

    return pos;
  }

  private int getTextBlockEnd(int offset) {
    int pos = offset;

    while ((pos = getClosingQuote(pos + 1, '"')) < myBufferEndOffset) {
      char current = charAt(pos);
      if (current == '\\') {
        pos++;
      }
      else if (current == '"' && pos + 1 < myBufferEndOffset && charAt(pos + 1) == '"') {
        pos += 2;
        break;
      }
    }

    return pos;
  }

  private char charAt(int position) {
    return myBufferArray != null ? myBufferArray[position] : myBuffer.charAt(position);
  }

  @Nonnull
  @Override
  public CharSequence getBufferSequence() {
    return myBuffer;
  }

  @Override
  public int getBufferEnd() {
    return myBufferEndOffset;
  }
}
