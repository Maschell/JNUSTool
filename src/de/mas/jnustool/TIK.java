package de.mas.jnustool;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import de.mas.jnustool.util.Decryption;
import de.mas.jnustool.util.ConversionUtils;

public class TIK
{
	private byte[] encryptedKey = new byte[16];
	private byte[] decryptedKey = new byte[16];

	public TIK(File commonETicket, long titleID) throws IOException
	{
		parse(commonETicket);
		calculateDecryptedKey(titleID);
	}

	public TIK(String ticketKey, long titleID)
	{
		setEncryptedKey(ticketKey);
		calculateDecryptedKey(titleID);
	}

	public TIK(byte[] file, long titleID) throws IOException
	{
		parse(file);
		calculateDecryptedKey(titleID);
	}

	private void calculateDecryptedKey(long titleID)
	{
		Decryption decryption = new Decryption(ConversionUtils.commonKey, titleID);
		decryptedKey = decryption.decrypt(encryptedKey);
	}

	private void parse(byte[] commonETicketBytes) throws IOException
	{
		System.arraycopy(commonETicketBytes, 0x1bf, this.encryptedKey, 0, 16);
	}

	private void parse(File commonETicket) throws IOException
	{
		RandomAccessFile f = new RandomAccessFile(commonETicket, "r");
		f.seek(0x1bf);
		f.read(this.encryptedKey, 0, 16);
		f.close();
	}

	public void setEncryptedKey(String key)
	{
		this.encryptedKey = ConversionUtils.hexStringToByteArray(key);
	}

	public byte[] getDecryptedKey()
	{
		return decryptedKey;
	}

	@Override
	public String toString()
	{
		return "encrypted key: " + ConversionUtils.ByteArrayToString(encryptedKey) + " decrypted key: " + ConversionUtils.ByteArrayToString(decryptedKey);
	}
}