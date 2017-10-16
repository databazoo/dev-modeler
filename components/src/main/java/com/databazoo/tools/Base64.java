
package com.databazoo.tools;

import java.util.Arrays;

/**
 * Cut-down version of original encoder/decoder by Mikael Grev
 *
 * @author Mikael Grev
 *         Date: 2004-aug-02
 */
public class Base64 {
	private static final char[] CA = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();
	private static final int[] IA = new int[256];
	static {
		Arrays.fill(IA, -1);
		for (int i = 0, iS = CA.length; i < iS; i++) {
			IA[CA[i]] = i;
		}
		IA['='] = 0;
	}

	static String encode(byte[] sArr, boolean lineSep)
	{
		// Check special case
		int sLen = sArr != null ? sArr.length : 0;
		if (sLen == 0) {
			return "";
		}

		int eLen = (sLen / 3) * 3;              // Length of even 24-bits.
		int cCnt = ((sLen - 1) / 3 + 1) << 2;   // Returned character count
		int dLen = cCnt + (lineSep ? (cCnt - 1) / 76 << 1 : 0); // Length of returned array
		char[] dArr = new char[dLen];

		// Encode even 24-bits
		for (int s = 0, d = 0, cc = 0; s < eLen;) {
			// Copy next three bytes into lower 24 bits of int, paying attension to sign.
			int i = (sArr[s++] & 0xff) << 16 | (sArr[s++] & 0xff) << 8 | (sArr[s++] & 0xff);

			// Encode the int into four chars
			dArr[d++] = CA[(i >>> 18) & 0x3f];
			dArr[d++] = CA[(i >>> 12) & 0x3f];
			dArr[d++] = CA[(i >>> 6) & 0x3f];
			dArr[d++] = CA[i & 0x3f];

			// Add optional line separator
			if (lineSep && ++cc == 19 && d < dLen - 2) {
				dArr[d++] = '\r';
				dArr[d++] = '\n';
				cc = 0;
			}
		}

		// Pad and encode last bits if source isn't even 24 bits.
		int left = sLen - eLen; // 0 - 2.
		if (left > 0) {
			// Prepare the int
			int i = ((sArr[eLen] & 0xff) << 10) | (left == 2 ? ((sArr[sLen - 1] & 0xff) << 2) : 0);

			// Set last four chars
			dArr[dLen - 4] = CA[i >> 12];
			dArr[dLen - 3] = CA[(i >>> 6) & 0x3f];
			dArr[dLen - 2] = left == 2 ? CA[i & 0x3f] : '=';
			dArr[dLen - 1] = '=';
		}
		return new String(dArr);
	}

	public static byte[] decode(String str)
	{
		if(str == null){
			str = "";
		}

		// Check special case
		int sLen = str.length();
		if (sLen == 0) {
			return new byte[0];
		}
		int sepCnt = 0; // Number of separator characters. (Actually illegal characters, but that's a bonus...)
		for (int i = 0; i < sLen; i++) {
			if (IA[str.charAt(i)] < 0) {
				sepCnt++;
			}
		}

		// Check so that legal chars (including '=') are evenly divideable by 4 as specified in RFC 2045.
		if ((sLen - sepCnt) % 4 != 0) {
			return null;
		}

		// Count '=' at end
		int pad = 0;
		for (int i = sLen; i > 1 && IA[str.charAt(--i)] <= 0;) {
			if (str.charAt(i) == '=') {
				pad++;
			}
		}

		int len = ((sLen - sepCnt) * 6 >> 3) - pad;
		byte[] dArr = new byte[len];       // Preallocate byte[] of exact length
		for (int s = 0, d = 0; d < len;) {
			// Assemble three bytes into an int from four "valid" characters.
			int i = 0;
			for (int j = 0; j < 4; j++) {   // j only increased if a valid char was found.
				int c = IA[str.charAt(s++)];
				if (c >= 0) {
					i |= c << (18 - j * 6);
				} else {
					j--;
				}
			}
			// Add the bytes
			dArr[d++] = (byte) (i >> 16);
			if (d < len) {
				dArr[d++]= (byte) (i >> 8);
				if (d < len) {
					dArr[d++] = (byte) i;
				}
			}
		}
		return dArr;
	}
}
