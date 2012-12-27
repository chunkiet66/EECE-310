package org.mozilla.javascript;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.mozilla.javascript.ast.*;
import java.util.*;

public class TestIterator {
	
	public static void main(String[] args) {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("Enter JavaScript File To Parse: ");
			System.out.flush();
			String js_file = br.readLine();
			
			//System.out.println("Test 2");
			
			//Parse the file
			Parser p = new Parser();
			AstRoot root = p.parse(new FileReader(js_file), "", 1);
			
			//Retrieve the AST iterator (note that this is different from the Node iterator(),
			//which only iterates over a node's children. This iterator iterates over all
			//nodes in the AST)
			int numNodes = 0;
			Iterator it = root.astIterator_depth();
			
			while (it.hasNext()) {
				AstNode an = (AstNode)it.next();
				String className = an.getClass().toString().substring(an.getClass().toString().lastIndexOf(".")+1);
				System.out.println(className + ":" + an.depth() + " ");
				numNodes++;
			}
			
			System.out.println("Number of AST Nodes: " + numNodes);
		}
		catch (IOException ioe) {
			
		}
	}
}