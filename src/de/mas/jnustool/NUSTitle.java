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
		if(version > 0 && Settings.DL_ALL_VERSIONS){
			result += "." + version;
		}
		return result;
	}
	
	public NUSTitle(long titleId,int version, String key) {
		setVersion(version);
		setTitleID(titleId);
		if(version != -1){
			Logger.log("Version " + version);
		}
			
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
					try{
						tmd = new TitleMetaData(Downloader.getInstance().downloadTMDToByteArray(titleId,this.version));
					}catch(IllegalArgumentException e){
						Logger.log("TMD wrong. Title not found");
						setTitleID(0);
						return;
					}
					
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
			
			decryption.init(ticket.getDecryptedKey(),0);
			byte[] decryptedFST = decryption.decrypt(encryptedFST);
			
			try{
				fst = new FST(decryptedFST,tmd);
			}catch(Exception e){
				e.printStackTrace();
			}
			tmd.setNUSTitle(this);
			
			setTargetPath(String.format("%016X", getTitleID()));			
			setLongNameFolder(String.format("%016X", getTitleID()));			
			
			if(fst != null && fst.metaFENtry != null){
				byte[] metaxml = fst.metaFENtry.downloadAsByteArray();
				if(metaxml != null){
					try{
						InputStream bis = new ByteArrayInputStream(metaxml);
						NUSTitleInformation nusinfo = readMeta(bis);
						if(nusinfo != null){
							//String folder = nusinfo.getLongnameEN().replaceAll("[^\\x20-\\x7E]", "") + " [" + nusinfo.getID6() + "]";
							String folder = nusinfo.getLongnameEN() + " [" + nusinfo.getID6() + "]";
							String subfolder = "";
							if(tmd.isUpdate()) subfolder = "/" + "updates" + "/" + "v" + tmd.titleVersion;				
							setTargetPath(folder + subfolder);
							setLongNameFolder(folder);
						}
					}catch(Exception e){
						e.printStackTrace();
					}
				}
			}
						
	        if(Settings.downloadContent){
				downloadEncryptedFiles(null);
	        }
			
			Logger.log("Total Size of Content Files: " + ((int)((getTotalContentSize()/1024.0/1024.0)*100))/100.0 +" MB");
			if(fst != null)Logger.log("Total Size of Decrypted Files: " + ((int)((fst.getTotalContentSizeInNUS()/1024.0/1024.0)*100))/100.0 +" MB");
			if(fst != null)Logger.log("Entries: " + fst.getTotalEntries());
			if(fst != null)Logger.log("Files: " + fst.getFileCount());
			if(fst != null)Logger.log("Files in NUSTitle: " + fst.getFileCountInNUS());
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void downloadEncryptedFiles(Progress progress) throws IOException {		
		Util.createSubfolder(getContentPath());
		
		Logger.log("---Downloading encrypted files---");
		Downloader.getInstance().downloadTMD(titleID,version,getContentPath());
		Logger.log("Downloaded title.tmd");
		tmd.downloadContents(progress);
		Logger.log("Downloaded content files");
		
	    File f = new File(getContentPath() + "/" + "title.tik");			    
        if(!f.exists()){
            try{
                Downloader.getInstance().downloadTicket(titleID,getContentPath());
                Logger.log("Downloaded title.tik");
            }catch(Exception e){
                Logger.log("!!!Missing file: title.tik. You need to add it manually before you can install this title.!!!");
            }
        }else{
            Logger.log("Skipped title.tik");
        }
     
        f = new File(getContentPath() + "/" + "title.tik");
        byte[] defaultcert = null;
        
        Logger.log("Trying to create title.cert");
        if(!f.exists()){
            try{
            defaultcert = Util.getDefaultCert();
            }catch(Exception e){
                Logger.log("Failed to get missing cert from OSv10 cetk =(. Couldn't create title.cert");
                e.printStackTrace();
                return;
            }
            Logger.log("Got missing cert from OSv10 title");            
        }else{
            defaultcert = ticket.cert1;
        }
        
        try{ 
            FileOutputStream fos = new FileOutputStream(getContentPath() + "/title.cert");
            fos.write(tmd.cert1);
            fos.write(tmd.cert2);
            fos.write(defaultcert);
            fos.close();
            if(version > 0 && Settings.DL_ALL_VERSIONS){            
                fos = new FileOutputStream(getContentPath() + "/title.cert." + version);        
                fos.write(tmd.cert1);
                fos.write(tmd.cert2);
                fos.write(defaultcert);
                fos.close();
            }			
		}catch(Exception e){
		    e.printStackTrace();
			Logger.log("Error while creating ticket files.");
		}
        Logger.log("Created title.cert");   
        Logger.log("---Successfully downloaded encrypted files---");
	}

	public NUSTitleInformation readMeta(InputStream bis) {
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
	         NUSTitleInformation nusinfo = new NUSTitleInformation(Util.StringToLong(title_id),longname,ID6,proc,content_platform,company_code,(int) Util.StringToLong(region),new String[1]);
	         return nusinfo;
	        
		} catch (ParserConfigurationException | SAXException | IOException e) {
			Logger.log("Error while parsing the meta files");
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
		if(version > 0 && !Settings.DL_ALL_VERSIONS){ //Only add the prefix when we don't download all version of that title			
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
		ForkJoinPool pool = new ForkJoinPool(25);
		List<FEntryDownloader> dlList = new ArrayList<>();
		for(FEntry f : list){
			if(!f.isDir() &&  f.isInNUSTitle()){                    			
				dlList.add(new FEntryDownloader(f,progress));
			}
		}
		pool.invokeAll(dlList);
		if(tmd.isUpdate()) Util.buildFileList(getTargetPath(),"content",Settings.FILELIST_NAME);
		Logger.log("Done!");
	}

	public void setTargetPath(String path){
		path = Util.replaceCharsInString(path);
		this.targetPath =  path;
	}
	
	public String getTargetPath() {		
		return this.targetPath;
	}

	public String getLongNameFolder() {		
		return longNameFolder;
	}
	
	public void setLongNameFolder(String path) {
		path = Util.replaceCharsInString(path);
		longNameFolder  = path;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}
}
