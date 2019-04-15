package de.mas.wiiu.jnus.jnustool;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

import de.mas.wiiu.jnus.jnustool.util.Decryption;
import de.mas.wiiu.jnus.jnustool.util.Util;

public class TIK {
		public static int KEY_LENGTH = 16;
		private byte[] encryptedKey = new byte[16];		
		private byte[] decryptedKey = new byte[16];
		
		public byte[] cert0 = new byte[0x400];
		public byte[] cert1 = new byte[0x300];
		
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
			if(cetk != null){
				System.arraycopy(cetk, 0x1bf, this.encryptedKey, 0,16);
				if(cetk.length >= 0x650 + 0x400){
					cert0 = Arrays.copyOfRange(cetk, 0x650, 0x650 + 0x400);
					cert1 = Arrays.copyOfRange(cetk, 0x350, 0x350 + 0x300);
				}else{
					Logger.log("No certs for TIK found. File too short!");
				}
			}
			
		}
		
		private void parse(File cetk) throws IOException {			
			RandomAccessFile f = new RandomAccessFile(cetk, "r");		
			f.seek(0x1bf);
			f.read(this.encryptedKey, 0, 16);	
			
			f.seek(0x650);			
			f.read(cert0, 0, 0x400);
			f.seek(0x350);
			f.read(cert1, 0, 0x300);
			
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
			String result = "Encrypted key: " +  Util.ByteArrayToString(encryptedKey)+ " Decrypted key: " +  Util.ByteArrayToString(decryptedKey) + "\n"; 
			result += "cert0:" + Util.ByteArrayToString(cert0) + "\n";
			result += "cert1:" + Util.ByteArrayToString(cert1) + "\n";
			return result;
		}
}
