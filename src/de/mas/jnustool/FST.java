package de.mas.jnustool;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.mas.jnustool.util.Util;

public class FST {
	private TitleMetaData tmd;
	long totalContentSize = 0L;
	long totalContentSizeInNUS = 0L;
	
	List<FEntry> fileEntries = new ArrayList<>();
	
	int totalContentCount = 0;
	
	int totalEntries = 0;
	int dirEntries = 0;
	public FEntry metaFENtry;
	public List<FEntry> metaFolder = new ArrayList<>();
	private Directory<FEntry> FSTDirectory = new Directory<FEntry>("root");
	
	private Directory<FEntry> contentDirectory = new Directory<FEntry>("root");
	
	public FST(byte[] decrypteddata, TitleMetaData tmd) throws IOException {		
		parse(decrypteddata,tmd);
		setTmd(tmd);
		buildDirectories();
		
	}	

	private void buildDirectories() {	
		 String contentfolder = "";
		 Directory<FEntry> curContent = contentDirectory;
		 for(FEntry f : getFileEntries()){			 
			 if(f.isInNUSTitle()){
				 contentfolder = String.format("%08X",tmd.contents[f.getContentID()].ID);
				 
				 if(!contentDirectory.containsFolder(contentfolder)){
						Directory<FEntry> newDir = new Directory<FEntry>(contentfolder);
						contentDirectory.addFolder(newDir);
				}
				 curContent = contentDirectory.getFolder(contentfolder);
				 
				 Directory<FEntry> current = FSTDirectory; 
				 int i = 0;
				 
				 for(String s :f.getPathList()){    
					 i++;
					 
					//Content
					if(curContent.containsFolder(s)){    				
						curContent = curContent.getFolder(s);
					}else{
						Directory<FEntry> newDir = new Directory<FEntry>(s);
						curContent.addFolder(newDir);
						curContent = newDir;
					}				
					if(i==f.getPathList().size()){
						curContent.addFile(f);
					}					
						
					//FST
					if(current.containsFolder(s)){    				
						current = current.getFolder(s);
					}else{    				
						Directory<FEntry> newDir = new Directory<FEntry>(s);
						current.addFolder(newDir);
						current = newDir;
					}
					if(i==f.getPathList().size()){
					    if(!f.isDir()){
					        current.addFile(f);
					    }
					}
				 }
			 }
		 }
	}
	//private Map<Content,List<FEntry>> contentmap = new HashMap<>();
	private void parse(byte[] decrypteddata, TitleMetaData tmd) throws IOException {
		
		if(!Arrays.equals(Arrays.copyOfRange(decrypteddata, 0, 3), new byte[]{0x46,0x53,0x54})){
			throw new IllegalArgumentException("Not a FST. Maybe a wrong key? Don't worry if you only want to download encrypted files!");		
		}
		this.totalContentCount = Util.getIntFromBytes(decrypteddata, 8);
		int base_offset = 0x20+totalContentCount*0x20;
		this.totalEntries = Util.getIntFromBytes(decrypteddata, base_offset+8);
		int nameOff = base_offset + totalEntries * 0x10;
		
		int level=0;
		int[] LEntry = new int[16];
		int[] Entry = new int[16];
		
		/*
		for(int i = 0;i<totalContentCount;i++){
		    contentmap.put(tmd.contents[i],new ArrayList<FEntry>());
		    int my_offset = 0x20 + (i* 0x20);
		    int address = Util.getIntFromBytes(decrypteddata, my_offset+ 0) ;		   
		    long parentid = Util.getLongFromBytes(decrypteddata, my_offset+ 8) ;
		    int groupid = Util.getIntFromBytes(decrypteddata, my_offset+ 16) ;
		    int size = Util.getIntFromBytes(decrypteddata, my_offset+ 4);
		    byte hashmode = decrypteddata[my_offset+ 20];
		    System.out.print(String.format("Content    %02X: ", i) + " offset " + String.format("%08X", address)+ "    size " + String.format("%08X", size) +" ");
		    System.out.print(String.format("parent(?) %016X: ", parentid) + " groupid " + String.format("%08X", groupid)+ "    hashmode " + String.format("%01X", hashmode) +" ");
		    System.out.println(String.format("encrypted content size: "+ String.format("%08X", tmd.contents[i].size)));
	    }*/
		
		for(int i = 0;i<this.totalEntries;i++){
			boolean dir = false;
			boolean in_nus_title = true;
			boolean extract_withHash = false;
			
			long fileOffset;
			long fileLength;
			int type;
			int contentID;
			
			String filename = "";
			String path = "";
			
			if( level > 0)
			{
				while( LEntry[level-1] == i )
				{					
					level--;
				}
			}			
			
			int offset = base_offset + i*0x10;
			
			//getting the type
			type = (int) decrypteddata[offset]+128;
			if((type & FEntry.DIR_FLAG) == 1) dir = true;			
			if((type & FEntry.NOT_IN_NUSTITLE_FLAG) == 0 ) in_nus_title = false;
			
			
			//getting Name
			decrypteddata[offset] = 0;			
			int nameoff_entry_offset = Util.getIntFromBytes(decrypteddata, offset);
			int j = 0;
			int nameoff_entry = nameOff + nameoff_entry_offset;
			while(decrypteddata[nameoff_entry + j] != 0){j++;}
			filename = new String(Arrays.copyOfRange(decrypteddata,nameoff_entry, nameoff_entry + j));
			
			
			
			//getting offsets. save in two ways
			offset+=4;
			fileOffset = Util.getUnsingedIntFromBytes(decrypteddata, offset);
				
			offset+=4;
			fileLength = Util.getUnsingedIntFromBytes(decrypteddata, offset);			
			
			@SuppressWarnings("unused")
			int parentOffset = (int) fileOffset;
			int nextOffset = (int) fileLength;
			
			
			//grabbing flags
			offset+=4;
			int flags = Util.getShortFromBytes(decrypteddata, offset);
			//if((flags & FEntry.EXTRACT_WITH_HASH_FLAG) > 0) extract_withHash = true;
			
			if((flags & FEntry.CHANGE_OFFSET_FLAG) == 0) fileOffset <<=5;
			
			//grabbing contentid
			offset+=2;
			contentID = Util.getShortFromBytes(decrypteddata, offset) ;
			
			if((tmd.contents[contentID].type & 0x2003) == 0x2003){
			    extract_withHash = true;
			}
			
			//remember total size
			this.totalContentSize += fileLength;
			if(in_nus_title)this.totalContentSizeInNUS += fileLength;
			
			boolean metafolder = false;
			
			List<String> pathList = new ArrayList<>();
			//getting the full path of entry
			
			if(dir)
			{
				dirEntries++;
				Entry[level] = i;
				LEntry[level++] =  nextOffset ;
				if( level > 15 )	// something is wrong!
				{
					break;
				}
				
				
				/*if(in_nus_title){
	                System.out.println("Dirname:      " + filename);
	                System.out.println("ID:           " + i);
	                System.out.println("ParentOffset: " + parentOffset);
	                System.out.println("  NextOffset: " + nextOffset);
	            }*/
			}//else{
			    /*
			    if(in_nus_title){
                    System.out.println("FILE   :      " + filename);
                    System.out.println("ID:           " + i);
                }*/
				StringBuilder sb = new StringBuilder();
				int k = 0;
				int nameoffoff,nameoff_entrypath;
				
				int startlevel =  level;
				if(dir){
				    startlevel = level -1;
				}
				for( j=0; j<startlevel; ++j )
				{
					nameoffoff = Util.getIntFromBytes(decrypteddata,base_offset+Entry[j]*0x10);
					k=0;
					nameoff_entrypath = nameOff + nameoffoff;
					while(decrypteddata[nameoff_entrypath + k] != 0){k++;}
					String tmpname = new String(Arrays.copyOfRange(decrypteddata,nameoff_entrypath, nameoff_entrypath + k));
					if(j==1 && tmpname.equals("meta")){
						metafolder = true;
					}
					if(!tmpname.equals("")){
						pathList.add(tmpname);						
					}					
						
					sb.append(tmpname);
					sb.append("/");
				}
				path = sb.toString();
			//}
			
			
			byte[] hash = tmd.contents[contentID].SHA2;
			//add this to the List!
			
			FEntry tmp = new FEntry(path,filename,contentID,tmd.contents[contentID].ID,fileOffset,fileLength,dir,in_nus_title,extract_withHash,pathList,this,hash,tmd.contents[contentID],(short) flags);
			
			fileEntries.add(tmp);
			if(filename.equals("meta.xml")){
				metaFENtry = tmp;
			}
			if(metafolder){
				metaFolder.add(tmp);
			}
			//contentmap.get(tmd.contents[contentID]).add(tmp);
			//Logger.log(tmd.contents[contentID].ID + " " + filename);
		}
		/*
		for(Content cur_content : contentmap.keySet()){
		    System.out.println("################");
		    System.out.println(cur_content);
		    //System.out.println(String.format("%08X", cur_content.ID));
		    System.out.println("--------------");
		    for(FEntry cur_entry : contentmap.get(cur_content)){
		        System.out.println(cur_entry);
		    }
		}*/
		
	}	
	
	public long getTotalContentSize() {
		return totalContentSize;
	}

	public void setTotalContentSize(long totalContentSize) {
		this.totalContentSize = totalContentSize;
	}

	public long getTotalContentSizeInNUS() {
		return totalContentSizeInNUS;
	}

	public void setTotalContentSizeInNUS(long totalContentSizeInNUS) {
		this.totalContentSizeInNUS = totalContentSizeInNUS;
	}

	public List<FEntry> getMetaFolder() {
		return metaFolder;
	}	

	public List<FEntry> getFileEntries() {
		return fileEntries;
	}

	public void setFileEntries(List<FEntry> fileEntries) {
		this.fileEntries = fileEntries;
	}

	public int getTotalContentCount() {
		return totalContentCount;
	}

	public void setTotalContentCount(int totalContentCount) {
		this.totalContentCount = totalContentCount;
	}

	public int getTotalEntries() {
		return totalEntries;
	}

	public void setTotalEntries(int totalEntries) {
		this.totalEntries = totalEntries;
	}

	public int getDirEntries() {
		return dirEntries;
	}


	public void setDirEntries(int dirEntries) {
		this.dirEntries = dirEntries;
	}

	@Override
	public String toString(){		
		return "entryCount: " +  totalContentCount+ " entries: " +  totalEntries; 
	}

	public int getFileCount() {
		int i = 0;
		for(FEntry f: getFileEntries()){				
			if(!f.isDir())
				i++;
		}	
		return i;
	}
	
	public int getFileCountInNUS() {
		int i = 0;
		for(FEntry f: getFileEntries()){				
			if(!f.isDir() &&  f.isInNUSTitle())
				i++;
		}	
		return i;
	}
	
	public String notInNUS() {
		StringBuilder sb = new StringBuilder();
		for(FEntry f: getFileEntries()){				
			if(!f.isDir() &&  !f.isInNUSTitle()){
				sb.append(f.getFullPath() + " " + String.format("%8.2f MB ", f.getFileLength()/1024.0/1024.0) + "\n");
			}				
		}	
		return sb.toString();
	}

	public Directory<FEntry> getFSTDirectory() {	   
		return FSTDirectory;
	}
	
	public Directory<FEntry> getContentDirectory() {
		return contentDirectory;
	}

	public TitleMetaData getTmd() {
		return tmd;
	}

	public void setTmd(TitleMetaData tmd) {
		this.tmd = tmd;
	}

    public List<FEntry> getFileEntriesByFilePath(String filepath){
        List<FEntry> newList = new ArrayList<>();
        if(!filepath.startsWith("/")){
            filepath = "/" + filepath;
        }
        Pattern p = Pattern.compile(filepath);
        for(FEntry f : fileEntries){ 
           
            Matcher m = p.matcher(f.getFullPath());
            if(m.matches()){
                newList.add(f);
            }
        }       
        return newList;
    }

	
	
}
