package de.mas.jnustool;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import de.mas.jnustool.util.Decryption;
import de.mas.jnustool.util.Downloader;
import de.mas.jnustool.util.ExitException;
import de.mas.jnustool.util.Settings;

public class NUSTitle {
	private TitleMetaData tmd;
	private TIK ticket;
	private FST fst;
	private long titleID;
	public NUSTitle(long titleId,String key) throws ExitException{
		setTitleID(titleId);
		try {
			if(Settings.downloadContent){
				File f  = new File(getContentPath());
				if(!f.exists())f.mkdir();
			}
			if(Settings.downloadContent){
				
				File f = new File(getContentPath() + "/" + "tmd");
				if(!(f.exists() && Settings.skipExistingTMDTICKET)){				
					System.out.println("Downloading TMD");
					Downloader.getInstance().downloadTMD(titleId,getContentPath());
				}else{
					System.out.println("Skipped download of TMD. Already existing");
				}
				f = new File(getContentPath() + "/" + "cetk");
				if(!(f.exists() && Settings.skipExistingTMDTICKET)){	
					if(key == null){
						System.out.print("Downloading Ticket");
						Downloader.getInstance().downloadTicket(titleId,getContentPath());
					}
				}else{
					System.out.println("Skipped download of ticket. Already existing");
				}
			}
			
			if(Settings.useCachedFiles){
				File f = new File(getContentPath() + "/" + "tmd");
				if(f.exists()){
					System.out.println("Using cached TMD.");
					tmd = new TitleMetaData(f);
				}else{
					System.out.println("No cached TMD found.");
				}
			}
			
			if(tmd == null){
				if(Settings.downloadWhenCachedFilesMissingOrBroken){
					if(Settings.useCachedFiles) System.out.println("Getting missing tmd from Server!");
					tmd = new TitleMetaData(Downloader.getInstance().downloadTMDToByteArray(titleId));
				}else{
					System.out.println("Downloading of missing files is not enabled. Exiting");
					throw new ExitException("TMD missing.");
				}
			}			
				
			if(key != null){
				System.out.println("Using ticket from parameter.");
				ticket = new TIK(key,titleId);				
			}else{
				if(Settings.useCachedFiles){
					File f = new File(getContentPath() + "/" + "cetk");
					if(f.exists()){
						System.out.println("Using cached cetk.");
						ticket = new TIK(f,titleId);
					}else{
						System.out.println("No cached ticket found.");
					}
				}
				if(ticket == null){
					if(Settings.downloadWhenCachedFilesMissingOrBroken){
						if(Settings.useCachedFiles) System.out.println("getting missing ticket");
						ticket = new TIK(Downloader.getInstance().downloadTicketToByteArray(titleId),tmd.titleID);
					}else{
						System.out.println("Downloading of missing files is not enabled. Exiting");
						throw new ExitException("Ticket missing.");
					}
				}
			}
			
			if(Settings.downloadContent){
				File f = new File(getContentPath() + "/" + String.format("%08x", tmd.contents[0].ID) + ".app");
				if(!(f.exists() && Settings.skipExistingFiles)){
					System.out.println("Downloading FST (" + String.format("%08x", tmd.contents[0].ID) + ")");
					Downloader.getInstance().downloadContent(titleId,tmd.contents[0].ID,getContentPath());
				}else{
					if(f.length() != tmd.contents[0].size){
						if(Settings.downloadWhenCachedFilesMissingOrBroken){
							System.out.println("FST already existing, but broken. Downloading it again.");
							Downloader.getInstance().downloadContent(titleId,tmd.contents[0].ID,getContentPath());
						}else{
							System.out.println("FST already existing, but broken. No download allowed.");
							throw new ExitException("FST missing.");
						}	
					}else{
						System.out.println("Skipped download of FST. Already existing");
					}
					
				}
				
			}
			
			Decryption decryption = new Decryption(ticket.getDecryptedKey(),0);
			byte[] encryptedFST = null;
			if(Settings.useCachedFiles){
				String path = getContentPath() + "/" + String.format("%08x", tmd.contents[0].ID) + ".app";
				File f = new File(path);				
				if(f.exists()){
					System.out.println("Using cached FST");
					Path file = Paths.get(path);
					encryptedFST = Files.readAllBytes(file);
				}else{
					System.out.println("No cached FST (" + String.format("%08x", tmd.contents[0].ID) +  ") found.");
				}
			}
			if(encryptedFST == null){
				if(Settings.downloadWhenCachedFilesMissingOrBroken){
					if(Settings.useCachedFiles)System.out.println("Getting FST from server.");
					encryptedFST = Downloader.getInstance().downloadContentToByteArray(titleId,tmd.contents[0].ID);
				}else{
					System.out.println("Downloading of missing files is not enabled. Exiting");
					throw new ExitException("");
				}
			}			
			byte[] decryptedFST = decryption.decrypt(encryptedFST);
			
			fst = new FST(decryptedFST,tmd);
			tmd.setNUSTitle(this);
			
			if(Settings.downloadContent){
				tmd.downloadContents();
			}
			
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
	

	public TitleMetaData getTmd() {
		return tmd;
	}

	public void setTmd(TitleMetaData tmd) {
		this.tmd = tmd;
	}

	public TIK getTicket() {
		return ticket;
	}



	public void setTicket(TIK ticket) {
		this.ticket = ticket;
	}

	public long getTotalContentSize() {
		return tmd.getTotalContentSize();
	}



	public String getContentPath() {		
		return getContentPathPrefix() + String.format("%016X", getTitleID());
	}
	
	public String getContentPathPrefix() {		
		return "tmp_";
	}



	private long getTitleID() {
		return titleID;
	}
	
	private void setTitleID(long titleId) {
		this.titleID = titleId;		
	}

	

}
