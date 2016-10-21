package de.mas.jnustool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicInteger;

import de.mas.jnustool.gui.NUSGUI;
import de.mas.jnustool.gui.UpdateChooser;
import de.mas.jnustool.util.Downloader;
import de.mas.jnustool.util.NUSTitleInformation;
import de.mas.jnustool.util.Util;

public class Starter {

	private static String updateCSVPath;
	
	public static void main(String[] args) {
	    Logger.log("JNUSTool 0.0.8 - alpha - by Maschell");
		Logger.log("");
		try {
			readConfig();
		} catch (IOException e) {
			System.err.println("Error while reading config! Needs to be:");
			System.err.println("DOWNLOAD URL BASE");
			System.err.println("COMMONKEY");
			System.err.println("updateinfos.csv");
			return;
		}

		long titleID = 0;
		String key = null;
		if(args.length != 0 ){				
			titleID = Util.StringToLong(args[0]);
			int version = -1;
			if( args.length > 1 && args[1].length() == 32){
				key = args[1].substring(0, 32);
			}
			
			if(titleID != 0){
			    String path = "";
				boolean dl_encrypted = false;
				boolean download_file = false;
				
				for(int i =0; i< args.length;i++){
					if(args[i].startsWith("v")){
						version = Integer.parseInt((args[i].substring(1)));
					}
					if(args[i].equals("-dlEncrypted")){
						dl_encrypted = true;						
					}					
					
					if(args[i].equals("-file")){
                        if(args.length > i){
                            i++;
                            path = args[i];                           
                        }
                        download_file = true;                        
                    }       
				}
				if(dl_encrypted){
					NUSTitle title = new NUSTitle(titleID,version, key);
					try {
						title.downloadEncryptedFiles(null);
					} catch (IOException e) {
						e.printStackTrace();
					}	
					System.exit(0);
				}else if(download_file){
                    NUSTitle title = new NUSTitle(titleID,version, key);
                    
                    title.decryptFEntries(title.getFst().getFileEntriesByFilePath(path), null);
                    
                    System.exit(0);
                }
				
				NUSGUI m = new NUSGUI(new NUSTitle(titleID,version, key));
		        m.setVisible(true);			
			}
		}else{
			for(NUSTitleInformation nus : getTitleID()){
				
				final long tID = nus.getTitleID();
				new Thread(new Runnable() {
					
					@Override
					public void run() {
						NUSGUI m = new NUSGUI(new NUSTitle(tID,nus.getSelectedVersion(), null));
				        m.setVisible(true);						
					}
				}).start();;
			}			
		}
	}
	


	private static List<NUSTitleInformation> getTitleID() {
		List<NUSTitleInformation> updatelist = readUpdateCSV();
		List<NUSTitleInformation> result = null;
		if(updatelist != null){
			result = new ArrayList<>();
			UpdateChooser.createAndShowGUI(updatelist,result);
			synchronized (result) {			    
		    	try {
					result.wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}else{
			Logger.messageBox("Updatefile is missing or not in config?");
			System.exit(2);
		}
		return result;
	}



	@SuppressWarnings("resource")
	private static List<NUSTitleInformation> readUpdateCSV() {
		if(updateCSVPath == null) return null;
		BufferedReader in = null;
		List<NUSTitleInformation> list = new ArrayList<>();
		try {
			in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(updateCSVPath)), "UTF-8"));
			String line;
		    while((line = in.readLine()) != null){
		    	String[] infos = line.split(";");
		    	if(infos.length != 8) {
		    		Logger.messageBox("Updatelist is broken!");
		    		Logger.log("Updatelist is broken!");
		    		return null;
		    	}
		    	long titleID = Util.StringToLong(infos[0].replace("-", ""));
		    	int region = Integer.parseInt(infos[1]);
		    	String  content_platform = infos[2];
		    	String  company_code = infos[3];
		    	String  product_code = infos[4];
		    	String  ID6 = infos[5];
		    	String  longnameEN = infos[6];
		    	String[]  versions = infos[7].split(",");		    	
		    	NUSTitleInformation info = new NUSTitleInformation(titleID, longnameEN, ID6, product_code, content_platform, company_code, region,versions);
		    	
		    	list.add(info);
		    }
		    in.close();
		} catch (IOException | NumberFormatException e) {
			try {
				if(in != null)in.close();
			} catch (IOException e1) {
			}
			Logger.messageBox("Updatelist is broken or missing");
			Logger.log("Updatelist is broken!");
			return null;
		}
		return list;		
	}



	public static void readConfig() throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(new File("config")));		
		Downloader.URL_BASE =  in.readLine();	
		String commonkey = in.readLine();
		if(commonkey.length() != 32){
			Logger.messageBox("CommonKey length is wrong");
			Logger.log("Commonkey length is wrong");
			System.exit(1);
		}
		Util.commonKey =  Util.hexStringToByteArray(commonkey);
		updateCSVPath =  in.readLine();
		in.close();
		
	}

	public static boolean deleteFolder(File element) {
	    if (element.isDirectory()) {	    	
	        for (File sub : element.listFiles()) {
	        	if(sub.isFile()){
	        		return false;
	        	}
	        }
	        for (File sub : element.listFiles()) {
	        	if(!deleteFolder(sub)) return false;
	        }	        
	    }
	    element.delete();	    
	    return true;
	}

	public static void downloadMeta(List<NUSTitleInformation> output_, Progress totalProgress) {
		ForkJoinPool pool = ForkJoinPool.commonPool();
		List<ForkJoinTask<Boolean>> list = new ArrayList<>();
		
		for(NUSTitleInformation nus : output_){
			final long tID = nus.getTitleID();
			list.add(pool.submit(new Callable<Boolean>(){
				@Override
				public Boolean call() throws Exception {
					NUSTitle nusa  = new NUSTitle(tID,nus.getSelectedVersion(),Util.ByteArrayToString(nus.getKey()));
					Progress childProgress = new Progress();
					
					totalProgress.add(childProgress);
					deleteFolder(new File(nusa.getLongNameFolder() + "/updates"));
					nusa.setTargetPath(nusa.getLongNameFolder());
					nusa.decryptFEntries(nusa.getFst().getMetaFolder(),childProgress);					
					return true;
				}				
			}));
		}
		for(ForkJoinTask<Boolean> task : list){
			try {
				task.get();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static void downloadEncrypted(List<NUSTitleInformation> output_, Progress progress) {
		ForkJoinPool pool = ForkJoinPool.commonPool();
		List<ForkJoinTask<Boolean>> list = new ArrayList<>();
		
		for(NUSTitleInformation nus : output_){
			final long tID = nus.getTitleID();
			list.add(pool.submit(new Callable<Boolean>(){
				@Override
				public Boolean call() throws Exception {
					NUSTitle nusa  = new NUSTitle(tID,nus.getSelectedVersion(), Util.ByteArrayToString(nus.getKey()));
					Progress childProgress = new Progress();					
					progress.add(childProgress);
					nusa.downloadEncryptedFiles(progress);
							
					return true;
				}				
			}));
		}
		for(ForkJoinTask<Boolean> task : list){
			try {
				task.get();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

	public static AtomicInteger finished = new AtomicInteger(); 

	public static void downloadEncryptedAllVersions(List<NUSTitleInformation> output_, Progress progress) {
		ForkJoinPool pool = new ForkJoinPool(25);

		List<ForkJoinTask<Boolean>> list = new ArrayList<>();
		
		for(NUSTitleInformation nus : output_){
			final long tID = nus.getTitleID();
			list.add(pool.submit(new Callable<Boolean>(){
				@Override
				public Boolean call() throws Exception {
					int count = 1;
					for(Integer i : nus.getAllVersions()){
						NUSTitle nusa  = new NUSTitle(tID,i, Util.ByteArrayToString(nus.getKey()));
						Progress childProgress = new Progress();
						progress.add(childProgress);			
						nusa.downloadEncryptedFiles(progress);
						System.out.println("Update download progress " + "(" + nus.getLongnameEN() + ") version "+ i + " complete! This was " + count  + " of " + nus.getAllVersions().size() + "!");
						count++;
					}	
					System.out.println("Update download complete " + "(" + nus.getLongnameEN() +")" +"! Loaded updates for " +  nus.getAllVersions().size() + " version. Now are " + finished.incrementAndGet() + " of " + output_.size() + " done! ");
					return true;
				}				
			}));
		}
		
		for(ForkJoinTask<Boolean> task : list){
			try {
				task.get();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

}
