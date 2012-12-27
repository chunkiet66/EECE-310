package org.mozilla.javascript;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.mozilla.javascript.ast.*;

public class IfCollectorSequential {
	private static IfData[] if_data = new IfData[500];
	private static int ifData_counter = -1;
	private static int ifData_ID_counter = 0;
	
	private static AssnData[] assn_data = new AssnData[500];
	private static int assnData_counter = -1;
	private static int assnData_ID_counter = 0;
	
	public static void main(String[] args) {
		for (int i = 0; i < 500; i++) {
			if_data[i] = new IfData();
			assn_data[i] = new AssnData();
		}
		
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("Enter JavaScript File To Parse: ");
			System.out.flush();
			String js_file = br.readLine();
			
			long startTime = System.nanoTime();
			long endTime;
			
			//Parse the file
			Parser p = new Parser();
			AstRoot root = p.parse(new FileReader(js_file), "", 1);
			
			IfCollectorSequential ifc = new IfCollectorSequential();
			for (Node kid : root) {
				FunctionNode fn = (FunctionNode)kid;
				ifc.new CollectIfs(fn);
			}
			
			endTime = System.nanoTime();
			long duration = endTime - startTime;
			
			System.out.println("Succeeded");
			System.out.println("Finished in " + duration + " ns");
			
			printStats();
		}
		catch (IOException ioe) {
			System.out.println("Error reading file");
		}
	}
	
	public static void printStats() {
		//Print If data first
		System.out.println("Number of ifs: " + ifData_counter);
		for (int i = 0; i < ifData_counter; i++) {
			System.out.println(if_data[i].ifName + " " + if_data[i].ifCondition + " " + if_data[i].functionName);
		}
		
		//Print Assn data
		System.out.println("\nNumber of assignments: " + assnData_counter);
		for (int i = 0; i < assnData_counter; i++) {
			System.out.println(assn_data[i].assnName + " " + assn_data[i].assnLeft + " " + assn_data[i].assnRight + " " + assn_data[i].functionName);
		}
	}
	
	private class CollectIfs implements NodeVisitor {
		private FunctionNode funcNode;
		
		public CollectIfs(FunctionNode fn) {
			this.funcNode = fn;
			funcNode.visit(this);
		}
		
		@Override
		public boolean visit(AstNode node) {
			if (node instanceof IfStatement) {
				IfStatement if_st = (IfStatement)node;
				
				//Get the condition and function name
				String condition = if_st.getCondition().toSource();
				String functionName = if_st.getEnclosingFunction().getName();
				String ifID;
				
				//Assign an ID to the function
				ifData_ID_counter = Tag.newTag(ifData_ID_counter);
				ifID = "if_" + ifData_ID_counter;
				
				//Add this to if_data
				ifData_counter++;
				if_data[ifData_counter].functionName = functionName;
				if_data[ifData_counter].ifCondition = condition;
				if_data[ifData_counter].ifName = ifID;
			}
			else if (node instanceof Assignment) {
				Assignment assn_st = (Assignment)node;
				
				//Get the left expression, right expression, and function name
				String leftExpr = assn_st.getLeft().toSource();
				String rightExpr = assn_st.getRight().toSource();
				String functionName = assn_st.getEnclosingFunction().getName();
				String assnID;
				
				//Assign an ID to the assignment
				assnData_ID_counter = Tag.newTag(assnData_ID_counter);
				assnID = "assn_" + assnData_ID_counter;
				
				//Add this to assn_data
				assnData_counter++;
				assn_data[assnData_counter].functionName = functionName;
				assn_data[assnData_counter].assnLeft = leftExpr;
				assn_data[assnData_counter].assnRight = rightExpr;
				assn_data[assnData_counter].assnName = assnID;
			}
			
			//Visit the child nodes
			return true;
		}
	}
}