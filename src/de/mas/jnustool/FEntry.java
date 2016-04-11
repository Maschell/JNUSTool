package de.mas.jnustool;

import java.io.File;
import java.io.IOException;
import java.util.List;

import de.mas.jnustool.util.Decryption;
import de.mas.jnustool.util.Downloader;
import de.mas.jnustool.util.Settings;
import de.mas.jnustool.util.Util;

public class FEntry implements IHasName{
	private FST fst;
	
	public static int DIR_FLAG = 1;
	public static int NOT_IN_NUSTITLE_FLAG = 0x80;
	public static int EXTRACT_WITH_HASH_FLAG = 0x440;	
	public static int CHANGE_OFFSET_FLAG = 0x04;
	
	private boolean dir = false;
	private boolean in_nus_title = false;
	private boolean extract_withHash = false;
	
	private String fileName = "";
	private String path = "";
	private long fileOffset = 0L;	
	private long fileLength = 0;
	private int contentID = 0;
	private int NUScontentID = 0;
	private List<String> pathList;

	
	public FEntry(String path, String filename, int contentID,int NUScontentID, long fileOffset, long fileLength, boolean dir,
			boolean in_nus_title, boolean extract_withHash, List<String> pathList,FST fst) {
		setPath(path);
		setFileName(filename);
		setContentID(contentID);
		setFileOffset(fileOffset);
		setFileLength(fileLength);
		setDir(dir);
		setInNusTitle(in_nus_title);
		setExtractWithHash(extract_withHash);
		setNUScontentID(NUScontentID);
		setPathList(pathList);
		this.fst = fst;
	}

	public boolean isDir() {
		return dir;
	}

	private void setDir(boolean dir) {
		this.dir = dir;
	}

	public boolean isInNUSTitle() {
		return in_nus_title;
	}

	private void setInNusTitle(boolean in_nus_title) {
		this.in_nus_title = in_nus_title;
	}

	public boolean isExtractWithHash() {
		return extract_withHash;
	}

	private void setExtractWithHash(boolean extract_withHash) {
		this.extract_withHash = extract_withHash;
	}

	public String getFileName() {
		return fileName;
	}

	private void setFileName(String filename) {
		this.fileName = filename;
	}

	public String getPath() {
		return path;
	}
	
	public String getFullPath() {
		return path + fileName;
	}

	private void setPath(String path) {
		this.path = path;
	}

	public long getFileOffset() {
		return fileOffset;
	}

	private void setFileOffset(long fileOffset) {
		this.fileOffset = fileOffset;
	}

	public int getContentID() {
		return contentID;
	}

	private void setContentID(int contentID) {
		this.contentID = contentID;
	}

	public long getFileLength() {
		return fileLength;
	}

	private void setFileLength(long fileLength) {
		this.fileLength = fileLength;
	}
	
	@Override
	public String toString(){		
		return getFullPath() + " Content ID:" + contentID + " Size: " + fileLength +"MB  Offset: " + fileOffset; 
	}

	public int getNUScontentID() {
		return NUScontentID;
	}

	private void setNUScontentID(int nUScontentID) {
		NUScontentID = nUScontentID;
	}
	
	
	public String getDownloadPath(){
		
		String [] path = getFullPath().split("/");	   
		String folder = getTargetPath() +"/";
	    for(int i = 0;i<path.length-1;i++){
	    	if(!path[i].equals("")){	    		
	    		folder += path[i] + "/";	    		
	    	}	    	
	    }
	    return folder;
	}

	public void downloadAndDecrypt(Progress progress) {
		Util.createSubfolder(fst.getTmd().getNUSTitle().getTargetPath() + getFullPath());
		
		File f = new File(fst.getTmd().getNUSTitle().getTargetPath() + getFullPath());
		if(f.exists()){
			if(f.length() == getFileLength()){
				Logger.log("Skipping: " + String.format("%8.2f MB ",getFileLength()/1024.0/1024.0)  + getFullPath());
				if(progress != null){
					progress.finish();
				}
				return;
			}
		}
		try {
			if(Settings.useCachedFiles){
				f = new File(getContentPath());
				if(f.exists()){				
					if(f.length() == fst.getTmd().contents[this.getContentID()].size){
						Logger.log("Decrypting: " + String.format("%8.2f MB ", getFileLength()/1024.0/1024.0)  + getFullPath());
						Decryption decrypt = new Decryption(fst.getTmd().getNUSTitle().getTicket());
						decrypt.setProgressListener(progress);
						decrypt.decrypt(this,getDownloadPath());
						return;
					}else{
						if(!Settings.downloadWhenCachedFilesMissingOrBroken){
							Logger.log("Cached content has the wrong size! Please check your: "+ getContentPath() + " Downloading not allowed");
							if(!Settings.skipBrokenFiles){
								System.err.println("File broken!");
								System.exit(2);
							}else{
								Logger.log("Ignoring the missing file: " + this.getFileName());
							}
						}else{
							if(fst.getTmd().contents[this.getContentID()].error_output_done.addAndGet(1) == 1){
								Logger.log("Content " + String.format("%08X",getContentID()) + " missing. Downloading the files from the server");
							}
						}
						
					}
				}else{
					if(!Settings.downloadWhenCachedFilesMissingOrBroken){
						Logger.log("Content missing. Downloading not allowed");
						if(!Settings.skipBrokenFiles){
							System.err.println("File broken!");
							System.exit(2);
						}else{
							Logger.log("Ignoring the missing file: " + this.getFileName());
						}
					}else{
						if(fst.getTmd().contents[this.getContentID()].error_output_done.addAndGet(1) == 1){
							Logger.log("Content " + String.format("%08X",getContentID())  + " missing. Downloading the files from the server");
						}
					}
				}
			}
			Logger.log("Downloading: " + String.format("%8.2f MB ", getFileLength()/1024.0/1024.0)  + getFullPath());
			Downloader.getInstance().downloadAndDecrypt(this,progress,false);
			
			
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(2);
		}
		
	}

	public List<String> getPathList() {
		return pathList;
	}

	public void setPathList(List<String> pathList) {
		this.pathList = pathList;
	}

	public String getContentPath() {
		return fst.getTmd().getContentPath() + "/" + String.format("%08X", getNUScontentID()) + ".app";
	}

	public long getTitleID() {		
		return fst.getTmd().titleID;
	}

	public TIK getTicket() {		
		return fst.getTmd().getNUSTitle().getTicket();
	}
	
	public String getTargetPath(){
		return fst.getTmd().getNUSTitle().getTargetPath();
	}

	public byte[] downloadAsByteArray() throws IOException {
		return Downloader.getInstance().downloadAndDecrypt(this,null,true);
	}

	@Override
	public String getName() {
		return getFileName();
	}

	
	
	
	
}
