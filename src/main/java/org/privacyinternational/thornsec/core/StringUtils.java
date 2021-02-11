/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.core;

import java.math.BigInteger;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.privacyinternational.thornsec.core.exception.data.InvalidPropertyException;

/**
 * This is a collection of helper methods I've built for doing string operations
 *
 * TODO: Really, they probably don't belong exactly here. I've yet to work out
 * where they do belong, however.
 */
public class StringUtils {

	/**
	 * Cleans a string of all non-alphanumeric characters.
	 *
	 * @param toClean     the dirty string
	 * @param replacement what to replace all non-alphanumeric characters with
	 * @return the cleaned string
	 */
	public static String stringToAlphaNumeric(String toClean, String replacement) {
		final String invalidChars = "[^a-zA-Z0-9]";

		return toClean.replaceAll(invalidChars, replacement);
	}

	/**
	 * Removes all non-alphanumeric characters from a given string
	 *
	 * @param toClean the dirty string
	 * @return the cleaned string
	 */
	public static String stringToAlphaNumeric(String toClean) {
		final String invalidChars = "[^a-zA-Z0-9]";

		return toClean.replaceAll(invalidChars, "");
	}

	/**
	 * Loosely based on https://stackoverflow.com/a/12090634
	 *
	 * @param freeformValue String representation of the value
	 * @return the value in megabytes, (rounded to the nearest whole number)
	 */
	public static Integer stringToMegaBytes(String freeformValue) throws InvalidPropertyException {
		Integer returnValue = null;

		final Pattern patt = Pattern.compile("([\\d,]+)([TGMK])", Pattern.CASE_INSENSITIVE);
		final Matcher matcher = patt.matcher(freeformValue.trim());
		final Map<String, Integer> powers = Map.of("T", 4, "G", 3, "M", 2, "K", 1);

		if (matcher.find()) {
			final String number = matcher.group(1);
			final int power = powers.get(matcher.group(2).toUpperCase());
			final BigInteger bytes = new BigInteger(number).multiply(BigInteger.valueOf(1024).pow(power));

			returnValue = bytes.divide(BigInteger.valueOf(1024).pow(2)).intValueExact();
		} else { // At this point it's either just a number, or totally invalid
			try {
				return Integer.parseInt(freeformValue);
			} catch (final NumberFormatException e) {
				throw new InvalidPropertyException(freeformValue + " cannot be parsed to megabytes");
			}
		}

		assert (stringToMegaBytes("1.2g") == 1229);
		assert (stringToMegaBytes("1,4g") == 1434); // We handle that delimiter too
		assert (stringToMegaBytes("0.1MB") == 0);
		assert (stringToMegaBytes("756k") == 1);
		assert (stringToMegaBytes("10g") == 10240);
		assert (stringToMegaBytes("abc") == null);
		assert (stringToMegaBytes("10G") == 10240);
		assert (stringToMegaBytes("1T") == 1048576);

		return returnValue;
	}

}
