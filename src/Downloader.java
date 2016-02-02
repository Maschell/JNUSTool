import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

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
	
	public void downloadAndDecrypt(String URL,long fileOffset,long fileLength,FEntry toDownload,TIK ticket) throws IOException{
		URL url = new URL(URL);

	    HttpURLConnection connection =
	            (HttpURLConnection) url.openConnection();
	
	    int BLOCKSIZE = 0x8000;
	    long dlFileLength = fileLength;
	    if(dlFileLength > (dlFileLength/BLOCKSIZE)*BLOCKSIZE){
	    	dlFileLength = ((dlFileLength/BLOCKSIZE)*BLOCKSIZE) +BLOCKSIZE;	    	
	    }
	   
	    connection.setRequestProperty("Range", "bytes=" + fileOffset+"-");	    
	    connection.connect();
	    
	    
	    FileOutputStream outputStream = new FileOutputStream(String.format("%016X", titleID) +"/" + toDownload.getFullPath().substring(1, toDownload.getFullPath().length()));
        InputStream input = connection.getInputStream();
     
        int bytesRead = -1;
       
        byte[] IV = new byte[16];
        IV[1] = (byte)toDownload.getContentID();
        
        byte[] downloadBuffer;
        
        byte[] blockBuffer = new byte[BLOCKSIZE];
        byte[] overflowBuffer = new byte[BLOCKSIZE];
        int overflowsize = 0;
        
        int inBlockBuffer = 0;
        byte[] tmp = new byte[BLOCKSIZE];
        boolean endd = false;
        long downloadTotalsize = 0;
        long wrote = 0;
        Decryption decryption = new Decryption(ticket);
        boolean first = true;
        do{
        	
        	downloadBuffer = new byte[BLOCKSIZE-overflowsize];
        
        	bytesRead = input.read(downloadBuffer);
        	downloadTotalsize += bytesRead;
        	if(bytesRead ==-1){ 
        		endd = true;
        	}
        	
        	if(!endd)System.arraycopy(downloadBuffer, 0, overflowBuffer, overflowsize,bytesRead);
        	
        	bytesRead += overflowsize;
        	
        	overflowsize = 0;
        	int oldInThisBlock  = inBlockBuffer;
        	
        	if(oldInThisBlock + bytesRead > BLOCKSIZE){
        		
        		int tooMuch = (oldInThisBlock + bytesRead) - BLOCKSIZE;
        		int toRead = BLOCKSIZE - oldInThisBlock;
        		
        		System.arraycopy(overflowBuffer, 0, blockBuffer, oldInThisBlock, toRead);
        		inBlockBuffer += toRead;
        		
        		overflowsize = tooMuch;
        		System.arraycopy(overflowBuffer, toRead, tmp, 0, tooMuch);
        		
        		System.arraycopy(tmp, 0, overflowBuffer, 0, tooMuch);
        		
        		
        	}else{        		
        		if(!endd)System.arraycopy(overflowBuffer, 0, blockBuffer, inBlockBuffer, bytesRead);        		
        		inBlockBuffer +=bytesRead;
        	}
        	
        	if(inBlockBuffer == BLOCKSIZE || endd){
        		if(first){
        			first = false;        			
        		}else{
        			IV = null;
        		}
        		
        		byte[] output =decryption.decryptFileChunk(blockBuffer,BLOCKSIZE,IV);
             	
             	if((wrote + inBlockBuffer) > fileLength){             		
             		inBlockBuffer = (int) (fileLength- wrote);             		
             	}
             	
             	wrote += inBlockBuffer;
        		outputStream.write(output, 0, inBlockBuffer);
        		
        		inBlockBuffer = 0;
        	}
        	      	        	
        }while(downloadTotalsize < dlFileLength && !endd);

        outputStream.close();
        input.close();
        
        connection.disconnect();
	}
	
	public void downloadAndDecryptHash(String URL,long fileOffset,long fileLength,FEntry toDownload,TIK ticket) throws IOException{
		int BLOCKSIZE = 0x10000;
		int HASHBLOCKSIZE = 0xFC00;
		long writeSize = HASHBLOCKSIZE;	// Hash block size
		
		long block			= (fileOffset / HASHBLOCKSIZE) & 0xF;
		
		long soffset = fileOffset - (fileOffset / HASHBLOCKSIZE * HASHBLOCKSIZE);
		fileOffset = ((fileOffset / HASHBLOCKSIZE) * BLOCKSIZE);		

		long size =  fileLength;
		
		if( soffset+size > writeSize )
			writeSize = writeSize - soffset;
		
		URL url = new URL(URL);

	    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	
	    connection.setRequestProperty("Range", "bytes=" + fileOffset+"-");
	    connection.connect();
	   	    		
        FileOutputStream outputStream = new FileOutputStream(String.format("%016X", titleID) +"/" +toDownload.getFullPath().substring(1, toDownload.getFullPath().length()));
        InputStream input = connection.getInputStream();
       
        int bytesRead = -1;
        byte[] downloadBuffer;
        
        byte[] encryptedBlockBuffer = new byte[BLOCKSIZE];
        byte[] buffer = new byte[BLOCKSIZE];
        
        int encryptedBytesInBuffer = 0;
        int bufferPostion = 0;
        
       
        byte[] tmp = new byte[BLOCKSIZE];
        boolean lastPart = false;
        long wrote = 0;
        Decryption decryption = new Decryption(ticket);       
        do{        	
        	downloadBuffer = new byte[BLOCKSIZE-bufferPostion];
        	bytesRead = input.read(downloadBuffer);
        	int bytesInBuffer = bytesRead + bufferPostion;
        	if(bytesRead ==-1){ 
        		lastPart = true;
        	}else{
        		System.arraycopy(downloadBuffer, 0, buffer, bufferPostion,bytesRead); //copy downloaded stuff in buffer        	
        		bufferPostion = 0;
        	}        	
        	        	
        	if(encryptedBytesInBuffer + bytesInBuffer > BLOCKSIZE){        		
        		int tooMuch = (encryptedBytesInBuffer + bytesInBuffer) - BLOCKSIZE;
        		int toRead = BLOCKSIZE - encryptedBytesInBuffer;  
        		
        		System.arraycopy(buffer, 0, encryptedBlockBuffer, encryptedBytesInBuffer, toRead); // make buffer with encrypteddata full        		
        		encryptedBytesInBuffer += toRead;
        		
        		bufferPostion = tooMuch; //set buffer position;
        		System.arraycopy(buffer, toRead, tmp, 0, tooMuch);        		
        		System.arraycopy(tmp, 0, buffer, 0, tooMuch);
        		
        	}else{
        		if(!lastPart) System.arraycopy(buffer, 0, encryptedBlockBuffer, encryptedBytesInBuffer, bytesInBuffer); //When File if at the end, no more need to copy
        		encryptedBytesInBuffer +=bytesInBuffer;
        	}
        	
        	//If downloaded BLOCKSIZE, or file at the end: Decrypt!
        	if(encryptedBytesInBuffer == BLOCKSIZE || lastPart){
        		
        		if( writeSize > size )
        			writeSize = size;
        		
        		byte[] output = decryption.decryptFileChunkHash(encryptedBlockBuffer, BLOCKSIZE, (int) block,toDownload.getContentID());
             	
             	if((wrote + writeSize) > fileLength){             		
             		writeSize = (int) (fileLength- wrote);
             	}
                         
        		outputStream.write(output, (int)(0+soffset), (int)writeSize);
        		wrote +=writeSize;
        		encryptedBytesInBuffer = 0;
        		
        		block++;
        		if( block >= 16 )
        				block = 0;
        		
        		if( soffset > 0)
        		{
        			writeSize = HASHBLOCKSIZE;
        			soffset = 0;
        		}
        	}    	
        }while(wrote < fileLength || lastPart);

        outputStream.close();
        input.close();
        connection.disconnect();
	}

	public long titleID =0;
	public TIK ticket = null;
	public void download( FEntry toDownload) {
		File f = new File (String.format("%016X", titleID));
		if(!f.exists())f.mkdir();
		
		f = new File(String.format("%016X", titleID) +"/" +toDownload.getFullPath().substring(1, toDownload.getFullPath().length()));
		if(f.exists()){
			if(f.length() == toDownload.getFileLength()){
				System.out.println("Skipping: " + String.format("%8.2f MB ", toDownload.getFileLength()/1024.0/1024.0)  + toDownload.getFullPath());
				return;
			}
		}
		
		
		System.out.println("Downloading: " + String.format("%8.2f MB ", toDownload.getFileLength()/1024.0/1024.0)  + toDownload.getFullPath());
		
		String URL = URL_BASE + "/" + String.format("%016X", titleID) +  "/" + String.format("%08X", toDownload.getNUScontentID());		
		String [] path = toDownload.getFullPath().split("/");
	   
	    String folder = String.format("%016X", titleID) +"/";
	    File folder_ = null;
	    for(int i = 0;i<path.length-1;i++){
	    	if(!path[i].equals("")){	    		
	    		folder += path[i] + "/";
	    		folder_ = new File(folder);
	    	    if(!folder_.exists()){
	    	    	folder_.mkdir();	    	    	
	    	    }
	    	}	    	
	    }
	    
	  
		try {
			//if(toDownload.isExtractWithHash()){
			if(!path[1].equals("code") && toDownload.isExtractWithHash()){
				downloadAndDecryptHash(URL,toDownload.getFileOffset(),toDownload.getFileLength(),toDownload,ticket);
			}else{
				downloadAndDecrypt(URL,toDownload.getFileOffset(),toDownload.getFileLength(),toDownload,ticket);
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
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
