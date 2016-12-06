package de.mas.jnustool.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.SynchronousQueue;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.omg.Messaging.SyncScopeHelper;

import de.mas.jnustool.Content;
import de.mas.jnustool.FEntry;
import de.mas.jnustool.Logger;
import de.mas.jnustool.Progress;
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
	
	public byte[] decryptFileChunkHash(byte[] blockBuffer, int block, int contentID,byte[] h3_hashes){
		IV = ByteBuffer.allocate(16).putShort((short) contentID).array();
		    
     	byte[] hashes = decryptFileChunk(blockBuffer,0x0400,IV);
     	
     	hashes[1] ^= (byte)contentID;
     	
     	//System.out.println("block : " + String.format("%04d", block) +":" +Util.ByteArrayToString(hashes));
     	
     	int H0_start = (block % 16) * 20;
     	int H1_start = (16 + (block / 16) % 16) * 20;
        int H2_start = (32 + (block / 256) % 16) * 20;
        int H3_start = ((block / 4096) % 16) * 20;
     	
     	IV = Arrays.copyOfRange(hashes,H0_start,H0_start + 16); 
     	
		byte[] output =  decryptFileChunk(blockBuffer,0x400,0xFC00,IV);
		
		byte[] real_h0_hash = HashUtil.hashSHA1(output);
		byte[] expected_h0_hash = Arrays.copyOfRange(hashes,H0_start,H0_start + 20);

		if(!Arrays.equals(real_h0_hash,expected_h0_hash)){			
			System.out.println("h0 checksum failed");
			System.out.println("real hash    :" + Util.ByteArrayToString(real_h0_hash));
			System.out.println("expected hash:" + Util.ByteArrayToString(expected_h0_hash));
			System.exit(2);
			//throw new IllegalArgumentException("h0 checksumfail");			
		}else{
			//System.out.println("h0 checksum right!");
		}
		
		if ((block % 16) == 0){
    		byte[] expected_h1_hash = Arrays.copyOfRange(hashes,H1_start,H1_start + 20);
     		byte[] real_h1_hash = HashUtil.hashSHA1(Arrays.copyOfRange(hashes,H0_start,H0_start + (16*20)));
     		
     		if(!Arrays.equals(expected_h1_hash, real_h1_hash)){            
                System.out.println("h1 checksum failed");
                System.out.println("real hash    :" + Util.ByteArrayToString(real_h1_hash));
                System.out.println("expected hash:" + Util.ByteArrayToString(expected_h1_hash));
                System.exit(2);
                //throw new IllegalArgumentException("h1 checksumfail");         
            }else{
                //System.out.println("h1 checksum right!");
            }
		}
		
		if ((block % 256) == 0){
            byte[] expected_h2_hash = Arrays.copyOfRange(hashes,H2_start,H2_start + 20);
            byte[] real_h2_hash = HashUtil.hashSHA1(Arrays.copyOfRange(hashes,H1_start,H1_start + (16*20)));
           
            if(!Arrays.equals(expected_h2_hash, real_h2_hash)){            
                System.out.println("h2 checksum failed");
                System.out.println("real hash    :" + Util.ByteArrayToString(real_h2_hash));
                System.out.println("expected hash:" + Util.ByteArrayToString(expected_h2_hash));
                System.exit(2);
                //throw new IllegalArgumentException("h2 checksumfail");
                
            }else{
                //System.out.println("h2 checksum right!");
            }
        }
		
		if ((block % 4096) == 0){
            byte[] expected_h3_hash = Arrays.copyOfRange(h3_hashes,H3_start,H3_start + 20);
            byte[] real_h3_hash = HashUtil.hashSHA1(Arrays.copyOfRange(hashes,H2_start,H2_start + (16*20)));

            if(!Arrays.equals(expected_h3_hash, real_h3_hash)){            
                System.out.println("h3 checksum failed");
                System.out.println("real hash    :" + Util.ByteArrayToString(real_h3_hash));
                System.out.println("expected hash:" + Util.ByteArrayToString(expected_h3_hash));
                System.exit(2);
                //throw new IllegalArgumentException("h3 checksumfail");         
            }else{
                //System.out.println("h3 checksum right!");
            }
        }
 		
		return output;
	}

	public boolean decryptFile(InputStream inputStream, OutputStream outputStream,FEntry toDownload) throws IOException{
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
        if(progressListener != null){
        	progressListener.setTotal(toDownload.getFileLength());
        	progressListener.resetCurrent();
        }
        
        MessageDigest sha1 = null;
        try {
            sha1 = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        
        do{ 
    		inBlockBuffer = Util.getChunkFromStream(inputStream,blockBuffer,overflow,BLOCKSIZE);
    		if(first){
    			first = false;        			
    		}else{
    			IV = null;
    		}
    		
    		byte[] output = decryptFileChunk(blockBuffer,BLOCKSIZE,IV);
    		
         	
         	if((wrote + inBlockBuffer) > toDownload.getFileLength()){             		
         		inBlockBuffer = (int) (toDownload.getFileLength()- wrote);             		
         	}
         	 if(progressListener != null){
         		progressListener.addCurrent(inBlockBuffer);
         	 }
         	wrote += inBlockBuffer;
    		outputStream.write(output,0,inBlockBuffer);
    		if(sha1 != null){
                sha1.update(output,0,inBlockBuffer);
            }
        }while(inBlockBuffer == BLOCKSIZE);
        
        long missingInHash =  toDownload.getContent().getSize() - wrote;
        
        if(missingInHash > 0){
            sha1.update(new byte[(int) missingInHash]);
        }
        
        byte[] hash = sha1.digest();
        byte[] real_hash = toDownload.getHash();
        
        boolean result = true;
        if(!Arrays.equals(hash, real_hash)){           
            Logger.messageBox("Checksum fail for: " + toDownload.getFileName() + " =(. Content " + String.format("%08X.app", toDownload.getContentID()) + " likely is broken. Please re-download it!");System.out.println(Util.ByteArrayToString(hash));
            System.out.println("Expected hash:  " +Util.ByteArrayToString(hash));
            System.out.println("Real hash    :  " +Util.ByteArrayToString(real_hash));
            System.exit(-1);
            //throw new IllegalArgumentException("Checksum fail for: " + toDownload.getFileName());
        }else{
            Logger.log("Checksum okay for: " + toDownload.getFileName());
        }
        
        outputStream.close();
        inputStream.close();
        return result;
	}
	public boolean decryptFileHash(InputStream inputStream, OutputStream outputStream,FEntry toDownload,byte[] h3) throws IOException{
		int BLOCKSIZE = 0x10000;
		int HASHBLOCKSIZE = 0xFC00;
		
		long writeSize = HASHBLOCKSIZE;		
		long block = (toDownload.getFileOffset() / HASHBLOCKSIZE);	
		
		long soffset = toDownload.getFileOffset() - (toDownload.getFileOffset() / HASHBLOCKSIZE * HASHBLOCKSIZE);	
		
		long size =  toDownload.getFileLength();
	
		if( soffset+size > writeSize )
			writeSize = writeSize - soffset;
		
		
	    byte[] encryptedBlockBuffer = new byte[BLOCKSIZE];
	    ByteArrayBuffer overflow = new ByteArrayBuffer(BLOCKSIZE);
	
	    long wrote = 0;
	    int inBlockBuffer;
	    
	    if(progressListener != null){
        	progressListener.setTotal(toDownload.getFileLength()/HASHBLOCKSIZE*BLOCKSIZE);
        	progressListener.resetCurrent();
        }
    	do{        	
    		inBlockBuffer = Util.getChunkFromStream(inputStream,encryptedBlockBuffer,overflow,BLOCKSIZE);
    		if(progressListener != null){            	
            	progressListener.addCurrent(inBlockBuffer);
            }
			if( writeSize > size )
				writeSize = size;
			
			byte[] output = decryptFileChunkHash(encryptedBlockBuffer, (int) block,toDownload.getContentID(),h3);
	     	
	     	if((wrote + writeSize) > toDownload.getFileLength()){             		
	     		writeSize = (int) (toDownload.getFileLength()- wrote);
	     	}
	                 
			outputStream.write(output, (int)(0+soffset), (int)writeSize);
			wrote +=writeSize;
			
			block++;			
			
			if( soffset > 0)
			{
				writeSize = HASHBLOCKSIZE;
				soffset = 0;
			} 	
		}while(wrote < toDownload.getFileLength() && (inBlockBuffer == BLOCKSIZE));
		
		outputStream.close();
		inputStream.close();
		return true;
		
	}
	
	public boolean decryptContentHash(InputStream inputStream, OutputStream outputStream,Content content,byte[] h3) throws IOException{
        int BLOCKSIZE = 0x10000;
        int HASHBLOCKSIZE = 0xFC00;
        
        long writeSize = HASHBLOCKSIZE;     
        long block = 0;  
        
        long soffset = 0;   
        
        long size =  content.getSize()/0x10000*0xfc00;
    
        if( soffset+size > writeSize )
            writeSize = writeSize - soffset;
        
        
        byte[] encryptedBlockBuffer = new byte[BLOCKSIZE];
        ByteArrayBuffer overflow = new ByteArrayBuffer(BLOCKSIZE);
    
        long wrote = 0;
        int inBlockBuffer;
        
        do{         
            inBlockBuffer = Util.getChunkFromStream(inputStream,encryptedBlockBuffer,overflow,BLOCKSIZE);
            if(progressListener != null){               
                progressListener.addCurrent(inBlockBuffer);
            }
            if( writeSize > size )
                writeSize = size;
            
            byte[] output = decryptFileChunkHash(encryptedBlockBuffer, (int) block,content.getContentID(),h3);
            
            if((wrote + writeSize) > size){                   
                writeSize = (int) (size - wrote);
            }
                     
            outputStream.write(output, (int)(0+soffset), (int)writeSize);
            wrote +=writeSize;
            
            block++; 
            
            if( soffset > 0)
            {
                writeSize = HASHBLOCKSIZE;
                soffset = 0;
            }   
        }while(wrote < size && (inBlockBuffer == BLOCKSIZE));
        
        outputStream.close();
        inputStream.close();
        return true;
        
    }
	
	
	public byte[] decryptAsByte(FEntry fileEntry,String outputPath) throws IOException {
        InputStream input = new FileInputStream(fileEntry.getContentPath());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        
        decryptStream(input,bos,fileEntry);
        
        byte[] result = bos.toByteArray();
        bos.close();
        return result;
    }
	
	public byte[] decryptContent(FEntry fileEntry,String outputPath) throws IOException {
        InputStream input = new FileInputStream(fileEntry.getContentPath());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        
        decryptStream(input,bos,fileEntry);
        
        byte[] result = bos.toByteArray();
        bos.close();
        return result;
    }
    
	
	public void decrypt(FEntry fileEntry,String outputPath) throws IOException {
	   
	    InputStream input = new FileInputStream(fileEntry.getContentPath());
	    
	    FileOutputStream outputStream = new FileOutputStream(outputPath + "/" + fileEntry.getFileName());
	    decryptStream(input,outputStream,fileEntry);
	}
	
	public void decryptStream(InputStream input,OutputStream outputStream,FEntry fileEntry) throws IOException {
        //String [] path = fileEntry.getFullPath().split("/");
        boolean decryptWithHash = false;
        if(/*!path[1].equals("code") && */fileEntry.isExtractWithHash()){
            decryptWithHash = true;
        }
        long fileOffset = fileEntry.getFileOffset();
        if(decryptWithHash){
            int BLOCKSIZE = 0x10000;
            int HASHBLOCKSIZE = 0xFC00;
            fileOffset = ((fileEntry.getFileOffset() / HASHBLOCKSIZE) * BLOCKSIZE);
        }       
        input.skip(fileOffset);
        
        boolean result = false;
        if(!decryptWithHash){
            result = decryptFile(input, outputStream, fileEntry);
        }else{
            byte[] h3 = fileEntry.getH3();
            result = decryptFileHash(input, outputStream, fileEntry,h3);
        }
        if(!result){
            
        }
    }
	
	private Progress progressListener = null;

	public void setProgressListener(Progress progressOfFile) {
		this.progressListener = progressOfFile;
		
	}

	/* Checking encrypted files is just a pain. Maybe I'll add it later ~
    public byte[] getHashEncryptedFile(File f, List<FEntry> list) throws IOException {
        if(list == null || list.size() > 1){
            return new byte[0x14];
        }else{
        }
        FEntry fentry = list.get(0);
        String [] path = fentry.getFullPath().split("/");
        boolean decryptWithHash = false;
        if(path.length < 2) return new byte[0x14];
        if(!path[1].equals("code") && fentry.isExtractWithHash()){
            decryptWithHash = true;
        }
        
        long fileOffset = fentry.getFileOffset();
        if(decryptWithHash){
            int BLOCKSIZE = 0x10000;
            int HASHBLOCKSIZE = 0xFC00;
            fileOffset = ((fentry.getFileOffset() / HASHBLOCKSIZE) * BLOCKSIZE);
        }
        
        InputStream input = new FileInputStream(fentry.getContentPath());
        
        input.skip(fileOffset);
        
        byte[] result = new byte[0x14];
        if(!decryptWithHash){
            result = getHashEncryptedFileNormal(input, fentry);
        }else{
            result = getHashEncryptedFileHashed(input, fentry);
        }
        return Arrays.copyOfRange(result,0,0x14);
    }

    private byte[] getHashEncryptedFileHashed(InputStream input, FEntry fileEntry) {
        return fileEntry.getHash(); //Ups.
    }

    private byte[] getHashEncryptedFileNormal(InputStream inputStream, FEntry fileEntry) throws IOException {
        int BLOCKSIZE = 0x8000;

        byte[] IV = new byte[16];
        IV[1] = (byte)fileEntry.getContentID();
     
        byte[] blockBuffer = new byte[BLOCKSIZE];
        
        int inBlockBuffer;  
        
        boolean first = true;
        ByteArrayBuffer overflow = new ByteArrayBuffer(BLOCKSIZE);
      
        MessageDigest sha1 = null;
        try {
            sha1 = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        
        long proccessed = 0;
        do{ 
            inBlockBuffer = Util.getChunkFromStream(inputStream,blockBuffer,overflow,BLOCKSIZE);
            if(first){
                first = false;                  
            }else{
                IV = null;
            }
            
            byte[] output = decryptFileChunk(blockBuffer,BLOCKSIZE,IV);
            
            if(sha1 != null){
                sha1.update(output);
            }else{
                break;
            }
            proccessed += inBlockBuffer;
        }while(proccessed < fileEntry.getFileLength() && inBlockBuffer == BLOCKSIZE);
        
        byte[] hash = new byte[0x14];
        if(sha1 != null){
            hash = sha1.digest();
        }
        
        inputStream.close();
        return hash;
    }
    */
	
	
}
