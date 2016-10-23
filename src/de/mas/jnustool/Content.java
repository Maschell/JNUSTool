package de.mas.jnustool;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import de.mas.jnustool.util.Downloader;
import de.mas.jnustool.util.Settings;
import de.mas.jnustool.util.Util;
/**
 * Content file of the NUSTitle. Holds the encrpyted files
 * 
 * Thanks to crediar for the offsets in CDecrypt
 * @author Maschell
 *
 */
public class Content {
	/**
	 * TODO: make it simpler
	 */
	int 	ID;						//	0	 0xB04
	short	index;					//	4    0xB08
	short 	type;					//	6	 0xB0A
	long	size;					//	8	 0xB0C
	byte[]	SHA2 = new byte[32];	//  16    0xB14
	TitleMetaData tmd;
	AtomicInteger error_output_done = new AtomicInteger(0);	
	
	public Content(int ID, short index, short type, long size, byte[] SHA2,TitleMetaData tmd) {
		this.ID = ID;
		this.index = index;
		this.type = type;
		this.size = size;
		this.SHA2 = SHA2;
		this.tmd = tmd;
	}
	
	/**
	 * Downloads the content files (encrypted) 
	 * @param progress: A progress object can be used to get informations of the progress. Will be ignored when null is used. 
	 * @throws IOException
	 */	
	public void download(Progress progress) throws IOException{		
		String tmpPath = tmd.getContentPath();
		
		File f = new File(tmpPath + "/" + String.format("%08X", ID ) + ".app");
		if(f.exists()){
			if(f.length() == size){
				Logger.log("Skipping Content: " + String.format("%08X", ID));
				if(progress != null){
				    progress.addCurrent((int) size);
				}				
			}else{
				if(Settings.downloadWhenCachedFilesMissingOrBroken){				
					Logger.log("Content " +String.format("%08X", ID) + " has a different filesize and may be broken. Downloading it again.");
					new File(tmpPath).delete();
					Logger.log("Downloading Content: " + String.format("%08X", ID));
					Downloader.getInstance().downloadContent(tmd.titleID,ID,tmpPath,progress);
					
				}else{
					if(Settings.skipBrokenFiles){
						Logger.log("Content " +String.format("%08X", ID) + " is broken. Ignoring it.");								
					}else{
						Logger.log("Content " +String.format("%08X", ID) + " is broken. Downloading not allowed.");
						System.exit(2);
					}
				}
			}
		}else{
			Logger.log("Downloading Content: " + String.format("%08X", ID));
			Downloader.getInstance().downloadContent(tmd.titleID,ID,tmpPath,progress);          
		}
		if ((type & 0x02) == 0x02){
		    f = new File(tmpPath + "/" + String.format("%08X", ID ) + ".h3");
	        if(!f.exists()){
	            Logger.log("Downloading H3: " + String.format("%08X.h3", ID));
	            Downloader.getInstance().downloadContentH3(tmd.titleID,ID,tmpPath,null);
	        }else{
	            Logger.log("Skipping H3: " + String.format("%08X.h3", ID));
	        }		    
		}
	}
	
	@Override
	public String toString(){		
		return "ID: " + ID +" index: " + index + " type: " + type + " size: " + size + " SHA2: " + Util.ByteArrayToString(SHA2); 
	}
}
