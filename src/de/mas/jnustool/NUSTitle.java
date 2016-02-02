package de.mas.jnustool;

import java.io.IOException;

import de.mas.jnustool.util.Decryption;
import de.mas.jnustool.util.Downloader;
import de.mas.jnustool.util.Util;

public class NUSTitle {
	private TitleMetaData tmd;
	private long titleID;
	private TIK ticket;
	private FST fst;
	public NUSTitle(long titleId,String key){
		
		try {
			titleID =  titleId;
			Downloader.getInstance().titleID = titleId;
			Decryption decryption = new Decryption(Util.commonKey,titleId);
			
			tmd = new TitleMetaData(Downloader.getInstance().downloadTMDToByteArray());			
				
			if(key == null){	
				ticket = new TIK(Downloader.getInstance().downloadTicketToByteArray(),tmd.titleID);
			}else{
				ticket = new TIK(key,titleId);
			}
			
			
			Downloader.getInstance().ticket = ticket;
			decryption.init(ticket.getDecryptedKey(),0);
									
			byte[] encryptedFST = Downloader.getInstance().downloadContentToByteArray(tmd.contents[0].ID);
			byte[] decryptedFST = decryption.decrypt(encryptedFST);
			
			fst = new FST(decryptedFST,tmd);
			tmd.setFst(fst);
			
			System.out.println("Total Size of Content Files: " + ((int)((getTotalContentSize()/1024.0/1024.0)*100))/100.0 +" MB");
			System.out.println("Total Size of Decrypted Files: " + ((int)((fst.getTotalContentSizeInNUS()/1024.0/1024.0)*100))/100.0 +" MB");
			System.out.println("Entries: " + fst.getTotalEntries());
			System.out.println("Entries: " + fst.getFileCount());
			System.out.println("Files in NUSTitle: " + fst.getFileCountInNUS());
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public FST getFst() {
		return fst;
	}


	public void setFst(FST fst) {
		this.fst = fst;
	}


	public long getTitleID() {
		return titleID;
	}

	public void setTitleID(long titleID) {
		this.titleID = titleID;
	}

	public long getTotalContentSize() {
		return tmd.getTotalContentSize();
	}

}
