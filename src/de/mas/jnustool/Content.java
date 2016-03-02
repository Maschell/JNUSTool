package de.mas.jnustool;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import de.mas.jnustool.util.Downloader;
import de.mas.jnustool.util.Settings;
import de.mas.jnustool.util.Util;

public class Content {
	
	int 	ID;						//	0	 0xB04
	short	index;					//	4    0xB08
	short 	type;					//	6	 0xB0A
	long	size;					//	8	 0xB0C
	byte[]	SHA2 = new byte[32];	//  16    0xB14
	TitleMetaData tmd;
	AtomicInteger error_output_done = new AtomicInteger(0);//  16    0xB14
	
	
	public Content(int ID, short index, short type, long size, byte[] SHA2,TitleMetaData tmd) {
		this.ID = ID;
		this.index = index;
		this.type = type;
		this.size = size;
		this.SHA2 = SHA2;
		this.tmd = tmd;
	}
	@Override
	public String toString(){		
		return "ID: " + ID +" index: " + index + " type: " + type + " size: " + size + " SHA2: " + Util.ByteArrayToString(SHA2); 
	}
	
	public void download(Progress progress) throws IOException{
		String tmpPath = tmd.getContentPath();
		if ((type & 0x02) == 0x02){
			Downloader.getInstance().downloadContentH3(tmd.titleID,ID,tmpPath,null);
		}
		File f = new File(tmpPath + "/" + String.format("%08X", ID ) + ".app");
		if(f.exists()){
			if(f.length() == size){
				Logger.log("Skipping Content: " + String.format("%08X", ID));
				progress.addCurrent((int) size);
			}else{
				if(Settings.downloadWhenCachedFilesMissingOrBroken){
					Logger.log("Content " +String.format("%08X", ID) + " is broken. Downloading it again.");
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
			Logger.log("Download Content: " + String.format("%08X", ID));
			Downloader.getInstance().downloadContent(tmd.titleID,ID,tmpPath,progress);
		}
		
	}
}
