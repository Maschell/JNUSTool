package de.mas.jnustool;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import de.mas.jnustool.util.Decryption;
import de.mas.jnustool.util.Util;

public class TIK {
		public static int KEY_LENGTH = 16;
		private byte[] encryptedKey = new byte[16];		
		private byte[] decryptedKey = new byte[16];
		
		public TIK(File cetk,long titleid) throws IOException{
			parse(cetk);
			calculateDecryptedKey(titleid);
		}

		public TIK(String ticketKey,long titleid) {			
			setEncryptedKey(ticketKey);
			calculateDecryptedKey(titleid);
		}
		

		public TIK(byte[] file, long titleID) throws IOException {
			parse(file);
			calculateDecryptedKey(titleID);
		}

		private void calculateDecryptedKey(long titleid) {
			Decryption decryption = new Decryption(Util.commonKey,titleid);
			decryptedKey = decryption.decrypt(encryptedKey);	
		}

		private void parse(byte[] cetk) throws IOException {			
			System.arraycopy(cetk, 0x1bf, this.encryptedKey, 0,16);
		}
		
		private void parse(File cetk) throws IOException {			
			RandomAccessFile f = new RandomAccessFile(cetk, "r");		
			f.seek(0x1bf);
			f.read(this.encryptedKey, 0, 16);
			f.close();
		}
						
		public void setEncryptedKey(String key) {			
			this.encryptedKey = Util.hexStringToByteArray(key);
		}
		
		public byte[] getEncryptedKey() {
			return encryptedKey;
		}

		public void setEncryptedKey(byte[] encryptedKey) {
			this.encryptedKey = encryptedKey;
		}

		public byte[] getDecryptedKey() {
			return decryptedKey;
		}

		public void setDecryptedKey(byte[] decryptedKey) {
			this.decryptedKey = decryptedKey;
		}

		@Override
		public String toString(){		
			return "encrypted key: " +  Util.ByteArrayToString(encryptedKey)+ " decrypted key: " +  Util.ByteArrayToString(decryptedKey); 
		}
}
