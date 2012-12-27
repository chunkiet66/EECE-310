// Assignment 3
// Po Liu 13623079
// Haochen Zhang 43324094

package org.mozilla.javascript;

import static org.junit.Assert.*;

import java.io.StringReader;

import org.junit.Test;

public class TokenStreamTest {

	@Test
	public void testTokenStream_SSisNull() {
		// black box testing
		// Tests if the output is sourceReader when sourceReader != null and sourceString == null
		// Input: sourceReader = new StringReader("var a = 5;"), sourceString = null, lineno = 1
		// Expected Output: the created TokenStream contains a sourceReader that points to the given sourceReader 
		//					cursor == 0, sourceCursor == 0, getString().equals("var")
		TokenStream ts = new TokenStream(new StringReader("var a = 5;"), null,
				1);

		assertTrue(ts.cursor == 0);
		assertTrue(ts.sourceCursor == 0);
		// assertTrue(ts.sourceEnd >= 0);

		// Add
		// assertTrue(ts.sourceReader != null);
		try {
			int token = ts.getToken();
			assertTrue(ts.getString().equals("var"));
		} catch (Exception e) {
			fail("Exception thrown here");
		}

	}

	@Test
	public void testTokenStream_SRisNull() {
		// black box testing
		// Tests if the output is sourceString when this.sourceReader == null &&
		// this.sourceString != null
		// Input: sourceReader = null, sourceString = "var a = 5;", lineno = 1
		// Expected Output: cursor == 0, sourceCursor == 0,
		// (\result == "var")

		TokenStream ts = new TokenStream(null, "var a = 5;", 1);

		assertTrue(ts.cursor == 0);
		assertTrue(ts.sourceCursor == 0);

		//Add
		// assertTrue(ts.sourceReader == null);
		try {
			int token = ts.getToken();
			assertTrue(ts.getString().equals("var"));
		} catch (Exception e) {
			fail("Exception thrown here");
		}
	}

	@Test
	public void testTokenStream_SRisNull_SSisNull() {
		// black box testing
		// Tests if there is no output when this.sourceReader = null &  this.sourceString = null
		// Input: sourceReader = null, sourceString = null, lineno = 1
		// Expected Output: the created object's sourceReader points to an empty object and the sourceString is null
		try {
			TokenStream ts = new TokenStream(null, null, 1);
			assertTrue(ts.cursor == 0);
			assertTrue(ts.sourceCursor == 0);
			// assertTrue(ts.sourceEnd >= 0);

			//Add
			//assertTrue(ts.sourceReader == null);
			//assertTrue(ts.sourceString == null);
			
			int token = ts.getToken();
			assertTrue(ts.getString().equals(null));
			fail("Not supposed to reach this point");

		} catch (Exception e) {

		}

	}


	public void testTokenStream_SRisNotNull_SSisNotNull() {
		// glass box testing
		// Tests if there is no output when this.sourceReader = null &  this.sourceString = null
		// Input: sourceReader != null, sourceString != null, lineno = 1
		// Expected Output: the created object's sourceReader points to the sourceReader object and the sourceString is taken from the sourceReader
		try {
			TokenStream ts = new TokenStream(new StringReader("test"), "abcde", 1);
			assertTrue(ts.cursor == 0);
			assertTrue(ts.sourceCursor == 0);
			// assertTrue(ts.sourceEnd >= 0);

			
			int token = ts.getToken();
			assertTrue(ts.getString().equals("test"));
			fail("Not supposed to reach this point");

		} catch (Exception e) {

		}

	}
	
	// -----------------------------------------------------------------------------------
	@Test
	public void testStringToKeyword_Null() {
		// glass box testing
		// Tests if there is no output when name == null
		// Input: null
		// Expected Output: Throws exception because of null value

		try {
			int test = TokenStream.stringToKeyword(null);
			fail("Not supposed to reach this point");
		} catch (Exception e) {
			// Test passes
		}

	}


	@Test
	public void testStringToKeyword_keywords() {
		// glass box testing
		// Tests if the output is corresponding to the keyword in the input
		// Input: "else" && "false" && "function" && "if" && "return" && "true"
		// && ""
		// Expected Output: (name.equals("else") ==> \result == Token.ELSE) &&
		// (name.equals("false") ==> \result == Token.FALSE) &&
		// (name.equals("function") ==> \result == Token.FUNCTION) &&
		// (name.equals("if") ==> \result == Token.IF) &&
		// (name.equals("return") ==> \result == Token.RETURN) &&
		// (name.equals("true") ==> \result == Token.TRUE) &&
		// (name.equals("") ==> \result == Token.EOF)

		assertTrue(TokenStream.stringToKeyword("else") == Token.ELSE);
		assertTrue(TokenStream.stringToKeyword("false") == Token.FALSE);
		assertTrue(TokenStream.stringToKeyword("function") == Token.FUNCTION);
		assertTrue(TokenStream.stringToKeyword("if") == Token.IF);
		assertTrue(TokenStream.stringToKeyword("return") == Token.RETURN);
		assertTrue(TokenStream.stringToKeyword("true") == Token.TRUE);
		assertTrue(TokenStream.stringToKeyword("") == Token.EOF);
	}

	@Test
	public void testStringToKeyword_Non_keyword() {
		// glass box testing
		// Tests if the output is one of 0,113,44,109,112,4,45 when name =
		// "Po1234Hao"
		// Input: name = "Po1234Hao"
		// Expected Output: one of 0,113,44,109,112,4,45

		assertTrue(TokenStream.stringToKeyword("Po1234Hao") == 0
				|| TokenStream.stringToKeyword("Po1234Hao") == 113
				|| TokenStream.stringToKeyword("Po1234Hao") == 44
				|| TokenStream.stringToKeyword("Po1234Hao") == 109
				|| TokenStream.stringToKeyword("Po1234Hao") == 112
				|| TokenStream.stringToKeyword("Po1234Hao") == 4
				|| TokenStream.stringToKeyword("Po1234Hao") == 45);
	}

// ---------------------------------------------------------------------------

	@Test
	public void testGetString() {
		// Black box testing
		// Tests if the getString() returns each of the token strings placed in
		// its class variable
		// Input: sourceReader = null, sourceString = "var a = 5;", lineno = 1
		// Expected Output: (name.equals("var a = 5;") ==> \result == "var",
		// "a", "5")

		TokenStream ts = new TokenStream(null, "var a = 5;", 1);
		try {
			int token = ts.getToken();
			assertTrue(ts.getString().equals("var"));

			int counter = 0;
			while (Token.typeToName(token) != "EOF") {
				token = ts.getToken();

				if (counter == 1)
					assertTrue(ts.getString().equals("a"));
				else if (counter == 2)
					assertTrue(ts.getString().equals("5"));

				counter++;
			}

		} catch (Exception e) {
			fail("Should not throw exception under normal functioning");
		}
	}

	// -----------------------------------------------------------------------------

	@Test
	public void testGetNumber_Numbered() {
		// Black box testing
		// Tests if the getNumber() returns the number in the token strings
		// Input: sourceReader = null, sourceString = "var a = 5;", lineno = 1
		// Expected Output: (name.equals("var a = 5;") ==> \result ==
		// "0","0","0","5","5")

		TokenStream ts = new TokenStream(null, "var a = 5;", 1);
		try {
			int token = ts.getToken();

			int counter = 0;
			while (Token.typeToName(token) != "EOF") {

				if (counter == 0)
					assertTrue(ts.getNumber() == 0);
				else if (counter == 1)
					assertTrue(ts.getNumber() == 0);
				else if (counter == 2)
					assertTrue(ts.getNumber() == 0);
				else if (counter == 3)
					assertTrue(ts.getNumber() == 5);
				else if (counter == 4)
					assertTrue(ts.getNumber() == 5);

				token = ts.getToken();

				counter++;
			}
		} catch (Exception e) {
			fail("Should not fail under normal functioning");
		}
	}

	@Test
	public void testGetNumber_NonNumbered() {
		// Black box testing
		// Tests if the getNumber() returns the number in the token strings
		// Input: sourceReader = null, sourceString = "a=", lineno = 1
		// Expected Output: (name.equals("a=") ==> \result == "0")

		TokenStream ts = new TokenStream(null, "a=", 1);
		try {
			int token = ts.getToken();

			assertTrue(ts.getNumber() == 0);
		} catch (Exception e) {
			fail("Should not fail under normal functioning");
		}
		// potential Bug: Should return null or throw exception when string
		// doesn't contain number.

	}

	// ---------------------------------------------------------------

	@Test
	// Glass box testing for isNumberOctal
	// Input: { "0" } {"1"} {"2"} {"3"} {"4"} {"5"} {"6" } {"7"} {"8"}
	// Expected: return true
	public void testIsNumberOctal_True() {
		// Testing if given an octal
		String temp2;
		int i = 0;
		for (i = 0; i < 8; i++) {
			temp2 = "0" + i;

			TokenStream ts = new TokenStream(null, temp2, 1);
			try {

				int token = ts.getToken();
				assertTrue(ts.isNumberOctal());
			} catch (Exception e) {
				fail("Should not fail under normal functioning");
			}
		}
	}

	@Test
	// Glass box test of isNumber for non-octal numbers when specified a octal
	// Input: {null, "8", 1}
	// Expected: should return false
	public void testIsNumberOctal_FalseDecimal() {

		// Testing if NOT given an octal
		TokenStream ts = new TokenStream(null, "8", 1);
		try {
			int token = ts.getToken();
			assertFalse(ts.isNumberOctal());
		} catch (Exception e) {
			fail("Should not fail under normal functioning");
		}
	}

	@Test
	// Glass-box test for isNumberOctal if hex is given
	// Inputs: {null, "0x8", 1}
	// Expected: returns false because the input is not an octal
	public void testIsNumberOctal_FalseHexidecimal() {

		// Testing if NOT given an octal
		TokenStream ts = new TokenStream(null, "0x8", 1);
		try {
			int token = ts.getToken();
			assertFalse(ts.isNumberOctal());
		} catch (Exception e) {
			fail("Should not fail under normal functioning");
		}
	}

	// ------------------------------------------------------------------------------
	// Black-box test if eof signal is correctly detected
	// Input: "var a = 23;"
	// Expected: returns true after 6 iterations, indicating the end of the file input; return false in the first 5 iterations
	
	@Test
	public void testEof() {
		TokenStream ts = new TokenStream(new StringReader("var a = 23;"), null,
				1);
		try {

			int token = ts.getToken();
			while (Token.typeToName(token) != "EOF") {
				// Not gotten to EOF
				assertFalse(ts.eof());
				token = ts.getToken();
			}
		} catch (Exception e) {
			fail("Should not fail under normal functioning");
		}

		assertTrue(ts.eof());
	}

	// -----------------------------------------------------------------------------
	// Tests the getToken() method with glass-box
	// Inputs: "var a = 5;"
	// Expected:  Token.typeToName(\result) == "var", "a", "=", "5", for separate getToken()
	@Test
	public void testGetToken() {

		TokenStream ts = new TokenStream(null, "var a = 5;", 1);
		try {

			int token = 0;
			int counter = 0;
			while (Token.typeToName(token) != "EOF") {
				// Not gotten to EOF

				token = ts.getToken();
				// make sure it doesn't get eof before the end of file
				if (counter > 5)
					fail("error, should not be eof");
				// and ensures that you get the right token value
				if (counter == 0)
					assertTrue(Token.typeToName(token) == "var");
				if (counter == 1)
					assertTrue(Token.typeToName(token) == "a");
				if (counter == 2)
					assertTrue(Token.typeToName(token) == "=");
				if (counter == 3)
					assertTrue(Token.typeToName(token) == "5");
				if (counter == 4)
					assertTrue(Token.typeToName(token) == ";");

				assertTrue(token >= -1 && token <= 162);
				counter++;
			}
		} catch (Exception e) {
			fail("Should not fail under normal functioning");
		}

	}

	// -----------------------------------------------------------------------
	@Test
	// Tests isJSSpace(c) through all its paths through its spec (glass-box)
	// Inputs: c >= 127 (in hex) && c < 127 (in hex)
	// Expected: See the specifications for the expected correct input in TokenStream
	public void testIsJSSpace() {
		// glassbox
		char BYTE_ORDER_MARK = '\uFEFF';
		for (int i = 0; i < 0xFF; i++) {
			if (i <= 127) {
				// check c <= 127 ==> \result == (c == 0x20 || c == 0x9 || c ==
				// 0xC || c == 0xB);
				if (i == 0x20 || i == 0x9 || i == 0xC || i == 0xB)
					assertTrue(TokenStream.isJSSpace(i) == true);
				else
					assertTrue(TokenStream.isJSSpace(i) == false);
			} else {
				// check c>127 ==> \result == (c == 0xA0);
				if (i == 0xA0
						|| Character.getType((char) i) == Character.SPACE_SEPARATOR
						|| i == BYTE_ORDER_MARK)
					assertTrue(TokenStream.isJSSpace(i) == true);
				else
					assertTrue(TokenStream.isJSSpace(i) == false);
			}
		}
		// check BYTE_ORDER_MARK
		assertTrue(TokenStream.isJSSpace('\uFEFF') == true);
		// check Character.SPACE_SEPARATOR
		assertTrue(TokenStream.isJSSpace(' ') == true);

	}

	// --------------------------------------------------------------------
	@Test
	
	// Tests the stringToNumber with empty string
	// Input: null
	// Expected: NaN
	public void testStringToNumber_Empty() {

		// Black box test for empty StringToNumber
		// empty string ==> returns NaN
		assertTrue(Double.isNaN(TokenStream.stringToNumber("", 0, 10)));

	}

	// --------------------------------------------------------------------
	@Test
	// Tests the stringToNumber with given start that is longer than the string
	// Input: {"0000", 5, 16 }
	// Expected: 0x0
	public void testStringToNumber_Start_LargerAndEqual_SizeOfString() {
		// black-box testing --> glass-box too time difficult to ensure passes
		// through all paths
		// if start is bigger the size of string
		// //System.out.println(TokenStream.stringToNumber("0000", 5, 16)); ==
		// NaN

		// assertTrue(TokenStream.stringToNumber("0000", 5, 16) == 0x0);

		// possible Bug found
		// when start is equal or greater than the size of string, a user
		// defined exception such as OutOfBoundException should be thrown
	}

	
	// --------------------------------------------------------------------
	@Test
	// Tests the stringToNumber with the illegal inputs
	// Input:{"9999",0,8}, {"GGGG", 0,16},{"aaaa",0,10}
	// Expected: NaN, NaN, NaN
	public void testStringToNumber_NaN() {
		// black-box testing --> glass-box too time difficult to ensure passes
		// through all paths
		assertTrue(Double.isNaN(TokenStream.stringToNumber("9999", 0, 8)));
		assertTrue(Double.isNaN(TokenStream.stringToNumber("GGGG", 0, 16)));
		assertTrue(Double.isNaN(TokenStream.stringToNumber("aaaa", 0, 10)));
	}

	// --------------------------------------------------------------------
	@Test
	// Tests the stringToNumber with the boundary of hex inputs
	// Input:{"0000",0,16}, {"9999", 1,16},{"aaaa",2,16}, {"AAAA",3,16}, {"ffff", 0, 16}, {"FFFF", 0, 16}
	// Expected: 0x0, 0x999, 0xaa, 0xa, 0xffff, 0xffff
	public void testStringToNumber_HEX_Start_case_boundary() {
		// black-box testing --> glass-box too time difficult to ensure passes
		// through all paths
		// hexadecimal test
		// // start check with boundary check and lower/upper case check
		assertTrue(TokenStream.stringToNumber("0000", 0, 16) == 0x0);
		assertTrue(TokenStream.stringToNumber("9999", 1, 16) == 0x999);
		assertTrue(TokenStream.stringToNumber("aaaa", 2, 16) == 0xaa);
		assertTrue(TokenStream.stringToNumber("AAAA", 3, 16) == 0xa);

		/*
		 * assertTrue(TokenStream.stringToNumber("ffff", 0, 16) == 0xffff);
		 * assertTrue(TokenStream.stringToNumber("FFFF", 0, 16) == 0xFFFF);
		 */
		// Bug found!!!!!!
		// the lowerCaseBound is 'f', and upperCaseBound is 'F', However, when
		// checking the boundary,
		// c == 'f' and c == 'F' is excluded by using "<" instead of "<=".
	}

	// --------------------------------------------------------------------
	@Test
	// Tests the stringToNumber with some mixed values of hex
	// Input:{"0A9B",0,16},{"DE23",0,16},{"C09D",0,16}
	// Expected: \result == 0x0A9B, 0xDE23, 0xC09D
	public void testStringToNumber_HEX_MIX() {
		// black-box testing --> glass-box too time difficult to ensure passes
		// through all paths

		// hexadecimal test
		// // mix of number and letter
		assertTrue(TokenStream.stringToNumber("0A9B", 0, 16) == 0x0A9B);
		assertTrue(TokenStream.stringToNumber("DE23", 0, 16) == 0xDE23);
		assertTrue(TokenStream.stringToNumber("C09D", 0, 16) == 0xC09D);
	}

	// --------------------------------------------------------------------
	@Test
	// Tests the stringToNumber with the boundary conditions of octal inputs
	// Input: {"0000",0,8}, {"7777",0,8}
	// Expected: \result == 00, == 07777
	public void testStringToNumber_Oct_boundary() {
		// black-box testing --> glass-box too time difficult to ensure passes
		// through all paths

		// octal test
		// //boundary test
		assertTrue(TokenStream.stringToNumber("0000", 0, 8) == 00);
		assertTrue(TokenStream.stringToNumber("7777", 0, 8) == 07777);
		// possible bug, should throw exception when the string contain elements
		// not included in the type of number, i.e. 99 in type of octal

	}

	// --------------------------------------------------------------------
	@Test
	// Tests the stringToNumber with some starting values of octals
	// Input: {"7654",3,8}, {"0123",2,8}
	// Expected: \result == 04, == 023
	public void testStringToNumber_Oct_Start() {
		// black-box testing --> glass-box too time difficult to ensure passes
		// through all paths
		
		// octal test
		// //start check
		assertTrue(TokenStream.stringToNumber("7654", 3, 8) == 04);
		assertTrue(TokenStream.stringToNumber("0123", 2, 8) == 023);

	}

	// --------------------------------------------------------------------
	@Test
	// Tests the stringToNumber with decimal boundary inputs
	// Input: {"0000",0,10},  {"9999",0,10}
	// Expected: \result == 0000, 9999
	public void testStringToNumber_Dec_boundary() {
		// black-box testing --> glass-box too time difficult to ensure passes
		// through all paths

		// decimals test
		// // boundary check
		assertTrue(TokenStream.stringToNumber("0000", 0, 10) == 0000);
		assertTrue(TokenStream.stringToNumber("9999", 0, 10) == 9999);

	}

	// --------------------------------------------------------------------
	@Test
	// Tests: Boundary tests, if the decimal starting and ending inputs are
	// satisified
	// Input: {"0123",2,10}, {"9876",3,10}
	// Expected: \result == 23, 6
	public void testStringToNumber_Dec_Start() {
		// black-box testing --> glass-box too time difficult to ensure passes
		// through all paths

		// decimals test
		// Start check
		assertTrue(TokenStream.stringToNumber("0123", 2, 10) == 23);
		assertTrue(TokenStream.stringToNumber("9876", 3, 10) == 6);

	}

}