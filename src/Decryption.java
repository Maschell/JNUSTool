import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

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
			this.decryptedKey = decryptedKey;
			init(titleId);
			
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		try {
			this.decryptedKey = decryptedKey;			
			SecretKeySpec secretKeySpec = new SecretKeySpec(decryptedKey, "AES");
			cipher2.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(iv));					
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public byte[] decrypt(byte[] input){
		try {
			return cipher2.doFinal(input);			
		} catch (IllegalBlockSizeException | BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		if(block == 0){
			
			hash[1] ^= contentID;
			
		}
		if(Arrays.equals(hash, h0)){
			//System.out.println("checksum right");
		}
		else{
			System.out.println("checksum failed");
			System.out.println(Util.ByteArrayToString(hash));
			System.out.println(Util.ByteArrayToString(h0));
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
	

	
	
	
	
}
