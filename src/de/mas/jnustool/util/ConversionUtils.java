package de.mas.jnustool.util;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class ConversionUtils
{
	public static byte[] commonKey;

	public static byte[] hexStringToByteArray(String s)
	{
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2)
		{
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
					+ Character.digit(s.charAt(i + 1), 16));
		}
		return data;
	}

	public static String ByteArrayToString(byte[] ba)
	{
		StringBuilder hex = new StringBuilder(ba.length * 2);
		for (byte b : ba)
		{
			hex.append(String.format("%X", b));
		}
		return hex.toString();
	}

	public static int getIntFromBytes(byte[] input, int offset)
	{
		return ByteBuffer.wrap(Arrays.copyOfRange(input, offset, offset + 4)).getInt();
	}

	public static long getIntAsLongFromBytes(byte[] input, int offset)
	{
		long result;

		if ((int) input[offset] + 128 > 0 && (int) input[offset] + 128 < 128)
		{

			input[offset] += 128;

			result = (long) ByteBuffer.wrap(Arrays.copyOfRange(input, offset, offset + 4)).getInt();

			result += 1024L * 1024L * 2048L;
			return result;

		}

		return (long) ByteBuffer.wrap(Arrays.copyOfRange(input, offset, offset + 4)).getInt();
	}

	public static short getShortFromBytes(byte[] input, int offset)
	{
		return ByteBuffer.wrap(Arrays.copyOfRange(input, offset, offset + 2)).getShort();
	}

	public static long StringToLong(String s)
	{
		try
		{
			BigInteger bi = new BigInteger(s, 16);
			return bi.longValue();
		} catch (NumberFormatException e)
		{
			System.err.println("Invalid Title ID");
			return 0L;
		}
	}
}