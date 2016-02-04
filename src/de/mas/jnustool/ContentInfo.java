package de.mas.jnustool;

import de.mas.jnustool.util.ConversionUtils;

public class ContentInfo
{
	public short indexOffset;            //	0	 0x204
	public short commandCount;            //	2	 0x206
	public byte[] SHA2 = new byte[32];    //  12 	 0x208

	// TODO: Test, size checking
	public ContentInfo(byte[] info)
	{
		this.indexOffset = (short) (((info[0] & 0xFF) << 8) | (info[1] & 0xFF));
		this.commandCount = (short) (((info[2] & 0xFF) << 8) | (info[3] & 0xFF));
		for (int i = 0; i < 32; i++)
		{
			this.SHA2[i] = info[4 + i];
		}
	}

	public ContentInfo(short indexOffset, short commandCount, byte[] SHA2)
	{
		this.indexOffset = indexOffset;
		this.commandCount = commandCount;
		this.SHA2 = SHA2;
	}

	@Override
	public String toString()
	{
		return "indexOffset: " + indexOffset + " commandCount: " + commandCount + " SHA2: " + ConversionUtils.ByteArrayToString(SHA2);
	}
}