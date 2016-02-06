package de.mas.jnustool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

import de.mas.jnustool.gui.NUSGUI;
import de.mas.jnustool.gui.UpdateChooser;
import de.mas.jnustool.util.Downloader;
import de.mas.jnustool.util.NUSTitleInformation;
import de.mas.jnustool.util.Util;

public class Starter {

	private static String updateCSVPath;
	
	public static void main(String[] args) {		
		Logger.log("JNUSTool 0.0.4 - pre alpha - by Maschell");
		Logger.log("");
		try {
			readConfig();
			
		} catch (IOException e) {
			System.err.println("Error while reading config! Needs to be:");
			System.err.println("DOWNLOAD URL BASE");
			System.err.println("COMMONKEY");
			return;
		}

		long titleID = 0;
		String key = null;
		if(args.length != 0 ){				
			titleID = Util.StringToLong(args[0]);			
			if( args.length > 1 && args[1].length() == 32){
				key = args[1].substring(0, 32);
			}
			if(titleID != 0){		
				NUSGUI m = new NUSGUI(new NUSTitle(titleID, key));
		        m.setVisible(true);			
			}
		}else{
			for(NUSTitleInformation nus : getTitleID()){
				
				final long tID = nus.getTitleID();
				new Thread(new Runnable() {
					
					@Override
					public void run() {
						NUSGUI m = new NUSGUI(new NUSTitle(tID, null));
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
			in = new BufferedReader(new FileReader(new File(updateCSVPath)));
			String line;
		    while((line = in.readLine()) != null){
		    	String[] infos = line.split(";");
		    	if(infos.length != 7) {
		    		Logger.messageBox("Updatelist is broken!");
		    		System.out.println("Updatelist is broken!");
		    		return null;
		    	}
		    	long titleID = Util.StringToLong(infos[0].replace("-", ""));
		    	int region = Integer.parseInt(infos[1]);
		    	String  content_platform = infos[2];
		    	String  company_code = infos[3];
		    	String  product_code = infos[4];
		    	String  ID6 = infos[5];
		    	String  longnameEN = infos[6];
		    	NUSTitleInformation info = new NUSTitleInformation(titleID, longnameEN, ID6, product_code, content_platform, company_code, region);	
		    	list.add(info);
		    }
		    in.close();
		} catch (IOException | NumberFormatException e) {
			try {
				if(in != null)in.close();
			} catch (IOException e1) {
			}
			Logger.messageBox("Updatelist is broken or missing");
			System.out.println("Updatelist is broken!");
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
			System.out.println("Commonkey length is wrong");
			System.exit(1);
		}
		Util.commonKey =  Util.hexStringToByteArray(commonkey);
		updateCSVPath =  in.readLine();
		in.close();
		
	}



	public static void downloadMeta(List<NUSTitleInformation> output_, Progress totalProgress) {
		ForkJoinPool pool = ForkJoinPool.commonPool();
		List<ForkJoinTask<Boolean>> list = new ArrayList<>();
		for(NUSTitleInformation nus : output_){
			final long tID = nus.getTitleID();
			list.add(pool.submit(new Callable<Boolean>(){
				@Override
				public Boolean call() throws Exception {					
					NUSTitle nus  = new NUSTitle(tID, null);
					Progress childProgress = new Progress();
					totalProgress.add(childProgress);
					nus.decryptFEntries(nus.getFst().getMetaFolder(),childProgress);					
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
