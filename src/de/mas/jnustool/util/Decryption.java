package de.mas.jnustool.util;

import de.mas.jnustool.FEntry;
import de.mas.jnustool.TIK;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Decryption
{
	private Cipher cipher2;
	private byte[] decryptedKey;

	public Decryption(TIK ticket)
	{
		this(ticket.getDecryptedKey());
	}

	public Decryption(byte[] decryptedKey)
	{
		this(decryptedKey, 0);
	}

	public Decryption(byte[] decryptedKey, long titleId)
	{
		try
		{
			cipher2 = Cipher.getInstance("AES/CBC/NoPadding");
			this.decryptedKey = decryptedKey;
			init(titleId);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e)
		{
			e.printStackTrace();
		}
	}

	private void init(byte[] IV)
	{
		init(decryptedKey, IV);
	}

	private void init(long titleID)
	{
		init(ByteBuffer.allocate(16).putLong(titleID).array());
	}

	public void init(byte[] decryptedKey, byte[] iv)
	{
		try
		{
			this.decryptedKey = decryptedKey;
			SecretKeySpec secretKeySpec = new SecretKeySpec(decryptedKey, "AES");
			cipher2.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(iv));
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public byte[] decrypt(byte[] input)
	{
		try
		{
			return cipher2.doFinal(input);
		} catch (IllegalBlockSizeException | BadPaddingException e)
		{
			e.printStackTrace();
		}
		return input;
	}

	public byte[] decrypt(byte[] input, int offset, int len)
	{
		try
		{
			return cipher2.doFinal(input, offset, len);
		} catch (IllegalBlockSizeException | BadPaddingException e)
		{
			e.printStackTrace();
		}
		return input;
	}

	byte[] IV;

	public byte[] decryptFileChunk(byte[] blockBuffer, int blockSize, byte[] IV)
	{
		return decryptFileChunk(blockBuffer, 0, blockSize, IV);
	}

	public byte[] decryptFileChunk(byte[] blockBuffer, int offset, int blockSize, byte[] IV)
	{
		if (IV != null)
		{
			this.IV = IV;
		}
		init(this.IV);
		byte[] output = decrypt(blockBuffer, offset, blockSize);
		this.IV = Arrays.copyOfRange(blockBuffer, blockSize - 16, blockSize);
		return output;
	}

	byte[] hash = new byte[20];
	byte[] h0 = new byte[20];

	public byte[] decryptFileChunkHash(byte[] blockBuffer, int blockSize, int block, int contentID)
	{
		if (blockSize != 0x10000)
		{
			throw new IllegalArgumentException("Block size not supported");
		}
		IV = new byte[16];
		IV[1] = (byte) contentID;

		byte[] hashes = decryptFileChunk(blockBuffer, 0x0400, IV);

		System.arraycopy(hashes, 0x14 * block, IV, 0, 16);
		System.arraycopy(hashes, 0x14 * block, h0, 0, 20);

		if (block == 0)
		{
			IV[1] ^= (byte) contentID;
		}

		byte[] output = decryptFileChunk(blockBuffer, 0x400, 0xFC00, IV);

		hash = hash(output);
		if (block == 0)
		{
			assert hash != null;
			hash[1] ^= contentID;
		}
		if (Arrays.equals(hash, h0))
		{
			//System.out.println("checksum right");
		} else
		{
			System.out.println("checksum failed");
			System.out.println(ConversionUtils.ByteArrayToString(hash));
			System.out.println(ConversionUtils.ByteArrayToString(h0));
			throw new IllegalArgumentException("checksumfail");
		}
		return output;
	}

	public static byte[] hash(byte[] hashThis)
	{
		try
		{
			byte[] hash;
			MessageDigest md = MessageDigest.getInstance("SHA-1");

			hash = md.digest(hashThis);
			return hash;
		} catch (NoSuchAlgorithmException noSuchAlgorithm)
		{
			System.err.println("SHA-1 algorithm is not available...");
			System.exit(2);
		}
		return null;
	}

	public void decryptFile(InputStream inputSteam, OutputStream outputStream, FEntry toDownload) throws IOException
	{
		int blockSize = 0x8000;
		long dlFileLength = toDownload.getFileLength();
		if (dlFileLength > (dlFileLength / blockSize) * blockSize)
		{
			dlFileLength = ((dlFileLength / blockSize) * blockSize) + blockSize;
		}

		int bytesRead;

		byte[] IV = new byte[16];
		IV[1] = (byte) toDownload.getContentID();

		byte[] downloadBuffer;

		byte[] blockBuffer = new byte[blockSize];
		byte[] overflowBuffer = new byte[blockSize];
		int overflowSize = 0;

		int inBlockBuffer = 0;
		byte[] tmp = new byte[blockSize];
		boolean end = false;
		long totalDownloadSize = 0;
		long wrote = 0;

		boolean first = true;
		do
		{
			downloadBuffer = new byte[blockSize - overflowSize];
			bytesRead = inputSteam.read(downloadBuffer);
			totalDownloadSize += bytesRead;
			if (bytesRead == -1)
			{
				end = true;
			}

			if (!end)
			{
				System.arraycopy(downloadBuffer, 0, overflowBuffer, overflowSize, bytesRead);
			}

			bytesRead += overflowSize;

			overflowSize = 0;
			int oldInThisBlock = inBlockBuffer;

			if (oldInThisBlock + bytesRead > blockSize)
			{
				int tooMuch = (oldInThisBlock + bytesRead) - blockSize;
				int toRead = blockSize - oldInThisBlock;

				System.arraycopy(overflowBuffer, 0, blockBuffer, oldInThisBlock, toRead);
				inBlockBuffer += toRead;

				overflowSize = tooMuch;
				System.arraycopy(overflowBuffer, toRead, tmp, 0, tooMuch);

				System.arraycopy(tmp, 0, overflowBuffer, 0, tooMuch);
			} else
			{
				if (!end)
				{
					System.arraycopy(overflowBuffer, 0, blockBuffer, inBlockBuffer, bytesRead);
				}
				inBlockBuffer += bytesRead;
			}

			if (inBlockBuffer == blockSize || end)
			{
				if (first)
				{
					first = false;
				} else
				{
					IV = null;
				}

				byte[] output = decryptFileChunk(blockBuffer, blockSize, IV);

				if ((wrote + inBlockBuffer) > toDownload.getFileLength())
				{
					inBlockBuffer = (int) (toDownload.getFileLength() - wrote);
				}

				wrote += inBlockBuffer;
				outputStream.write(output, 0, inBlockBuffer);

				inBlockBuffer = 0;
			}

		} while (totalDownloadSize < dlFileLength && !end);

		outputStream.close();
		inputSteam.close();
	}

	public void decryptFileHash(InputStream inputSteam, OutputStream outputStream, FEntry toDownload) throws IOException
	{
		int blockSize = 0x10000;
		int hashBlockSize = 0xFC00;
		long writeSize = hashBlockSize;    // Hash block size

		long block = (toDownload.getFileOffset() / hashBlockSize) & 0xF;

		long sOffset = toDownload.getFileOffset() - (toDownload.getFileOffset() / hashBlockSize * hashBlockSize);

		long size = toDownload.getFileLength();

		if (sOffset + size > writeSize)
		{
			writeSize = writeSize - sOffset;
		}

		int bytesRead;
		byte[] downloadBuffer;

		byte[] encryptedBlockBuffer = new byte[blockSize];
		byte[] buffer = new byte[blockSize];

		int encryptedBytesInBuffer = 0;
		int bufferPosition = 0;

		byte[] tmp = new byte[blockSize];
		boolean lastPart = false;
		long wrote = 0;

		do
		{
			downloadBuffer = new byte[blockSize - bufferPosition];
			bytesRead = inputSteam.read(downloadBuffer);
			int bytesInBuffer = bytesRead + bufferPosition;
			if (bytesRead == -1)
			{
				lastPart = true;
			} else
			{
				System.arraycopy(downloadBuffer, 0, buffer, bufferPosition, bytesRead); //copy downloaded stuff in buffer
				bufferPosition = 0;
			}

			if (encryptedBytesInBuffer + bytesInBuffer > blockSize)
			{
				int tooMuch = (encryptedBytesInBuffer + bytesInBuffer) - blockSize;
				int toRead = blockSize - encryptedBytesInBuffer;

				System.arraycopy(buffer, 0, encryptedBlockBuffer, encryptedBytesInBuffer, toRead); // make buffer with encrypteddata full
				encryptedBytesInBuffer += toRead;

				bufferPosition = tooMuch; //set buffer position;
				System.arraycopy(buffer, toRead, tmp, 0, tooMuch);
				System.arraycopy(tmp, 0, buffer, 0, tooMuch);

			} else
			{
				if (!lastPart)
				{
					System.arraycopy(buffer, 0, encryptedBlockBuffer, encryptedBytesInBuffer, bytesInBuffer); //When File if at the end, no more need to copy
				}
				encryptedBytesInBuffer += bytesInBuffer;
			}

			//If downloaded block size, or file at the end: Decrypt!
			if (encryptedBytesInBuffer == blockSize || lastPart)
			{
				if (writeSize > size)
				{
					writeSize = size;
				}

				byte[] output = decryptFileChunkHash(encryptedBlockBuffer, blockSize, (int) block, toDownload.getContentID());

				if ((wrote + writeSize) > toDownload.getFileLength())
				{
					writeSize = (int) (toDownload.getFileLength() - wrote);
				}

				outputStream.write(output, (int) (sOffset), (int) writeSize);
				wrote += writeSize;
				encryptedBytesInBuffer = 0;

				block++;
				if (block >= 16)
				{
					block = 0;
				}

				if (sOffset > 0)
				{
					writeSize = hashBlockSize;
					sOffset = 0;
				}
			}
		} while (wrote < toDownload.getFileLength() || lastPart);

		outputStream.close();
		inputSteam.close();
	}

	public void decrypt(FEntry fileEntry, String outputPath) throws IOException
	{
		String[] path = fileEntry.getFullPath().split("/");
		boolean decryptWithHash = false;
		if (!path[1].equals("code") && fileEntry.isExtractWithHash())
		{
			decryptWithHash = true;
		}

		long fileOffset = fileEntry.getFileOffset();
		if (decryptWithHash)
		{
			int blockSize = 0x10000;
			int hashBlockSize = 0xFC00;
			fileOffset = ((fileEntry.getFileOffset() / hashBlockSize) * blockSize);
		}

		InputStream input = new FileInputStream(fileEntry.getContentPath());
		FileOutputStream outputStream = new FileOutputStream(outputPath + "/" + fileEntry.getFileName());

		long actualBytesSkipped = 0;
		long bytesToSkip = fileOffset;

		while (actualBytesSkipped != bytesToSkip)
		{
			actualBytesSkipped += input.skip(bytesToSkip - actualBytesSkipped);
		}

		if (!decryptWithHash)
		{
			decryptFile(input, outputStream, fileEntry);
		} else
		{
			decryptFileHash(input, outputStream, fileEntry);
		}
	}
}