package de.mas.jnustool;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import de.mas.jnustool.util.Decryption;
import de.mas.jnustool.util.Downloader;
import de.mas.jnustool.util.NUSTitleInformation;
import de.mas.jnustool.util.Settings;
import de.mas.jnustool.util.Util;

public class NUSTitle {
	private TitleMetaData tmd;
	private TIK ticket;
	private FST fst;
	private long titleID;
	private String targetPath = new String();
	private String longNameFolder = new String();
	private int version = -1;
	
	private String getTMDName(){
		String result = "title.tmd";
		
		return result;
	}
	
	public NUSTitle(long titleId,int version, String key) {		
		setVersion(version);
		setTitleID(titleId);
		try {
			/*
			if(Settings.downloadContent){
				File f  = new File(getContentPath());
				if(!f.exists())f.mkdir();
			}
			
			if(Settings.downloadContent){
				
				File f = new File(getContentPath() + "/" + getTMDName());
				if(!(f.exists() && Settings.skipExistingTMDTICKET)){				
					Logger.log("Downloading TMD");
					Downloader.getInstance().downloadTMD(titleId,getContentPath());
				}else{
					Logger.log("Skipped download of TMD. Already existing");
				}
				
				f = new File(getContentPath() + "/" + "cetk");
				if(!(f.exists() && Settings.skipExistingTMDTICKET)){	
					if(key == null){
						Logger.log("Downloading Ticket");
						Downloader.getInstance().downloadTicket(titleId,getContentPath());
					}
				}else{
					Logger.log("Skipped download of ticket. Already existing");
				}
			}*/
			
			if(Settings.useCachedFiles){
				File f = new File(getContentPath() + "/" + getTMDName());
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
					tmd = new TitleMetaData(Downloader.getInstance().downloadTMDToByteArray(titleId,this.version));
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
					File f = new File(getContentPath() + "/" + "title.tik");
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
			
			/*if(Settings.downloadContent){
				File f = new File(getContentPath() + "/" + String.format("%08x", tmd.contents[0].ID) + ".app");
				if(!(f.exists() && Settings.skipExistingFiles)){
					Logger.log("Downloading FST (" + String.format("%08x", tmd.contents[0].ID) + ")");
					Downloader.getInstance().downloadContent(titleId,tmd.contents[0].ID,getContentPath(),null);
				}else{
					if(f.length() != tmd.contents[0].size){
						if(Settings.downloadWhenCachedFilesMissingOrBroken){
							Logger.log("FST already existing, but broken. Downloading it again.");
							Downloader.getInstance().downloadContent(titleId,tmd.contents[0].ID,getContentPath(),null);
						}else{
							Logger.log("FST already existing, but broken. No download allowed.");
							System.exit(2);
						}	
					}else{
						Logger.log("Skipped download of FST. Already existing");
					}					
				}
			}*/
			
			
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
			
			setTargetPath(String.format("%016X", getTitleID()));
			
			setLongNameFolder(String.format("%016X", getTitleID()));
			
			if(tmd.isUpdate()){
				byte[] metaxml = fst.metaFENtry.downloadAsByteArray();
				if(metaxml != null){
					InputStream bis = new ByteArrayInputStream(metaxml);
					NUSTitleInformation nusinfo = readMeta(bis);
					String folder = nusinfo.getLongnameEN() + " [" + nusinfo.getID6() + "]";
					String subfolder = "/" + "updates" + "/" + "v" + tmd.titleVersion;				
					setTargetPath(folder + subfolder);					
					setLongNameFolder(folder);					
				}
			}
						
	        if(Settings.downloadContent){
				downloadEncryptedFiles(null);
			}
			
			Logger.log("Total Size of Content Files: " + ((int)((getTotalContentSize()/1024.0/1024.0)*100))/100.0 +" MB");
			Logger.log("Total Size of Decrypted Files: " + ((int)((fst.getTotalContentSizeInNUS()/1024.0/1024.0)*100))/100.0 +" MB");
			Logger.log("Entries: " + fst.getTotalEntries());
			Logger.log("Files: " + fst.getFileCount());
			Logger.log("Files in NUSTitle: " + fst.getFileCountInNUS());
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void downloadEncryptedFiles(Progress progress) throws IOException {
		Util.createSubfolder(getContentPath());
		
		Downloader.getInstance().downloadTMD(titleID,version,getContentPath());		
		Downloader.getInstance().downloadTicket(titleID,getContentPath());
		
		tmd.downloadContents(progress);
		
		FileOutputStream fos = new FileOutputStream(getContentPath()  +"/title.cert");		
		fos.write(ticket.cert0);
		fos.write(tmd.cert);
		fos.write(ticket.cert1);
		fos.close();
	}

	NUSTitleInformation readMeta(InputStream bis) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        
        String ID6 = null;
		try {
			builder = factory.newDocumentBuilder();
			Document document = builder.parse(bis);
	         String proc = document.getElementsByTagName("product_code").item(0).getTextContent().toString();
	         String comp = document.getElementsByTagName("company_code").item(0).getTextContent().toString();
	         String title_id = document.getElementsByTagName("title_id").item(0).getTextContent().toString();
	         
	         String longname = document.getElementsByTagName("longname_en").item(0).getTextContent().toString();
	         longname = longname.replace("\n", " ");
	         String id = proc.substring(proc.length()-4, proc.length());
	         comp = comp.substring(comp.length()-2, comp.length());
	         ID6 = id+comp;
	         String  company_code = document.getElementsByTagName("company_code").item(0).getTextContent().toString();
	         String content_platform = document.getElementsByTagName("content_platform").item(0).getTextContent().toString();
	         String region = document.getElementsByTagName("region").item(0).getTextContent().toString();
	         NUSTitleInformation nusinfo = new NUSTitleInformation(Util.StringToLong(title_id),longname,ID6,proc,content_platform,company_code,Integer.parseInt(region),new String[1]);
	         return nusinfo;
	        
		} catch (ParserConfigurationException | SAXException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();	        		
		}
		return null;	
		
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
		String result = getContentPathPrefix() + String.format("%016X", getTitleID());
		if(version > 0){
			result += "_v" + version;
		}
		return result;
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

	public void decryptFEntries(List<FEntry> list,Progress progress) {
		Util.createSubfolder(getTargetPath());
		//progress = null;
		ForkJoinPool pool = ForkJoinPool.commonPool();
		List<FEntryDownloader> dlList = new ArrayList<>();
		for(FEntry f : list){
			if(!f.isDir() &&  f.isInNUSTitle()){                    			
				dlList.add(new FEntryDownloader(f,progress));
			}
		}
		pool.invokeAll(dlList);
		Logger.log("Done!");
	}
	
	public void setTargetPath(String path){
		path = path.replaceAll("[:\\\\*?|<>]", "");
		this.targetPath =  path;
	}
	
	public String getTargetPath() {		
		return this.targetPath;
	}

	public String getLongNameFolder() {		
		return longNameFolder;
	}
	
	public void setLongNameFolder(String path) {
		path = path.replaceAll("[:\\\\*?|<>]", "");
		longNameFolder  = path;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	

}
