package de.mas.jnustool;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

import de.mas.jnustool.util.Decryption;
import de.mas.jnustool.util.Downloader;
import de.mas.jnustool.util.Settings;

public class NUSTitle {
	private TitleMetaData tmd;
	private TIK ticket;
	private FST fst;
	private long titleID;
	public NUSTitle(long titleId,String key) {
		setTitleID(titleId);
		try {			
			if(Settings.downloadContent){
				File f  = new File(getContentPath());
				if(!f.exists())f.mkdir();
			}
			
			if(Settings.downloadContent){
				
				File f = new File(getContentPath() + "/" + "tmd");
				if(!(f.exists() && Settings.skipExistingTMDTICKET)){				
					Logger.log("Downloading TMD");
					Downloader.getInstance().downloadTMD(titleId,getContentPath());
				}else{
					Logger.log("Skipped download of TMD. Already existing");
				}
				f = new File(getContentPath() + "/" + "cetk");
				if(!(f.exists() && Settings.skipExistingTMDTICKET)){	
					if(key == null){
						System.out.print("Downloading Ticket");
						Downloader.getInstance().downloadTicket(titleId,getContentPath());
					}
				}else{
					Logger.log("Skipped download of ticket. Already existing");
				}
			}
			
			if(Settings.useCachedFiles){
				File f = new File(getContentPath() + "/" + "tmd");
				if(f.exists()){
					Logger.log("Using cached TMD.");
					tmd = new TitleMetaData(f);
				}else{
					Logger.log("No cached TMD found.");
				}
			}
			if(tmd == null){
				if(Settings.downloadWhenCachedFilesMissingOrBroken){
					if(Settings.useCachedFiles) Logger.log("Getting missing tmd from Server!");
					tmd = new TitleMetaData(Downloader.getInstance().downloadTMDToByteArray(titleId));
				}else{
					Logger.log("Downloading of missing files is not enabled. Exiting");
					System.exit(2);
				}
			}			
			if(key != null){
				Logger.log("Using ticket from parameter.");
				ticket = new TIK(key,titleId);				
			}else{
				if(Settings.useCachedFiles){
					File f = new File(getContentPath() + "/" + "cetk");
					if(f.exists()){
						Logger.log("Using cached cetk.");
						ticket = new TIK(f,titleId);
					}else{
						Logger.log("No cached ticket found.");
					}
				}
				if(ticket == null){
					if(Settings.downloadWhenCachedFilesMissingOrBroken){
						if(Settings.useCachedFiles) Logger.log("getting missing ticket");
						ticket = new TIK(Downloader.getInstance().downloadTicketToByteArray(titleId),tmd.titleID);
					}else{
						Logger.log("Downloading of missing files is not enabled. Exiting");
						System.exit(2);
					}
				}
			}
			
			if(Settings.downloadContent){
				File f = new File(getContentPath() + "/" + String.format("%08x", tmd.contents[0].ID) + ".app");
				if(!(f.exists() && Settings.skipExistingFiles)){
					Logger.log("Downloading FST (" + String.format("%08x", tmd.contents[0].ID) + ")");
					Downloader.getInstance().downloadContent(titleId,tmd.contents[0].ID,getContentPath());
				}else{
					if(f.length() != tmd.contents[0].size){
						if(Settings.downloadWhenCachedFilesMissingOrBroken){
							Logger.log("FST already existing, but broken. Downloading it again.");
							Downloader.getInstance().downloadContent(titleId,tmd.contents[0].ID,getContentPath());
						}else{
							Logger.log("FST already existing, but broken. No download allowed.");
							System.exit(2);
						}	
					}else{
						Logger.log("Skipped download of FST. Already existing");
					}
					
				}
				
			}
			
			
			Decryption decryption = new Decryption(ticket.getDecryptedKey(),0);
			byte[] encryptedFST = null;
			if(Settings.useCachedFiles){
				Logger.log(getContentPath());
				String path = getContentPath() + "/" + String.format("%08x", tmd.contents[0].ID) + ".app";
				File f = new File(path);				
				if(f.exists()){
					Logger.log("Using cached FST");
					Path file = Paths.get(path);
					encryptedFST = Files.readAllBytes(file);
				}else{
					Logger.log("No cached FST (" + String.format("%08x", tmd.contents[0].ID) +  ") found.");
				}	
			}
			if(encryptedFST == null){
				if(Settings.downloadWhenCachedFilesMissingOrBroken){
					if(Settings.useCachedFiles)Logger.log("Getting FST from server.");
					encryptedFST = Downloader.getInstance().downloadContentToByteArray(titleId,tmd.contents[0].ID);
				}else{
					Logger.log("Downloading of missing files is not enabled. Exiting");
					System.exit(2);
				}
			}
			
			
			decryption.init(ticket.getDecryptedKey(), 0);
			byte[] decryptedFST = decryption.decrypt(encryptedFST);
			
			fst = new FST(decryptedFST,tmd);
			tmd.setNUSTitle(this);
			
			if(Settings.downloadContent){
				tmd.downloadContents();
			}
			
			Logger.log("Total Size of Content Files: " + ((int)((getTotalContentSize()/1024.0/1024.0)*100))/100.0 +" MB");
			Logger.log("Total Size of Decrypted Files: " + ((int)((fst.getTotalContentSizeInNUS()/1024.0/1024.0)*100))/100.0 +" MB");
			Logger.log("Entries: " + fst.getTotalEntries());
			Logger.log("Entries: " + fst.getFileCount());
			Logger.log("Files in NUSTitle: " + fst.getFileCountInNUS());
			
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



	public long getTitleID() {
		return titleID;
	}
	
	private void setTitleID(long titleId) {
		this.titleID = titleId;		
	}

	public void decryptFEntries(List<FEntry> list) {
		ForkJoinPool pool = ForkJoinPool.commonPool();
		List<FEntryDownloader> dlList = new ArrayList<>();
		for(FEntry f : list){
			if(!f.isDir() &&  f.isInNUSTitle()){                    			
				dlList.add(new FEntryDownloader(f));
			}
		}
		
		pool.invokeAll(dlList);
		Logger.log("Done!");
	}

	

}
