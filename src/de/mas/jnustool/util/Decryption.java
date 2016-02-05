package de.mas.jnustool.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import de.mas.jnustool.FEntry;
import de.mas.jnustool.TIK;

public class Decryption {	
	Cipher cipher2;
	
	public Decryption(TIK ticket){		
		this(ticket.getDecryptedKey());
	}
	
	public Decryption(byte[] decryptedKey){
		this(decryptedKey,0);
	}
	
	public Decryption(byte[] decryptedKey, long titleId) {	
		try {
			cipher2 = Cipher.getInstance("AES/CBC/NoPadding");
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			e.printStackTrace();
		}
		this.decryptedKey = decryptedKey;
		init(titleId);	
	}


	byte[] decryptedKey;
	
	
	private void init(byte[] IV) {
		init(decryptedKey,IV);
	}
	
	private void init(long titleid) {
		init(ByteBuffer.allocate(16).putLong(titleid).array());
	}
	
	public void init(byte[] decryptedKey,long titleid){
		init(decryptedKey,ByteBuffer.allocate(16).putLong(titleid).array());
	}
	
	public void init(byte[] decryptedKey,byte[] iv){
		this.decryptedKey = decryptedKey;			
		SecretKeySpec secretKeySpec = new SecretKeySpec(decryptedKey, "AES");
		try {
			cipher2.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(iv));
		} catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
			e.printStackTrace();
			System.exit(2);
		}
	}
	
	public byte[] decrypt(byte[] input){
		try {
			return cipher2.doFinal(input);			
		} catch (IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
			System.exit(2);
		}
		return input;	
	}
	
	public byte[] decrypt(byte[] input,int len){
		return decrypt(input,0,len);
	}
	
	public byte[] decrypt(byte[] input,int offset,int len){
		try {			
			return cipher2.doFinal(input, offset, len);			
		} catch (IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
			System.exit(2);
		}
		return input;	
	}
	
	byte[] IV;
	public byte[] decryptFileChunk(byte[] blockBuffer, int BLOCKSIZE, byte[] IV) {
		return  decryptFileChunk(blockBuffer,0,BLOCKSIZE, IV);
	}
		
	public byte[] decryptFileChunk(byte[] blockBuffer, int offset, int BLOCKSIZE, byte[] IV) {		
		if(IV != null) this.IV = IV;
		init(this.IV);
     	byte[] output = decrypt(blockBuffer,offset,BLOCKSIZE);        
     	this.IV = Arrays.copyOfRange(blockBuffer,BLOCKSIZE-16, BLOCKSIZE);
		return output;
	}

 	byte[] hash = new byte[20];
 	byte[] h0 = new byte[20];
 	
	public byte[] decryptFileChunkHash(byte[] blockBuffer, int BLOCKSIZE, int block, int contentID){
		if(BLOCKSIZE != 0x10000) throw new IllegalArgumentException("Blocksize not supported");
		IV = new byte[16];
		IV[1] = (byte)contentID;
		    
     	byte[] hashes = decryptFileChunk(blockBuffer,0x0400,IV);
     	
     	System.arraycopy(hashes, (int) (0x14*block), IV, 0, 16);     	
     	System.arraycopy(hashes, (int) (0x14*block), h0, 0, 20);     	
     	
     	if( block == 0 )
     		IV[1] ^= (byte)contentID;     	
     	
		byte[] output =  decryptFileChunk(blockBuffer,0x400,0xFC00,IV);
		
		hash = hash(output);
		if(block == 0)			
			hash[1] ^= contentID;
		
		if(!Arrays.equals(hash, h0)){			
			System.out.println("checksum failed");
			System.out.println(Util.ByteArrayToString(hash));
			System.out.println(Util.ByteArrayToString(h0));
			throw new IllegalArgumentException("checksumfail");			
		}else{
			//System.out.println("checksum right!");
		}
		return output;
	}
	
	public static byte[] hash(byte[] hashThis) {
	    try {
	      byte[] hash = new byte[20];
	      MessageDigest md = MessageDigest.getInstance("SHA-1");
	      hash = md.digest(hashThis);
	      return hash;
	    } catch (NoSuchAlgorithmException nsae) {
	      System.err.println("SHA-1 algorithm is not available...");
	      System.exit(2);
	    }
	    return null;
	  }
	
	
	public void decryptFile(InputStream inputStream, OutputStream outputStream,FEntry toDownload) throws IOException{		 
		int BLOCKSIZE = 0x8000;		
		long dlFileLength = toDownload.getFileLength();
	    if(dlFileLength > (dlFileLength/BLOCKSIZE)*BLOCKSIZE){
	    	dlFileLength = ((dlFileLength/BLOCKSIZE)*BLOCKSIZE) +BLOCKSIZE;	    	
	    }		
	
        byte[] IV = new byte[16];
        IV[1] = (byte)toDownload.getContentID();
     
        byte[] blockBuffer = new byte[BLOCKSIZE];
        
        int inBlockBuffer;      
        long wrote = 0;
        
        boolean first = true;
        ByteArrayBuffer overflow = new ByteArrayBuffer(BLOCKSIZE);
        do{ 
        		inBlockBuffer = getChunkFromStream(inputStream,blockBuffer,overflow,BLOCKSIZE);
        		if(first){
        			first = false;        			
        		}else{
        			IV = null;
        		}
        		
        		byte[] output = decryptFileChunk(blockBuffer,BLOCKSIZE,IV);
             	
             	if((wrote + inBlockBuffer) > toDownload.getFileLength()){             		
             		inBlockBuffer = (int) (toDownload.getFileLength()- wrote);             		
             	}
             	
             	wrote += inBlockBuffer;
        		outputStream.write(output, 0, inBlockBuffer);
        }while(inBlockBuffer == BLOCKSIZE);

        outputStream.close();
        inputStream.close();
	}
	public void decryptFileHash(InputStream inputStream, OutputStream outputStream,FEntry toDownload) throws IOException{
		int BLOCKSIZE = 0x10000;
		int HASHBLOCKSIZE = 0xFC00;
		
		long writeSize = HASHBLOCKSIZE;		
		long block = (toDownload.getFileOffset() / HASHBLOCKSIZE) & 0xF;		
		long soffset = toDownload.getFileOffset() - (toDownload.getFileOffset() / HASHBLOCKSIZE * HASHBLOCKSIZE);	
		long size =  toDownload.getFileLength();
		
		if( soffset+size > writeSize )
			writeSize = writeSize - soffset;
	    
	    byte[] encryptedBlockBuffer = new byte[BLOCKSIZE];
	    ByteArrayBuffer overflow = new ByteArrayBuffer(BLOCKSIZE);
	
	    long wrote = 0;
	    int inBlockBuffer;
    	do{        	
    		inBlockBuffer = getChunkFromStream(inputStream,encryptedBlockBuffer,overflow,BLOCKSIZE);
				
			if( writeSize > size )
				writeSize = size;
			
			byte[] output = decryptFileChunkHash(encryptedBlockBuffer, BLOCKSIZE, (int) block,toDownload.getContentID());
	     	
	     	if((wrote + writeSize) > toDownload.getFileLength()){             		
	     		writeSize = (int) (toDownload.getFileLength()- wrote);
	     	}
	                 
			outputStream.write(output, (int)(0+soffset), (int)writeSize);
			wrote +=writeSize;
			
			block++;
			if( block >= 16 )
					block = 0;
			
			if( soffset > 0)
			{
				writeSize = HASHBLOCKSIZE;
				soffset = 0;
			} 	
		}while(wrote < toDownload.getFileLength() && (inBlockBuffer == BLOCKSIZE));
		
		outputStream.close();
		inputStream.close();
		
	}
	
	public void decrypt(FEntry fileEntry,String outputPath) throws IOException {
		String [] path = fileEntry.getFullPath().split("/");
		boolean decryptWithHash = false;
		if(!path[1].equals("code") && fileEntry.isExtractWithHash()){
			decryptWithHash = true;
		}
		
		long fileOffset = fileEntry.getFileOffset();
		if(decryptWithHash){
			int BLOCKSIZE = 0x10000;
			int HASHBLOCKSIZE = 0xFC00;
			fileOffset = ((fileEntry.getFileOffset() / HASHBLOCKSIZE) * BLOCKSIZE);
		}		
		
	    
	    InputStream input = new FileInputStream(fileEntry.getContentPath());
	    FileOutputStream outputStream = new FileOutputStream(outputPath + "/" + fileEntry.getFileName());
	    
	    input.skip(fileOffset);
	    
	    if(!decryptWithHash){
	    	decryptFile(input, outputStream, fileEntry);
	    }else{
	    	decryptFileHash(input, outputStream, fileEntry);
	    }		
	}

	
	private int getChunkFromStream(InputStream inputStream,byte[] output, ByteArrayBuffer overflowbuffer,int BLOCKSIZE) throws IOException {
		int bytesRead = -1;
    	int inBlockBuffer = 0;
    	do{
    		bytesRead = inputStream.read(overflowbuffer.buffer,overflowbuffer.getLengthOfDataInBuffer(),overflowbuffer.getSpaceLeft());    		
    		if(bytesRead <= 0) break;

    		overflowbuffer.addLengthOfDataInBuffer(bytesRead);
	    	
	    	if(inBlockBuffer + overflowbuffer.getLengthOfDataInBuffer() > BLOCKSIZE){
	    		int tooMuch = (inBlockBuffer + bytesRead) - BLOCKSIZE;
	    		int toRead = BLOCKSIZE - inBlockBuffer;
	    		
	    		System.arraycopy(overflowbuffer.buffer, 0, output, inBlockBuffer, toRead);
	    		inBlockBuffer += toRead;
	    		
	    		System.arraycopy(overflowbuffer.buffer, toRead, overflowbuffer.buffer, 0, tooMuch);
	    		overflowbuffer.setLengthOfDataInBuffer(tooMuch);
	    	}else{     
	    		System.arraycopy(overflowbuffer.buffer, 0, output, inBlockBuffer, overflowbuffer.getLengthOfDataInBuffer()); 
	    		inBlockBuffer +=overflowbuffer.getLengthOfDataInBuffer();
	    		overflowbuffer.resetLengthOfDataInBuffer();
	    	}
    	}while(inBlockBuffer != BLOCKSIZE);
    	return inBlockBuffer;
	}
	
	
}
