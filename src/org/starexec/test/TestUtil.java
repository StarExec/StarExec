package org.starexec.test;

import org.starexec.constants.R;
import java.util.Random;
public class TestUtil {
	private static String[] letters={"a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z"};
	private static Random rnd=new Random();
	
	/**
	 * produces a random name of the maximum length for a new space. Useful for testing
	 * @return
	 */
	public static String getRandomSpaceName() {
		int length=R.SPACE_NAME_LEN-1;
		
		String name="";
		while (length>0) {
			name=name+letters[rnd.nextInt(letters.length)];
			length--;
		}
		return name;	
	}
}
