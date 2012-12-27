package org.mozilla.javascript;

public class Tag {
	public static int newTag(int oldTag) {
		int y = 0;
		for (int i = 0; i < 10000; i++) {
			double x = 2500000000.0;
			for (int j = 0; j < 100000; j++)
				x = Math.sqrt(x);
			oldTag++;
			y = (int)x;
		}
		
		return oldTag + y;
	}
}