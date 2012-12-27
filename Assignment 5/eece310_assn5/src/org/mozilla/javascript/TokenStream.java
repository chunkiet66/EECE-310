/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Rhino code, released
 * May 6, 1999.
 *
 * The Initial Developer of the Original Code is
 * Netscape Communications Corporation.
 * Portions created by the Initial Developer are Copyright (C) 1997-1999
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Roger Lawrence
 *   Mike McCabe
 *   Igor Bukanov
 *   Ethan Hugg
 *   Bob Jervis
 *   Terry Lucas
 *   Milen Nankov
 *   Steve Yegge
 *
 * Alternatively, the contents of this file may be used under the terms of
 * the GNU General Public License Version 2 or later (the "GPL"), in which
 * case the provisions of the GPL are applicable instead of those above. If
 * you wish to allow use of your version of this file only under the terms of
 * the GPL and not to allow others to use your version of this file under the
 * MPL, indicate your decision by deleting the provisions above and replacing
 * them with the notice and other provisions required by the GPL. If you do
 * not delete the provisions above, a recipient may use your version of this
 * file under either the MPL or the GPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.mozilla.javascript;

import java.io.*;

/**
 * This class implements the JavaScript scanner.
 *
 * It is based on the C source files jsscan.c and jsscan.h
 * in the jsref package.
 *
 * @see org.mozilla.javascript.Parser
 *
 * @author Mike McCabe
 * @author Brendan Eich
 */

public class TokenStream
{
    /*
     * For chars - because we need something out-of-range
     * to check.  (And checking EOF by exception is annoying.)
     * Note distinction from EOF token type!
     */
    private final static int
        EOF_CHAR = -1;

    private final static char BYTE_ORDER_MARK = '\uFEFF';

    //@ ensures (this.sourceString != null ==> this.sourceReader == null) && (this.sourceString == null ==> this.sourceReader != null) && (this.sourceEnd >= 0) && (this.sourceCursor == 0 && this.cursor == 0);
    TokenStream(Reader sourceReader, String sourceString,
                int lineno)
    {
        this.lineno = lineno;
        if (sourceReader != null) {
            this.sourceReader = sourceReader;
            this.sourceBuffer = new char[512];
            this.sourceEnd = 0;
        } else {
            this.sourceString = sourceString;
            this.sourceEnd = sourceString.length();
        }
        this.sourceCursor = this.cursor = 0;
    }

    /* This function uses the cached op, string and number fields in
     * TokenStream; if getToken has been called since the passed token
     * was scanned, the op or string printed may be incorrect.
     */
    String tokenToString(int token)
    {
        if (Token.printTrees) {
            String name = Token.name(token);

            switch (token) {
            case Token.STRING:
            case Token.REGEXP:
            case Token.NAME:
                return name + " `" + this.string + "'";

            case Token.NUMBER:
                return "NUMBER " + this.number;
            }

            return name;
        }
        return "";
    }
    
    /*@ requires Token.EOF == 0 && name != null;
        ensures (name.equals("else") ==> \result == Token.ELSE) && 
        		(name.equals("false") ==> \result == Token.FALSE) &&
        		(name.equals("function") ==> \result == Token.FUNCTION) &&
        		(name.equals("if") ==> \result == Token.IF) &&
        		(name.equals("return") ==> \result == Token.RETURN) &&
        		(name.equals("true") ==> \result == Token.TRUE) &&
        		(!(name.equals("else") || name.equals("false") || name.equals("function") || name.equals("if") || name.equals("return") || name.equals("true")) ==> \result == Token.EOF) &&
        		((\result == 0) || (\result == 113) || (\result == 44) || (\result == 109) || (\result == 112) || (\result == 4) || (\result == 45));
    @*/ 
    public static int stringToKeyword(String name)
    {
    	// The following assumes that Token.EOF == 0
        final int
            Id_else          = Token.ELSE,
            Id_false         = Token.FALSE,
            Id_function      = Token.FUNCTION,
            Id_if            = Token.IF,
            Id_return        = Token.RETURN,
            Id_true          = Token.TRUE;

        int id;
        String s = name;

        id = 0;
        if (s.equals("else")) {
        	id = Id_else;
        }
        else if (s.equals("false")) {
        	id = Id_false;
        }
        else if (s.equals("function")) {
        	id = Id_function;
        }
        else if (s.equals("if")) {
        	id = Id_if;
        }
        else if (s.equals("return")) {
        	id = Id_return;
        }
        else if (s.equals("true")) {
        	id = Id_true;
        }

        if (id == 0) { return Token.EOF; }
        return id;
    }

    //@ ensures \result == sourceString;
    final String getSourceString() { return sourceString; }
    
    //@ ensures \result == lineno;
    final int getLineno() { return lineno; }

    //@ ensures \result == string;
    public final String getString() { return string; }

    //@ ensures \result == (char)quoteChar;
    final char getQuoteChar() {
        return (char) quoteChar;
    }

    //@ ensures \result == number;
    public final double getNumber() { return number; }
    
    //@ ensures \result == isOctal;
    public final boolean isNumberOctal() { return isOctal; }

    //@ ensures \result == hitEOF;
    public final boolean eof() { return hitEOF; }

    //@ requires stringBuffer != null && (sourceString == null ==> (sourceBuffer != null && sourceReader != null));
    //@ ensures \result >= -1 && \result <= 162;
    //@ signals_only RuntimeException, IOException;
    public final int getToken() throws IOException
    {
        int c;

    retry:
        for (;;) {
            // Eat whitespace, possibly sensitive to newlines.
            for (;;) {
                c = getChar();
                if (c == EOF_CHAR) {
                    tokenBeg = cursor - 1;
                    tokenEnd = cursor;
                    return Token.EOF;
                } else if (c == '\n') {
                    dirtyLine = false;
                    tokenBeg = cursor - 1;
                    tokenEnd = cursor;
                    return Token.EOL;
                } else if (!isJSSpace(c)) {
                    if (c != '-') {
                        dirtyLine = true;
                    }
                    break;
                }
            }

            // Assume the token will be 1 char - fixed up below.
            tokenBeg = cursor - 1;
            tokenEnd = cursor;

            // identifier/keyword/instanceof?
            // watch out for starting with a <backslash>
            boolean identifierStart;
            boolean isUnicodeEscapeStart = false;
            if (c == '\\') {
                c = getChar();
                if (c == 'u') {
                    identifierStart = true;
                    isUnicodeEscapeStart = true;
                    stringBufferTop = 0;
                } else {
                    identifierStart = false;
                    ungetChar(c);
                    c = '\\';
                }
            } else {
                identifierStart = Character.isJavaIdentifierStart((char)c);
                if (identifierStart) {
                    stringBufferTop = 0;
                    addToString(c);
                }
            }

            if (identifierStart) {
                boolean containsEscape = isUnicodeEscapeStart;
                for (;;) {
                    if (isUnicodeEscapeStart) {
                        // strictly speaking we should probably push-back
                        // all the bad characters if the <backslash>uXXXX
                        // sequence is malformed. But since there isn't a
                        // correct context(is there?) for a bad Unicode
                        // escape sequence in an identifier, we can report
                        // an error here.
                        int escapeVal = 0;
                        for (int i = 0; i != 4; ++i) {
                            c = getChar();
                            escapeVal = Kit.xDigitToInt(c, escapeVal);
                            // Next check takes care about c < 0 and bad escape
                            if (escapeVal < 0) { break; }
                        }
                        if (escapeVal < 0) {
                            return Token.ERROR;
                        }
                        addToString(escapeVal);
                        isUnicodeEscapeStart = false;
                    } else {
                        c = getChar();
                        if (c == '\\') {
                            c = getChar();
                            if (c == 'u') {
                                isUnicodeEscapeStart = true;
                                containsEscape = true;
                            } else {
                                return Token.ERROR;
                            }
                        } else {
                            if (c == EOF_CHAR || c == BYTE_ORDER_MARK
                                || !Character.isJavaIdentifierPart((char)c))
                            {
                                break;
                            }
                            addToString(c);
                        }
                    }
                }
                ungetChar(c);

                String str = getStringFromBuffer();
                if (!containsEscape) {
                    // OPT we shouldn't have to make a string (object!) to
                    // check if it's a keyword.

                    // Return the corresponding token if it's a keyword
                    int result = stringToKeyword(str);
                    if (result != Token.EOF) { //i.e., str does not correspond to a keyword
                        return result;
                    }
                }
                this.string = (String)allStrings.intern(str);
                return Token.NAME;
            }

            // is it a number?
            if (isDigit(c) || (c == '.' && isDigit(peekChar()))) {
                isOctal = false;
                stringBufferTop = 0;
                int base = 10;
                
                if (c == '0') {
                    c = getChar();
                    if (c == 'x' || c == 'X') {
                        base = 16;
                        c = getChar();
                    } else if (isDigit(c)) {
                        base = 8;
                        isOctal = true;
                    } else {
                        addToString('0');
                    }
                }

                if (base == 16) {
                    while (0 <= Kit.xDigitToInt(c, 0)) {
                        addToString(c);
                        c = getChar();
                    }
                } else {
                    while ('0' <= c && c <= '9') {
                        /*
                         * We permit 08 and 09 as decimal numbers, which
                         * makes our behavior a superset of the ECMA
                         * numeric grammar.  We might not always be so
                         * permissive, so we warn about it.
                         */
                        if (base == 8 && c >= '8') {
                        	//Malformed octal (max digit for octal is 7) - revert to base 10
                            base = 10;
                        }
                        addToString(c);
                        c = getChar();
                    }
                }

                boolean isInteger = true;
                ungetChar(c);
                String numString = getStringFromBuffer();
                this.string = numString;

                double dval;
                int radix = base;
                // @ assume numString != null;
                dval = stringToNumber(numString, 0, radix);

                this.number = dval;
                return Token.NUMBER;
            }
            
            return operatorToken(c);
        }
    }
    
    //@ requires stringBuffer != null && (sourceString == null ==> (sourceBuffer != null && sourceReader != null));
    //@ ensures \result >= -1 && \result <= 162;
    //@ signals_only RuntimeException, IOException;
    private final int operatorToken(int c) throws IOException {
        switch (c) {
        case ';': return Token.SEMI;
        case '[': return Token.LB;
        case ']': return Token.RB;
        case '{': return Token.LC;
        case '}': return Token.RC;
        case '(': return Token.LP;
        case ')': return Token.RP;
        case ',': return Token.COMMA;
        case '?': return Token.HOOK;
        case ':':
            if (matchChar(':')) {
                return Token.COLONCOLON;
            } else {
                return Token.COLON;
            }
        case '.':
            if (matchChar('.')) {
                return Token.DOTDOT;
            } else if (matchChar('(')) {
                return Token.DOTQUERY;
            } else {
                return Token.DOT;
            }

        case '|':
            if (matchChar('|')) {
                return Token.OR;
            } else if (matchChar('=')) {
                return Token.ASSIGN_BITOR;
            } else {
                return Token.BITOR;
            }

        case '^':
            if (matchChar('=')) {
                return Token.ASSIGN_BITXOR;
            } else {
                return Token.BITXOR;
            }

        case '&':
            if (matchChar('&')) {
                return Token.AND;
            } else if (matchChar('=')) {
                return Token.ASSIGN_BITAND;
            } else {
                return Token.BITAND;
            }

        case '=':
            if (matchChar('=')) {
                if (matchChar('=')) {
                    return Token.SHEQ;
                } else {
                    return Token.EQ;
                }
            } else {
                return Token.ASSIGN;
            }

        case '!':
            if (matchChar('=')) {
                if (matchChar('=')) {
                    return Token.SHNE;
                } else {
                    return Token.NE;
                }
            } else {
                return Token.NOT;
            }

        case '<':
            if (matchChar('<')) {
                if (matchChar('=')) {
                    return Token.ASSIGN_LSH;
                } else {
                    return Token.LSH;
                }
            } else {
                if (matchChar('=')) {
                    return Token.LE;
                } else {
                    return Token.LT;
                }
            }

        case '>':
            if (matchChar('>')) {
                if (matchChar('>')) {
                    if (matchChar('=')) {
                        return Token.ASSIGN_URSH;
                    } else {
                        return Token.URSH;
                    }
                } else {
                    if (matchChar('=')) {
                        return Token.ASSIGN_RSH;
                    } else {
                        return Token.RSH;
                    }
                }
            } else {
                if (matchChar('=')) {
                    return Token.GE;
                } else {
                    return Token.GT;
                }
            }

        case '*':
            if (matchChar('=')) {
                return Token.ASSIGN_MUL;
            } else {
                return Token.MUL;
            }

        case '/':
            if (matchChar('=')) {
                return Token.ASSIGN_DIV;
            } else {
                return Token.DIV;
            }

        case '%':
            if (matchChar('=')) {
                return Token.ASSIGN_MOD;
            } else {
                return Token.MOD;
            }

        case '~':
            return Token.BITNOT;

        case '+':
            if (matchChar('=')) {
                return Token.ASSIGN_ADD;
            } else if (matchChar('+')) {
                return Token.INC;
            } else {
                return Token.ADD;
            }

        case '-':
            if (matchChar('=')) {
                c = Token.ASSIGN_SUB;
            } else if (matchChar('-')) {
                c = Token.DEC;
            } else {
                c = Token.SUB;
            }
            return c;

        default:
            System.out.println("Illegal character");
            return Token.ERROR;
        }
	}

    //@ ensures (c <= 'Z' ==> \result == ('A' <= c)) && (c > 'Z' ==> \result == ('a' <= c && c <= 'z'));
    private static boolean isAlpha(int c)
    {
        // Use 'Z' < 'a'
        if (c <= 'Z') {
            return 'A' <= c;
        } else {
            return 'a' <= c && c <= 'z';
        }
    }

    //@ ensures (('0' <= c && c <= '9') ==> \result == true) && (!('0' <= c && c <= '9') ==> \result == false);
    static boolean isDigit(int c)
    {
        return '0' <= c && c <= '9';
    }

    /* As defined in ECMA.  jsscan.c uses C isspace() (which allows
     * \v, I think.)  note that code in getChar() implicitly accepts
     * '\r' == \u000D as well.
     */
    //@ ensures c <= 127 ==> \result == (c == 0x20 || c == 0x9 || c == 0xC || c == 0xB);
    public static boolean isJSSpace(int c)
    {
        if (c <= 127) {
            return c == 0x20 || c == 0x9 || c == 0xC || c == 0xB;
        } else {
            return c == 0xA0 || c == BYTE_ORDER_MARK
                || Character.getType((char)c) == Character.SPACE_SEPARATOR;
        }
    }

    private static boolean isJSFormatChar(int c)
    {
        return c > 127 && Character.getType((char)c) == Character.FORMAT;
    }

    //@ requires stringBuffer != null && stringBufferTop >= 0;
    //@ ensures tokenEnd == cursor && \result != null;
    //@ signals_only RuntimeException;
    private String getStringFromBuffer()
    {
        tokenEnd = cursor;
        return new String(stringBuffer, 0, stringBufferTop);
    }

    //@ requires stringBuffer != null;
    //@ ensures stringBufferTop == \old(stringBufferTop) + 1;
    private void addToString(int c)
    {
        int N = stringBufferTop;
        if (N == stringBuffer.length) {
            char[] tmp = new char[stringBuffer.length * 2];
            //@ assume stringBuffer != null && tmp != null && N < stringBuffer.length && N < tmp.length;
            System.arraycopy(stringBuffer, 0, tmp, 0, N);
            stringBuffer = tmp;
        }
        stringBuffer[N] = (char)c;
        stringBufferTop = N + 1;
    }
    
    //@ requires ungetBuffer != null;
    //@ ensures \result == (ungetCursor == 0 || ungetBuffer[ungetCursor - 1] != '\n');
    private boolean canUngetChar() {
        return ungetCursor == 0 || ungetBuffer[ungetCursor - 1] != '\n';
    }

    //@ requires ungetBuffer != null;
    //@ signals_only RuntimeException;
    private void ungetChar(int c)
    {
        // can not unread past across line boundary
        if (ungetCursor != 0 && ungetBuffer[ungetCursor - 1] == '\n')
            Kit.codeBug();
        ungetBuffer[ungetCursor++] = c;
        cursor--;
    }

    //@ requires (ungetCursor != 0 ==> ungetBuffer != null) && (sourceString == null ==> (sourceBuffer != null && sourceReader != null));
    //@ signals_only RuntimeException, IOException;
    private boolean matchChar(int test) throws IOException
    {
        int c = getCharIgnoreLineEnd();
        if (c == test) {
            tokenEnd = cursor;
            return true;
        } else {
            ungetCharIgnoreLineEnd(c);
            return false;
        }
    }

    //@ requires (ungetCursor != 0 ==> ungetBuffer != null) && (sourceString == null ==> (sourceBuffer != null && sourceReader != null));
    //@ signals_only RuntimeException, IOException;
    private int peekChar() throws IOException
    {
        int c = getChar();
        ungetChar(c);
        return c;
    }

    //@ requires (ungetCursor != 0 ==> ungetBuffer != null) && (sourceString == null ==> (sourceBuffer != null && sourceReader != null));
    //@ ensures (\old(ungetCursor) != 0 ==> cursor == \old(cursor) + 1);
    //@ signals_only RuntimeException, IOException;
    private int getChar() throws IOException
    {
        if (ungetCursor != 0) {
            cursor++;
            return ungetBuffer[--ungetCursor];
        }

        for(;;) {
            int c;
            if (sourceString != null) {
                if (sourceCursor == sourceEnd) {
                    hitEOF = true;
                    return EOF_CHAR;
                }
                cursor++;
                c = sourceString.charAt(sourceCursor++);
            } else {
                if (sourceCursor == sourceEnd) {
                    if (!fillSourceBuffer()) {
                        hitEOF = true;
                        return EOF_CHAR;
                    }
                }
                cursor++;
                c = sourceBuffer[sourceCursor++];
            }

            if (lineEndChar >= 0) {
                if (lineEndChar == '\r' && c == '\n') {
                    lineEndChar = '\n';
                    continue;
                }
                lineEndChar = -1;
                lineStart = sourceCursor - 1;
                lineno++;
            }

            if (c <= 127) {
                if (c == '\n' || c == '\r') {
                    lineEndChar = c;
                    c = '\n';
                }
            } else {
                if (c == BYTE_ORDER_MARK) return c; // BOM is considered whitespace
                if (isJSFormatChar(c)) {
                    continue;
                }
                if (isJSLineTerminator(c)) {
                    lineEndChar = c;
                    c = '\n';
                }
            }
            return c;
        }
    }

    //@ requires (ungetCursor != 0 ==> ungetBuffer != null) && (sourceString == null ==> (sourceBuffer != null && sourceReader != null));
    //@ ensures (\old(ungetCursor) != 0 ==> cursor == \old(cursor) + 1);
    //@ signals_only RuntimeException, IOException;
    private int getCharIgnoreLineEnd() throws IOException
    {
        if (ungetCursor != 0) {
            cursor++;
            return ungetBuffer[--ungetCursor];
        }

        for(;;) {
            int c;
            if (sourceString != null) {
                if (sourceCursor == sourceEnd) {
                    hitEOF = true;
                    return EOF_CHAR;
                }
                cursor++;
                c = sourceString.charAt(sourceCursor++);
            } else {
                if (sourceCursor == sourceEnd) {
                    if (!fillSourceBuffer()) {
                        hitEOF = true;
                        return EOF_CHAR;
                    }
                }
                cursor++;
                c = sourceBuffer[sourceCursor++];
            }

            if (c <= 127) {
                if (c == '\n' || c == '\r') {
                    lineEndChar = c;
                    c = '\n';
                }
            } else {
                if (c == BYTE_ORDER_MARK) return c; // BOM is considered whitespace
                if (isJSFormatChar(c)) {
                    continue;
                }
                if (isJSLineTerminator(c)) {
                    lineEndChar = c;
                    c = '\n';
                }
            }
            return c;
        }
    }

    //@ requires ungetBuffer != null;
    //@ assignable ungetBuffer[*], ungetCursor, cursor;
    //@ ensures cursor == \old(cursor) - 1 && ungetCursor == \old(ungetCursor) + 1;
    private void ungetCharIgnoreLineEnd(int c)
    {
        ungetBuffer[ungetCursor++] = c;
        cursor--;
    }

    //@ requires (ungetCursor != 0 ==> ungetBuffer != null) && (sourceString == null ==> (sourceBuffer != null && sourceReader != null));
    //@ ensures tokenEnd == cursor;
    //@ signals_only RuntimeException, IOException;
    private void skipLine() throws IOException
    {
        // skip to end of line
        int c;
        while ((c = getChar()) != EOF_CHAR && c != '\n') { }
        ungetChar(c);
        tokenEnd = cursor;
    }

    /**
     * Returns the offset into the current line.
     */
    //@ requires (lineEndChar >= 0 ==> sourceCursor > lineStart) && (lineEndChar < 0 ==> sourceCursor >= lineStart);
    //@ assignable \nothing;
    //@ ensures \result >= 0;
    final int getOffset()
    {
        int n = sourceCursor - lineStart;
        if (lineEndChar >= 0) { --n; }
        return n;
    }

    //@ requires (sourceString == null ==> (sourceBuffer != null && sourceReader != null));
    //@ signals_only RuntimeException;
    final String getLine()
    {
        if (sourceString != null) {
            // String case
            int lineEnd = sourceCursor;
            if (lineEndChar >= 0) {
                --lineEnd;
            } else {
                for (; lineEnd != sourceEnd; ++lineEnd) {
                    int c = sourceString.charAt(lineEnd);
                    if (isJSLineTerminator(c)) {
                        break;
                    }
                }
            }
            return sourceString.substring(lineStart, lineEnd);
        } else {
            // Reader case
            int lineLength = sourceCursor - lineStart;
            if (lineEndChar >= 0) {
                --lineLength;
            } else {
                // Read until the end of line
                for (;; ++lineLength) {
                    int i = lineStart + lineLength;
                    if (i == sourceEnd) {
                        try {
                            if (!fillSourceBuffer()) { break; }
                        } catch (IOException ioe) {
                            // ignore it, we're already displaying an error...
                            break;
                        }
                        // i recalculuation as fillSourceBuffer can move saved
                        // line buffer and change lineStart
                        i = lineStart + lineLength;
                    }
                    int c = sourceBuffer[i];
                    if (isJSLineTerminator(c)) {
                        break;
                    }
                }
            }
            return new String(sourceBuffer, lineStart, lineLength);
        }
    }

    //@ requires sourceBuffer != null && sourceReader != null;
    //@ assignable sourceBuffer[*], sourceEnd, sourceCursor, lineStart, sourceReader, \not_specified;
    //@ signals_only RuntimeException, IndexOutOfBoundsException, ArrayStoreException, NullPointerException, IOException; 
    private boolean fillSourceBuffer() throws IOException
    {
        if (sourceString != null) Kit.codeBug();
        if (sourceEnd == sourceBuffer.length) {
            if (lineStart != 0) {
            	//@ assume sourceBuffer != null && lineStart > 0 && sourceEnd < sourceBuffer.length;
                System.arraycopy(sourceBuffer, lineStart, sourceBuffer, 0,
                                 sourceEnd - lineStart);
                sourceEnd -= lineStart;
                sourceCursor -= lineStart;
                lineStart = 0;
            } else {
                char[] tmp = new char[sourceBuffer.length * 2];
                //@ assume sourceBuffer != null && tmp != null && sourceEnd < sourceBuffer.length && sourceEnd < tmp.length;
                System.arraycopy(sourceBuffer, 0, tmp, 0, sourceEnd);
                sourceBuffer = tmp;
            }
        }
        int n = sourceReader.read(sourceBuffer, sourceEnd,
                                  sourceBuffer.length - sourceEnd);
        if (n < 0) {
            return false;
        }
        sourceEnd += n;
        return true;
    }

    /**
     * Return the current position of the scanner cursor.
     */
    //@ ensures \result == cursor;
    public int getCursor() {
        return cursor;
    }

    /**
     * Return the absolute source offset of the last scanned token.
     */
    //@ ensures \result == tokenBeg;
    public int getTokenBeg() {
        return tokenBeg;
    }

    /**
     * Return the absolute source end-offset of the last scanned token.
     */
    //@ ensures \result == tokenEnd;
    public int getTokenEnd() {
        return tokenEnd;
    }

    /**
     * Return tokenEnd - tokenBeg
     */
    //@ requires tokenEnd > tokenBeg;
    //@ assignable \nothing;
    //@ ensures \result > 0 && \result == (tokenEnd - tokenBeg);
    public int getTokenLength() {
        return tokenEnd - tokenBeg;
    }
     
	 //@ requires (radix == 10 || radix == 8 || radix == 16) && start >= 0 && s != null;
	 static double stringToNumber(String s, int start, int radix) {
	     char digitMax = '9';
	     char lowerCaseBound = 'a';
	     char upperCaseBound = 'A';
	     int len = s.length();
	     if (radix == 8) {
	    	 digitMax = '7';
	     }
	     if (radix == 16) {
	    	 lowerCaseBound = 'f';
	    	 upperCaseBound = 'F';
	     }
	     int end;
	     double sum = 0.0;
	     for (end=start; end < len; end++) {
			 char c = s.charAt(end);
			 int newDigit;
			 if ('0' <= c && c <= digitMax)
				 newDigit = c - '0';
			 else if ('a' <= c && c < lowerCaseBound)
				 newDigit = c - 'a' + 10;
			 else if ('A' <= c && c < upperCaseBound)
				 newDigit = c - 'A' + 10;
			 else
			     break;
			 sum = sum*radix + newDigit;
	     }
	     if (start == end) {
	         return NaN;
	     }
	     return sum;
	 }
     
     public static boolean isJSLineTerminator(int c)
     {
         // Optimization for faster check for eol character:
         // they do not have 0xDFD0 bits set
         if ((c & 0xDFD0) != 0) {
             return false;
         }
         return c == '\n' || c == '\r' || c == 0x2028 || c == 0x2029;
     }

    // stuff other than whitespace since start of line
    private /*@ spec_public @*/ boolean dirtyLine;

    // Set this to an initial non-null value so that the Parser has
    // something to retrieve even if an error has occurred and no
    // string is found.  Fosters one class of error, but saves lots of
    // code.
    private /*@ spec_public @*/ String string = "";
    private /*@ spec_public @*/ double number;
    private /*@ spec_public @*/ boolean isOctal;

    // delimiter for last string literal scanned
    private /*@ spec_public @*/ int quoteChar;

    private /*@ spec_public @*/ char[] stringBuffer = new char[128];
    private /*@ spec_public @*/ int stringBufferTop;
    private /*@ spec_public @*/ ObjToIntMap allStrings = new ObjToIntMap(50);

    // Room to backtrace from to < on failed match of the last - in <!--
    private /*@ spec_public @*/ final int[] ungetBuffer = new int[3];
    private /*@ spec_public @*/ int ungetCursor;

    private /*@ spec_public @*/ boolean hitEOF = false;

    private /*@ spec_public @*/ int lineStart = 0;
    private /*@ spec_public @*/ int lineEndChar = -1; //the char at the current cursor
    int lineno;

    private /*@ spec_public @*/ String sourceString;
    private /*@ spec_public @*/ Reader sourceReader;
    private /*@ spec_public @*/ char[] sourceBuffer;
    private /*@ spec_public @*/ int sourceEnd;

    // sourceCursor is an index into a small buffer that keeps a
    // sliding window of the source stream.
    int sourceCursor;

    // cursor is a monotonically increasing index into the original
    // source stream, tracking exactly how far scanning has progressed.
    // Its value is the index of the next character to be scanned.
    int cursor;

    // Record start and end positions of last scanned token.
    int tokenBeg;
    int tokenEnd;
    
    public static final double NaN = Double.longBitsToDouble(0x7ff8000000000000L);
}
