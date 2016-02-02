package de.mas.jnustool.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import de.mas.jnustool.FEntry;
import de.mas.jnustool.TIK;

public class Downloader {
	private static Downloader instance;
	
	public static Downloader getInstance(){
		if(instance == null){
			instance = new Downloader();
		}
		return instance;		
	}
	private Downloader(){		
		
	}
	
	public long titleID =0;
	public TIK ticket = null;
	
	public void downloadAndDecrypt(FEntry toDownload) throws IOException{
		String URL = URL_BASE + "/" + String.format("%016X", titleID) +  "/" + String.format("%08X", toDownload.getNUScontentID());		
		URL url = new URL(URL);
		String [] path = toDownload.getFullPath().split("/");
		boolean decryptWithHash = false;
		if(!path[1].equals("code") && toDownload.isExtractWithHash()){
			decryptWithHash = true;
		}
		HttpURLConnection connection =(HttpURLConnection) url.openConnection();	
		long fileOffset = toDownload.getFileOffset();
		
		if(decryptWithHash){
			int BLOCKSIZE = 0x10000;
			int HASHBLOCKSIZE = 0xFC00;
			fileOffset = ((toDownload.getFileOffset() / HASHBLOCKSIZE) * BLOCKSIZE);
					
		}

		connection.setRequestProperty("Range", "bytes=" + fileOffset +"-");	
	  
	    connection.connect();
	    
	    Decryption decryption = new Decryption(ticket);
	    
	    InputStream input = connection.getInputStream();
	    FileOutputStream outputStream = new FileOutputStream(String.format("%016X", titleID) +"/" + toDownload.getFullPath().substring(1, toDownload.getFullPath().length()));
	    if(!decryptWithHash){
	        decryption.decryptFile(input, outputStream, toDownload);
	    }else{
	    	 decryption.decryptFileHash(input, outputStream, toDownload);
	    }

        connection.disconnect();
	}	
	
	public static String URL_BASE = "";

	public void downloadTMD(int version) throws IOException {
		downloadTMD();		
	}
	public void downloadTMD() throws IOException {
		String URL = URL_BASE + "/" + String.format("%016X", titleID) +  "/tmd";
		downloadFile(URL, "tmd");		
	}
	public void downloadFile(String fileURL,String filename) throws IOException{
		int BUFFER_SIZE = 0x800;
		URL url = new URL(fileURL);
	    HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
	
        InputStream inputStream = httpConn.getInputStream();
    
        FileOutputStream outputStream = new FileOutputStream(filename);

        int bytesRead = -1;
        byte[] buffer = new byte[BUFFER_SIZE];
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        outputStream.close();
        inputStream.close();
	  
	    httpConn.disconnect();
	}
	public void downloadTicket() throws IOException {
		String URL = URL_BASE + "/" + String.format("%016X", titleID) +  "/cetk";
		downloadFile(URL, "cetk");
	}
	public void downloadContent(int contentID) throws IOException {
		String URL = URL_BASE + "/" + String.format("%016X", titleID) +  "/" + String.format("%08X", contentID);
		downloadFile(URL, String.format("%08X", contentID));
		
	}
	public byte[] downloadContentToByteArray(int contentID) throws IOException {
		String URL = URL_BASE + "/" + String.format("%016X", titleID) +  "/" + String.format("%08X", contentID);
		return downloadFileToByteArray(URL);		
	}
	public byte[] downloadTMDToByteArray() throws IOException {
		String URL = URL_BASE + "/" + String.format("%016X", titleID) +  "/tmd";
		return downloadFileToByteArray(URL);
	}
	private byte[] downloadFileToByteArray(String fileURL) throws IOException {
		
		int BUFFER_SIZE = 0x800;
		URL url = new URL(fileURL);
	    HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
	    int responseCode = httpConn.getResponseCode();
	    
        // always check HTTP response code first
	    byte[] file = null;
	    
        if (responseCode == HttpURLConnection.HTTP_OK) {
		    int contentLength = httpConn.getContentLength();
		    
		    file = new byte[contentLength];
		    // always check HTTP response code first
	        
	        InputStream inputStream = httpConn.getInputStream();
	
	        int bytesRead = -1;
	        byte[] buffer = new byte[BUFFER_SIZE];
	        int filePostion = 0;
	        while ((bytesRead = inputStream.read(buffer)) != -1) {
	        	System.arraycopy(buffer, 0, file, filePostion,bytesRead);
	        	filePostion+=bytesRead;
	    		
	        }
	        inputStream.close();
        }else{
        	System.err.println("File not found: " + fileURL);
        }
	    httpConn.disconnect();
	    return file;
		
	}
	public byte[] downloadTicketToByteArray() throws IOException {
		String URL = URL_BASE + "/" + String.format("%016X", titleID) +  "/cetk";
		return downloadFileToByteArray(URL);
	}
	
}
