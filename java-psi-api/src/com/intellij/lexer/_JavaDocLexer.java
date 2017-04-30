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

/* The following code was generated by JFlex 1.4.1 on 15.09.08 21:08 */

/* It's an automatically generated code. Do not modify it. */
package com.intellij.lexer;

import com.intellij.psi.tree.IElementType;


/**
 * This class is a scanner generated by 
 * <a href="http://www.jflex.de/">JFlex</a> 1.4.1
 * on 15.09.08 21:08 from the specification file
 * <tt>C:/JetBrains/IDEA/tools/lexer/_JavaDocLexer.flex</tt>
 */
public class _JavaDocLexer implements FlexLexer { /** initial size of the lookahead buffer */
  private static final int ZZ_BUFFERSIZE = 16384;

  /** lexical states */
  public static final int INLINE_TAG_NAME = 8;
  public static final int PARAM_TAG_SPACE = 4;
  public static final int COMMENT_DATA = 2;
  public static final int DOC_TAG_VALUE = 5;
  public static final int COMMENT_DATA_START = 1;
  public static final int TAG_DOC_SPACE = 3;
  public static final int YYINITIAL = 0;
  public static final int DOC_TAG_VALUE_IN_PAREN = 6;
  public static final int DOC_TAG_VALUE_IN_LTGT = 7;

  /** 
   * Translates characters to character classes
   */
  private static final String ZZ_CMAP_PACKED = 
    "\11\0\1\2\1\1\1\0\1\2\1\1\22\0\1\2\2\0\1\13"+
    "\1\4\3\0\1\11\1\12\1\7\1\0\1\14\1\5\1\3\1\6"+
    "\12\3\1\5\1\0\1\22\1\0\1\23\1\0\1\15\32\4\1\10"+
    "\1\0\1\10\1\0\1\4\1\0\1\17\13\4\1\21\2\4\1\16"+
    "\1\4\1\20\10\4\1\24\1\0\1\25\44\0\4\4\4\0\1\4"+
    "\12\0\1\4\4\0\1\4\5\0\27\4\1\0\37\4\1\0\u013f\4"+
    "\31\0\162\4\4\0\14\4\16\0\5\4\11\0\1\4\213\0\1\4"+
    "\13\0\1\4\1\0\3\4\1\0\1\4\1\0\24\4\1\0\54\4"+
    "\1\0\46\4\1\0\5\4\4\0\202\4\10\0\105\4\1\0\46\4"+
    "\2\0\2\4\6\0\20\4\41\0\46\4\2\0\1\4\7\0\47\4"+
    "\110\0\33\4\5\0\3\4\56\0\32\4\5\0\13\4\43\0\2\4"+
    "\1\0\143\4\1\0\1\4\17\0\2\4\7\0\2\4\12\0\3\4"+
    "\2\0\1\4\20\0\1\4\1\0\36\4\35\0\3\4\60\0\46\4"+
    "\13\0\1\4\u0152\0\66\4\3\0\1\4\22\0\1\4\7\0\12\4"+
    "\43\0\10\4\2\0\2\4\2\0\26\4\1\0\7\4\1\0\1\4"+
    "\3\0\4\4\3\0\1\4\36\0\2\4\1\0\3\4\16\0\4\4"+
    "\21\0\6\4\4\0\2\4\2\0\26\4\1\0\7\4\1\0\2\4"+
    "\1\0\2\4\1\0\2\4\37\0\4\4\1\0\1\4\23\0\3\4"+
    "\20\0\11\4\1\0\3\4\1\0\26\4\1\0\7\4\1\0\2\4"+
    "\1\0\5\4\3\0\1\4\22\0\1\4\17\0\2\4\17\0\1\4"+
    "\23\0\10\4\2\0\2\4\2\0\26\4\1\0\7\4\1\0\2\4"+
    "\1\0\5\4\3\0\1\4\36\0\2\4\1\0\3\4\17\0\1\4"+
    "\21\0\1\4\1\0\6\4\3\0\3\4\1\0\4\4\3\0\2\4"+
    "\1\0\1\4\1\0\2\4\3\0\2\4\3\0\3\4\3\0\10\4"+
    "\1\0\3\4\77\0\1\4\13\0\10\4\1\0\3\4\1\0\27\4"+
    "\1\0\12\4\1\0\5\4\46\0\2\4\43\0\10\4\1\0\3\4"+
    "\1\0\27\4\1\0\12\4\1\0\5\4\3\0\1\4\40\0\1\4"+
    "\1\0\2\4\43\0\10\4\1\0\3\4\1\0\27\4\1\0\20\4"+
    "\46\0\2\4\43\0\22\4\3\0\30\4\1\0\11\4\1\0\1\4"+
    "\2\0\7\4\72\0\60\4\1\0\2\4\13\0\10\4\72\0\2\4"+
    "\1\0\1\4\2\0\2\4\1\0\1\4\2\0\1\4\6\0\4\4"+
    "\1\0\7\4\1\0\3\4\1\0\1\4\1\0\1\4\2\0\2\4"+
    "\1\0\4\4\1\0\2\4\11\0\1\4\2\0\5\4\1\0\1\4"+
    "\25\0\2\4\42\0\1\4\77\0\10\4\1\0\42\4\35\0\4\4"+
    "\164\0\42\4\1\0\5\4\1\0\2\4\45\0\6\4\112\0\46\4"+
    "\12\0\51\4\7\0\132\4\5\0\104\4\5\0\122\4\6\0\7\4"+
    "\1\0\77\4\1\0\1\4\1\0\4\4\2\0\7\4\1\0\1\4"+
    "\1\0\4\4\2\0\47\4\1\0\1\4\1\0\4\4\2\0\37\4"+
    "\1\0\1\4\1\0\4\4\2\0\7\4\1\0\1\4\1\0\4\4"+
    "\2\0\7\4\1\0\7\4\1\0\27\4\1\0\37\4\1\0\1\4"+
    "\1\0\4\4\2\0\7\4\1\0\47\4\1\0\23\4\105\0\125\4"+
    "\14\0\u026c\4\2\0\10\4\12\0\32\4\5\0\113\4\3\0\3\4"+
    "\17\0\15\4\1\0\4\4\16\0\22\4\16\0\22\4\16\0\15\4"+
    "\1\0\3\4\17\0\64\4\43\0\1\4\3\0\2\4\103\0\130\4"+
    "\10\0\51\4\127\0\35\4\63\0\36\4\2\0\5\4\u038b\0\154\4"+
    "\224\0\234\4\4\0\132\4\6\0\26\4\2\0\6\4\2\0\46\4"+
    "\2\0\6\4\2\0\10\4\1\0\1\4\1\0\1\4\1\0\1\4"+
    "\1\0\37\4\2\0\65\4\1\0\7\4\1\0\1\4\3\0\3\4"+
    "\1\0\7\4\3\0\4\4\2\0\6\4\4\0\15\4\5\0\3\4"+
    "\1\0\7\4\102\0\2\4\23\0\1\4\34\0\1\4\15\0\1\4"+
    "\40\0\22\4\120\0\1\4\4\0\1\4\2\0\12\4\1\0\1\4"+
    "\3\0\5\4\6\0\1\4\1\0\1\4\1\0\1\4\1\0\4\4"+
    "\1\0\3\4\1\0\7\4\3\0\3\4\5\0\5\4\26\0\44\4"+
    "\u0e81\0\3\4\31\0\11\4\7\0\5\4\2\0\5\4\4\0\126\4"+
    "\6\0\3\4\1\0\137\4\5\0\50\4\4\0\136\4\21\0\30\4"+
    "\70\0\20\4\u0200\0\u19b6\4\112\0\u51a6\4\132\0\u048d\4\u0773\0\u2ba4\4"+
    "\u215c\0\u012e\4\2\0\73\4\225\0\7\4\14\0\5\4\5\0\1\4"+
    "\1\0\12\4\1\0\15\4\1\0\5\4\1\0\1\4\1\0\2\4"+
    "\1\0\2\4\1\0\154\4\41\0\u016b\4\22\0\100\4\2\0\66\4"+
    "\50\0\15\4\66\0\2\4\30\0\3\4\31\0\1\4\6\0\5\4"+
    "\1\0\207\4\7\0\1\4\34\0\32\4\4\0\1\4\1\0\32\4"+
    "\12\0\132\4\3\0\6\4\2\0\6\4\2\0\6\4\2\0\3\4"+
    "\3\0\2\4\3\0\2\4\31\0";

  /** 
   * Translates characters to character classes
   */
  private static final char [] ZZ_CMAP = zzUnpackCMap(ZZ_CMAP_PACKED);

  /** 
   * Translates DFA states to action switch labels.
   */
  private static final int [] ZZ_ACTION = zzUnpackAction();

  private static final String ZZ_ACTION_PACKED_0 =
    "\11\0\3\1\1\2\1\3\2\2\1\4\1\5\1\6"+
    "\1\7\1\10\1\11\1\12\1\13\1\14\1\15\1\16"+
    "\1\17\1\12\1\20\1\1\1\0\1\21\1\0\2\22"+
    "\1\23\3\22\1\24";

  private static int [] zzUnpackAction() {
    int [] result = new int[41];
    int offset = 0;
    offset = zzUnpackAction(ZZ_ACTION_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackAction(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }


  /** 
   * Translates a state to a row index in the transition table
   */
  private static final int [] ZZ_ROWMAP = zzUnpackRowMap();

  private static final String ZZ_ROWMAP_PACKED_0 =
    "\0\0\0\26\0\54\0\102\0\130\0\156\0\204\0\232"+
    "\0\260\0\306\0\334\0\362\0\306\0\u0108\0\362\0\u011e"+
    "\0\306\0\306\0\u0134\0\u014a\0\u0160\0\u0176\0\u018c\0\306"+
    "\0\306\0\306\0\306\0\306\0\u01a2\0\306\0\u011e\0\u01b8"+
    "\0\306\0\362\0\u01ce\0\u01e4\0\u01fa\0\u0210\0\u0226\0\u023c"+
    "\0\u01ce";

  private static int [] zzUnpackRowMap() {
    int [] result = new int[41];
    int offset = 0;
    offset = zzUnpackRowMap(ZZ_ROWMAP_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackRowMap(String packed, int offset, int [] result) {
    int i = 0;  /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int high = packed.charAt(i++) << 16;
      result[j++] = high | packed.charAt(i++);
    }
    return j;
  }

  /** 
   * The transition table of the DFA
   */
  private static final int [] ZZ_TRANS = zzUnpackTrans();

  private static final String ZZ_TRANS_PACKED_0 =
    "\6\12\1\13\1\14\16\12\1\15\2\16\4\15\1\17"+
    "\5\15\1\20\6\15\1\21\1\22\1\15\1\16\1\23"+
    "\4\15\1\17\14\15\1\21\1\22\1\12\2\24\4\12"+
    "\1\14\15\12\1\22\1\12\2\25\4\12\1\14\16\12"+
    "\1\15\2\26\2\27\2\15\1\17\1\27\1\30\1\15"+
    "\1\31\1\32\1\15\4\27\1\33\2\15\1\22\1\12"+
    "\2\16\2\27\2\12\1\14\1\27\1\12\1\34\1\12"+
    "\1\32\1\12\4\27\10\12\1\35\2\12\1\14\6\12"+
    "\4\35\1\12\1\36\11\12\1\14\5\12\1\37\10\12"+
    "\35\0\1\40\24\0\1\41\1\42\17\0\2\16\27\0"+
    "\1\43\11\0\1\44\3\43\6\0\1\23\24\0\2\24"+
    "\24\0\2\25\24\0\2\26\26\0\2\27\3\0\1\27"+
    "\5\0\4\27\7\0\3\35\10\0\4\35\13\0\1\45"+
    "\21\0\3\43\10\0\4\43\7\0\3\43\10\0\1\43"+
    "\1\46\2\43\12\0\1\41\22\0\3\43\10\0\2\43"+
    "\1\47\1\43\7\0\3\43\10\0\1\43\1\50\2\43"+
    "\7\0\3\43\10\0\3\43\1\51\4\0";

  private static int [] zzUnpackTrans() {
    int [] result = new int[594];
    int offset = 0;
    offset = zzUnpackTrans(ZZ_TRANS_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackTrans(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      value--;
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }


  /* error codes */
  private static final int ZZ_UNKNOWN_ERROR = 0;
  private static final int ZZ_NO_MATCH = 1;
  private static final int ZZ_PUSHBACK_2BIG = 2;
  private static final char[] EMPTY_BUFFER = new char[0];
  private static final int YYEOF = -1;
  private static java.io.Reader zzReader = null; // Fake

  /* error messages for the codes above */
  private static final String ZZ_ERROR_MSG[] = {
    "Unkown internal scanner error",
    "Error: could not match input",
    "Error: pushback value was too large"
  };

  /**
   * ZZ_ATTRIBUTE[aState] contains the attributes of state <code>aState</code>
   */
  private static final int [] ZZ_ATTRIBUTE = zzUnpackAttribute();

  private static final String ZZ_ATTRIBUTE_PACKED_0 =
    "\11\0\1\11\2\1\1\11\3\1\2\11\5\1\5\11"+
    "\1\1\1\11\1\1\1\0\1\11\1\0\7\1";

  private static int [] zzUnpackAttribute() {
    int [] result = new int[41];
    int offset = 0;
    offset = zzUnpackAttribute(ZZ_ATTRIBUTE_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackAttribute(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }

  /** the current state of the DFA */
  private int zzState;

  /** the current lexical state */
  private int zzLexicalState = YYINITIAL;

  /** this buffer contains the current text to be matched and is
      the source of the yytext() string */
  private CharSequence zzBuffer = "";

  /** this buffer may contains the current text array to be matched when it is cheap to acquire it */
  private char[] zzBufferArray;

  /** the textposition at the last accepting state */
  private int zzMarkedPos;

  /** the textposition at the last state to be included in yytext */
  private int zzPushbackPos;

  /** the current text position in the buffer */
  private int zzCurrentPos;

  /** startRead marks the beginning of the yytext() string in the buffer */
  private int zzStartRead;

  /** endRead marks the last character in the buffer, that has been read
      from input */
  private int zzEndRead;

  /**
   * zzAtBOL == true <=> the scanner is currently at the beginning of a line
   */
  private boolean zzAtBOL = true;

  /** zzAtEOF == true <=> the scanner is at the EOF */
  private boolean zzAtEOF;

  /** denotes if the user-EOF-code has already been executed */
  private boolean zzEOFDone;

  /* user code: */
  private boolean myJdk15Enabled;
  private DocCommentTokenTypes myTokenTypes;

  public _JavaDocLexer(boolean isJdk15Enabled, DocCommentTokenTypes tokenTypes) {
    this((java.io.Reader)null);
    myJdk15Enabled = isJdk15Enabled;
    myTokenTypes = tokenTypes;
  }

  public boolean checkAhead(char c) {
     if (zzMarkedPos >= zzBuffer.length()) return false;
     return zzBuffer.charAt(zzMarkedPos) == c;
  }

  public void goTo(int offset) {
    zzCurrentPos = zzMarkedPos = zzStartRead = offset;
    zzPushbackPos = 0;
    zzAtEOF = offset < zzEndRead;
  }


  _JavaDocLexer(java.io.Reader in) {
    this.zzReader = in;
  }

  /**
   * Creates a new scanner.
   * There is also java.io.Reader version of this constructor.
   *
   * @param   in  the java.io.Inputstream to read input from.
   */
  _JavaDocLexer(java.io.InputStream in) {
    this(new java.io.InputStreamReader(in));
  }

  /** 
   * Unpacks the compressed character translation table.
   *
   * @param packed   the packed character translation table
   * @return         the unpacked character translation table
   */
  private static char [] zzUnpackCMap(String packed) {
    char [] map = new char[0x10000];
    int i = 0;  /* index in packed string  */
    int j = 0;  /* index in unpacked array */
    while (i < 1250) {
      int  count = packed.charAt(i++);
      char value = packed.charAt(i++);
      do map[j++] = value; while (--count > 0);
    }
    return map;
  }

  @Override
  public final int getTokenStart(){
    return zzStartRead;
  }

  @Override
  public final int getTokenEnd(){
    return getTokenStart() + yylength();
  }

  @Override
  public void reset(CharSequence buffer, int start, int end,int initialState){
    zzBuffer = buffer;
    zzBufferArray = com.intellij.util.text.CharArrayUtil.fromSequenceWithoutCopying(buffer);
    zzCurrentPos = zzMarkedPos = zzStartRead = start;
    zzPushbackPos = 0;
    zzAtEOF  = false;
    zzAtBOL = true;
    zzEndRead = end;
    yybegin(initialState);
  }

  /**
   * Refills the input buffer.
   *
   * @return      <code>false</code>, iff there was new input.
   *
   * @exception   java.io.IOException  if any I/O-Error occurs
   */
  private boolean zzRefill() throws java.io.IOException {
    return true;
  }


  /**
   * Returns the current lexical state.
   */
  @Override
  public final int yystate() {
    return zzLexicalState;
  }


  /**
   * Enters a new lexical state
   *
   * @param newState the new lexical state
   */
  @Override
  public final void yybegin(int newState) {
    zzLexicalState = newState;
  }


  /**
   * Returns the text matched by the current regular expression.
   */
  public final CharSequence yytext() {
    return zzBuffer.subSequence(zzStartRead, zzMarkedPos);
  }


  /**
   * Returns the character at position <tt>pos</tt> from the
   * matched text.
   *
   * It is equivalent to yytext().charAt(pos), but faster
   *
   * @param pos the position of the character to fetch.
   *            A value from 0 to yylength()-1.
   *
   * @return the character at position pos
   */
  public final char yycharat(int pos) {
    return zzBufferArray != null ? zzBufferArray[zzStartRead+pos]:zzBuffer.charAt(zzStartRead+pos);
  }


  /**
   * Returns the length of the matched text region.
   */
  public final int yylength() {
    return zzMarkedPos-zzStartRead;
  }


  /**
   * Reports an error that occured while scanning.
   *
   * In a wellformed scanner (no or only correct usage of
   * yypushback(int) and a match-all fallback rule) this method
   * will only be called with things that "Can't Possibly Happen".
   * If this method is called, something is seriously wrong
   * (e.g. a JFlex bug producing a faulty scanner etc.).
   *
   * Usual syntax/scanner level error handling should be done
   * in error fallback rules.
   *
   * @param   errorCode  the code of the errormessage to display
   */
  private void zzScanError(int errorCode) {
    String message;
    try {
      message = ZZ_ERROR_MSG[errorCode];
    }
    catch (ArrayIndexOutOfBoundsException e) {
      message = ZZ_ERROR_MSG[ZZ_UNKNOWN_ERROR];
    }

    throw new Error(message);
  }


  /**
   * Pushes the specified amount of characters back into the input stream.
   *
   * They will be read again by then next call of the scanning method
   *
   * @param number  the number of characters to be read again.
   *                This number must not be greater than yylength()!
   */
  public void yypushback(int number)  {
    if ( number > yylength() )
      zzScanError(ZZ_PUSHBACK_2BIG);

    zzMarkedPos -= number;
  }


  /**
   * Contains user EOF-code, which will be executed exactly once,
   * when the end of file is reached
   */
  private void zzDoEOF() {
    if (!zzEOFDone) {
      zzEOFDone = true;
    
    }
  }


  /**
   * Resumes scanning until the next regular expression is matched,
   * the end of input is encountered or an I/O-Error occurs.
   *
   * @return      the next token
   * @exception   java.io.IOException  if any I/O-Error occurs
   */
  @Override
  public IElementType advance() throws java.io.IOException {
    int zzInput;
    int zzAction;

    // cached fields:
    int zzCurrentPosL;
    int zzMarkedPosL;
    int zzEndReadL = zzEndRead;
    CharSequence zzBufferL = zzBuffer;
    char[] zzBufferArrayL = zzBufferArray;
    char [] zzCMapL = ZZ_CMAP;

    int [] zzTransL = ZZ_TRANS;
    int [] zzRowMapL = ZZ_ROWMAP;
    int [] zzAttrL = ZZ_ATTRIBUTE;

    while (true) {
      zzMarkedPosL = zzMarkedPos;

      zzAction = -1;

      zzCurrentPosL = zzCurrentPos = zzStartRead = zzMarkedPosL;

      zzState = zzLexicalState;


      zzForAction: {
        while (true) {

          if (zzCurrentPosL < zzEndReadL)
            zzInput = zzBufferArrayL != null ? zzBufferArrayL[zzCurrentPosL++]:zzBufferL.charAt(zzCurrentPosL++);
          else if (zzAtEOF) {
            zzInput = YYEOF;
            break zzForAction;
          }
          else {
            // store back cached positions
            zzCurrentPos  = zzCurrentPosL;
            zzMarkedPos   = zzMarkedPosL;
            boolean eof = zzRefill();
            // get translated positions and possibly new buffer
            zzCurrentPosL  = zzCurrentPos;
            zzMarkedPosL   = zzMarkedPos;
            zzBufferL      = zzBuffer;
            zzEndReadL     = zzEndRead;
            if (eof) {
              zzInput = YYEOF;
              break zzForAction;
            }
            else {
              zzInput = zzBufferArrayL != null ? zzBufferArrayL[zzCurrentPosL++]:zzBufferL.charAt(zzCurrentPosL++);
            }
          }
          int zzNext = zzTransL[ zzRowMapL[zzState] + zzCMapL[zzInput] ];
          if (zzNext == -1) break zzForAction;
          zzState = zzNext;

          int zzAttributes = zzAttrL[zzState];
          if ( (zzAttributes & 1) == 1 ) {
            zzAction = zzState;
            zzMarkedPosL = zzCurrentPosL;
            if ( (zzAttributes & 8) == 8 ) break zzForAction;
          }

        }
      }

      // store back cached position
      zzMarkedPos = zzMarkedPosL;

      switch (zzAction < 0 ? zzAction : ZZ_ACTION[zzAction]) {
        case 12: 
          { return myTokenTypes.tagValueSharp();
          }
        case 21: break;
        case 16: 
          { yybegin(COMMENT_DATA); return myTokenTypes.tagValueGT();
          }
        case 22: break;
        case 3: 
          { return myTokenTypes.space();
          }
        case 23: break;
        case 6: 
          { return myTokenTypes.commentData();
          }
        case 24: break;
        case 7: 
          { if (checkAhead('<') || checkAhead('\"')) yybegin(COMMENT_DATA);
  else if (checkAhead('\u007b') ) yybegin(COMMENT_DATA); //lbrace -  there's some error in JLex when typing lbrace directly
  else yybegin(DOC_TAG_VALUE);

 return myTokenTypes.space();
          }
        case 25: break;
        case 4: 
          { if (checkAhead('@')){
    yybegin(INLINE_TAG_NAME);
    return myTokenTypes.inlineTagStart();
  }
  else{
    yybegin(COMMENT_DATA);
    return myTokenTypes.inlineTagStart();
  }
          }
        case 26: break;
        case 5: 
          { yybegin(COMMENT_DATA); return myTokenTypes.inlineTagEnd();
          }
        case 27: break;
        case 19: 
          { yybegin(COMMENT_DATA_START); return myTokenTypes.commentStart();
          }
        case 28: break;
        case 8: 
          { yybegin(DOC_TAG_VALUE); return myTokenTypes.space();
          }
        case 29: break;
        case 14: 
          { if (myJdk15Enabled) {yybegin(DOC_TAG_VALUE_IN_LTGT); return myTokenTypes.tagValueLT();} else { yybegin(COMMENT_DATA); return myTokenTypes.commentData(); }
          }
        case 30: break;
        case 10: 
          { return myTokenTypes.tagValueToken();
          }
        case 31: break;
        case 18: 
          { yybegin(TAG_DOC_SPACE); return myTokenTypes.tagName();
          }
        case 32: break;
        case 9: 
          { yybegin(COMMENT_DATA); return myTokenTypes.space();
          }
        case 33: break;
        case 20: 
          { yybegin(PARAM_TAG_SPACE); return myTokenTypes.tagName();
          }
        case 34: break;
        case 1: 
          { return myTokenTypes.badCharacter();
          }
        case 35: break;
        case 15: 
          { yybegin(DOC_TAG_VALUE); return myTokenTypes.tagValueRParen();
          }
        case 36: break;
        case 11: 
          { yybegin(DOC_TAG_VALUE_IN_PAREN); return myTokenTypes.tagValueLParen();
          }
        case 37: break;
        case 13: 
          { return myTokenTypes.tagValueComma();
          }
        case 38: break;
        case 2: 
          { yybegin(COMMENT_DATA); return myTokenTypes.commentData();
          }
        case 39: break;
        case 17: 
          { return myTokenTypes.commentEnd();
          }
        case 40: break;
        default:
          if (zzInput == YYEOF && zzStartRead == zzCurrentPos) {
            zzAtEOF = true;
            zzDoEOF();
            return null;
          }
          else {
            zzScanError(ZZ_NO_MATCH);
          }
      }
    }
  }


}
