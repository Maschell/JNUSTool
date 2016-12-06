package de.mas.jnustool;

import java.io.File;
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
import de.mas.jnustool.util.NUSTitleInformation;
import de.mas.jnustool.util.Settings;
import de.mas.jnustool.util.UpdateListManager;
import de.mas.jnustool.util.Util;

public class Starter {
	public static void main(String[] args) {
	    Logger.log("JNUSTool 0.3b - by Maschell");
		Logger.log("");
		try {
			Settings.readConfig();
		} catch (IOException e) {
			System.err.println("Error while reading config! Needs to be:");
			System.err.println("DOWNLOAD URL BASE");
			System.err.println("COMMONKEY");
			System.err.println("updateinfos.csv");
			System.err.println("UPDATELIST VERSION URL");
			System.err.println("UPDATELIST URL PATTERN");
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
		    List<NUSTitleInformation> updatelist = UpdateListManager.getTitles();
		    List<NUSTitleInformation> result = new ArrayList<>();
            if(updatelist != null){
    		    UpdateChooser.createAndShowGUI(updatelist,result);
    		    synchronized (result) {             
                    try {
                        result.wait();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
			for(final NUSTitleInformation nus : result){				
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

	public static void downloadMeta(List<NUSTitleInformation> output_, final Progress totalProgress) {
		ForkJoinPool pool = ForkJoinPool.commonPool();
		List<ForkJoinTask<Boolean>> list = new ArrayList<>();
		
		for(final NUSTitleInformation nus : output_){
			final long tID = nus.getTitleID();
			list.add(pool.submit(new Callable<Boolean>(){
				@Override
				public Boolean call() throws Exception {
					NUSTitle nusa  = new NUSTitle(tID,nus.getSelectedVersion(),Util.ByteArrayToString(nus.getKey()));
					Progress childProgress = new Progress();					
					totalProgress.add(childProgress);
					Util.deleteFolder(new File(nusa.getLongNameFolder() + "/updates"));
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

	public static void downloadEncrypted(List<NUSTitleInformation> output_, final Progress progress) {
		ForkJoinPool pool = ForkJoinPool.commonPool();
		List<ForkJoinTask<Boolean>> list = new ArrayList<>();
		
		for(final NUSTitleInformation nus : output_){
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

	public static void downloadEncryptedAllVersions(List<NUSTitleInformation> output_, final Progress progress) {
		ForkJoinPool pool = new ForkJoinPool(25);

		List<ForkJoinTask<Boolean>> list = new ArrayList<>();
		final int outputsize = output_.size();
		for(final NUSTitleInformation nus : output_){
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
					System.out.println("Update download complete " + "(" + nus.getLongnameEN() +")" +"! Loaded updates for " +  nus.getAllVersions().size() + " version. Now are " + finished.incrementAndGet() + " of " + outputsize + " done! ");
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
