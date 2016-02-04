package de.mas.jnustool.util;

import de.mas.jnustool.FEntry;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Downloader
{
	private static Downloader instance;

	public static Downloader getInstance()
	{
		if (instance == null)
		{
			instance = new Downloader();
		}

		return instance;
	}

	private Downloader()
	{

	}

	public void downloadAndDecrypt(FEntry toDownload) throws IOException
	{
		String URL = URL_BASE + "/" + String.format("%016X", toDownload.getTitleID()) + "/" + String.format("%08X", toDownload.getNUSContentID());
		URL url = new URL(URL);
		String[] path = toDownload.getFullPath().split("/");
		boolean decryptWithHash = false;
		if (!path[1].equals("code") && toDownload.isExtractWithHash())
		{
			decryptWithHash = true;
		}
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		long fileOffset = toDownload.getFileOffset();

		if (decryptWithHash)
		{
			int blockSize = 0x10000;
			int hashBlockSize = 0xFC00;
			fileOffset = ((toDownload.getFileOffset() / hashBlockSize) * blockSize);
		}

		connection.setRequestProperty("Range", "bytes=" + fileOffset + "-");

		connection.connect();

		Decryption decryption = new Decryption(toDownload.getTicket());

		InputStream input = connection.getInputStream();
		FileOutputStream outputStream = new FileOutputStream(String.format("%016X", toDownload.getTitleID()) + "/" + toDownload.getFullPath().substring(1, toDownload.getFullPath().length()));
		if (!decryptWithHash)
		{
			decryption.decryptFile(input, outputStream, toDownload);
		} else
		{
			decryption.decryptFileHash(input, outputStream, toDownload);
		}

		connection.disconnect();
	}

	public static String URL_BASE = "";

	public void downloadTMD(long titleID, String path) throws IOException
	{
		String URL = URL_BASE + "/" + String.format("%016X", titleID) + "/tmd";
		downloadFile(URL, "tmd", path);
	}

	public void downloadFile(String fileURL, String filename, String tmpPath) throws IOException
	{
		int BUFFER_SIZE = 0x800;
		URL url = new URL(fileURL);
		HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();

		InputStream inputStream = httpConn.getInputStream();
		if (tmpPath != null)
		{
			filename = tmpPath + "/" + filename;
		}

		FileOutputStream outputStream = new FileOutputStream(filename);

		int bytesRead;
		byte[] buffer = new byte[BUFFER_SIZE];
		while ((bytesRead = inputStream.read(buffer)) != -1)
		{
			outputStream.write(buffer, 0, bytesRead);
		}

		outputStream.close();
		inputStream.close();

		httpConn.disconnect();
	}

	public void downloadTicket(long titleID, String path) throws IOException
	{
		String URL = URL_BASE + "/" + String.format("%016X", titleID) + "/cetk";
		downloadFile(URL, "cetk", path);
	}

	public byte[] downloadContentToByteArray(long titleID, int contentID) throws IOException
	{
		String URL = URL_BASE + "/" + String.format("%016X", titleID) + "/" + String.format("%08X", contentID);
		return downloadFileToByteArray(URL);
	}

	public byte[] downloadTMDToByteArray(long titleID) throws IOException
	{
		String URL = URL_BASE + "/" + String.format("%016X", titleID) + "/tmd";
		return downloadFileToByteArray(URL);
	}

	private byte[] downloadFileToByteArray(String fileURL) throws IOException
	{
		int BUFFER_SIZE = 0x800;
		URL url = new URL(fileURL);
		HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
		int responseCode = httpConn.getResponseCode();

		// always check HTTP response code first
		byte[] file = null;

		if (responseCode == HttpURLConnection.HTTP_OK)
		{
			int contentLength = httpConn.getContentLength();

			file = new byte[contentLength];
			// always check HTTP response code first

			InputStream inputStream = httpConn.getInputStream();

			int bytesRead;
			byte[] buffer = new byte[BUFFER_SIZE];
			int filePosition = 0;
			while ((bytesRead = inputStream.read(buffer)) != -1)
			{
				System.arraycopy(buffer, 0, file, filePosition, bytesRead);
				filePosition += bytesRead;
			}
			inputStream.close();
		} else
		{
			System.err.println("File not found: " + fileURL);
		}
		httpConn.disconnect();
		return file;
	}

	public byte[] downloadTicketToByteArray(long titleID) throws IOException
	{
		String URL = URL_BASE + "/" + String.format("%016X", titleID) + "/cetk";
		return downloadFileToByteArray(URL);
	}

	public void downloadContent(long titleID, int contentID, String tmpPath) throws IOException
	{
		String URL = URL_BASE + "/" + String.format("%016X", titleID) + "/" + String.format("%08X", contentID);
		downloadFile(URL, String.format("%08X", contentID) + ".app", tmpPath);
	}
}