package core;

public class JsonTransform {
	
	/**
	 * Cleans a string of all non-alphanumeric characters.
	 *
	 * @param toClean the dirty string
	 * @param replacement what to replace all non-alphanumeric characters with
	 * @return the cleaned string
	 */
	public static String stringToAlphaNumeric(String toClean, String replacement) {
		String invalidChars = "[^a-zA-Z0-9]";
		
		return toClean.replaceAll(invalidChars, replacement);

	}
}
