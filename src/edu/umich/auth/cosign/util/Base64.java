package edu.umich.auth.cosign.util;

import java.io.ByteArrayOutputStream;

/**
 * This class creates a string in Base64 format.  It is used
 * for the Cosign service cookie.
 * 
 * @author htchan
 */
public class Base64 {
	
	private static final byte NON_BASE64_CHAR = -1;
	private static final byte PADDING = -2;

	private static final char[] base64Chars = {
		'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
		'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
		'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
		'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
		'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
		'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
		'w', 'x', 'y', 'z', '0', '1', '2', '3',
		'4', '5', '6', '7', '8', '9', '+', '-'
	};
	
	private static final byte[] revBase64Chars = new byte[0x100];
	
	static {
		for (int i = 64; i < revBase64Chars.length; i++) {
			revBase64Chars[i] = NON_BASE64_CHAR;
		}
		revBase64Chars['='] = PADDING;
		
		for (byte i = 0; i < base64Chars.length; i++) {
			revBase64Chars[base64Chars[i]] = i;
		}
	}

	/**
	 * Constructor for Base64.
	 */
	public Base64() {
		super();
	}
	
	/*
	 * The first byte of our bytes array is always valid
	 * or the StringBuffer will return an empty string.
	 * The idea here is to divide the bytes array into
	 * logic groups with 3 bytes each.  Since 3 bytes of
	 * data can create 4 bytes for base64 encoding:
	 *    [AAAAAABB] [BBBBCCCC] [CCDDDDDD]
	 * [00AAAAAA] [00BBBBBB] [00CCCCCC] [00DDDDDD]
	 * Because we are processing the byte array 3 bytes
	 * at a time, we have to check to make sure the second
	 * and third bytes exist or not.
	 * 
	 * Note: 0xff & bytes[i] is used to converted signed
	 *       binary to unsigned binary.
	 */
	public static String encode(byte[] bytes) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < bytes.length; i += 3) {
			// Use the first 6 bit of byte 1
			sb.append(base64Chars[(0xff & bytes[i]) >> 2]);
			if (bytes.length > i + 1) {
				// Use the last 2 bits of byte 1 and first 4 bits of byte 2
				sb.append(base64Chars[(((0xff & bytes[i]) << 4) & 0x30) 
									  | ((0xff & bytes[i + 1]) >> 4)]);
				if (bytes.length > i + 2) {
					// use the last 4 bits of byte 2 and first 2 bits of byte 3
					sb.append(base64Chars[(((0xff & bytes[i + 1]) << 2) & 0x3c)
										  | ((0xff & bytes[i + 2]) >> 6)]);
					// use the last 6 bits of byte 3
					sb.append(base64Chars[bytes[i + 2] & 0x3F]);
				}
				else {
					// use the last 4 bits of byte 2 and padding a '='
					sb.append(base64Chars[(((0xff & bytes[i + 1]) << 2) & 0x3c)]);
					sb.append('=');
				}
			}
			else {
				// Use the last 2 bits of byte 1 and padding two '='s
				sb.append(base64Chars[(((0xff & bytes[i]) << 4) & 0x30)]);
				sb.append('=');
				sb.append('=');
			}
		}
		return sb.toString();
	}
	
	/*
	 * It is the reverse process of Base64 encoding.  The
	 * input string is first converted to a byte array.  It
	 * is then parsed by the parseEncodedBytes method to
	 * make sure it is a valid Base64 encoded bytes array.
	 * After that, the byte array is then divided into groups
	 * of 4 bytes each and the least significant 6 bits are
	 * extracted like below:
	 * 
	 * [00AAAAAA] [00BBBBBB] [00CCCCCC] [00DDDDDD]
	 *     [AAAAAABB] [BBBBCCCC] [CCDDDDDD]
	 * 
	 * Each Base64 encoded string is guaranteed to have at
	 * least 2 bytes.  So we just need to handle the cases
	 * when there are 2 bytes, 3 bytes or 4 bytes of data.
	 */
	public static byte[] decode(String str) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] bytes = parseEncodedBytes(str.getBytes());
		for (int i = 0; i < bytes.length; i += 4) {
			// Use last 6 bits of byte 1 and first 2 bits of byte 2
			out.write(bytes[i] << 2 | bytes[i + 1] >> 4);
			if (bytes.length > i + 2) {
				// Use last 4 bits of byte 2 and first 4 bits of byte 3
				out.write(bytes[i + 1] << 4 | bytes[i + 2] >> 2);
				if (bytes.length > i + 3) {
					// Use last 2 bits of byte 3 and first 6 bits of byte 4
					out.write(bytes[i + 2] << 6 | bytes[i + 3]);
				}
			}
		}
		return out.toByteArray();
	}
	
	private static byte[] parseEncodedBytes(byte[] bytes) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int padding = 0;
		for (int i = 0; i < bytes.length; i++) {
			byte tmp = revBase64Chars[bytes[i]];
			if (tmp == NON_BASE64_CHAR
				|| bytes.length < 2
				|| (padding > 0 && tmp > NON_BASE64_CHAR)) {
				throw new RuntimeException("Not a valid Base64 encoded string!");
			}
			if (tmp == PADDING) {
				padding++;
			}
			else {
				out.write(tmp);
			}
		}
		return out.toByteArray();
	}
}
