import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

public class NUSTitle {
	private TitleMetaData tmd;
	private long titleID;
	private TIK ticket;
		
	public NUSTitle(long titleId,String key){
		
		try {
			titleID =  titleId;
			Downloader.getInstance().titleID = titleId;
			Decryption decryption = new Decryption(Util.commonKey,titleId);
			
			tmd = new TitleMetaData(Downloader.getInstance().downloadTMDToByteArray());			
			
			if(key == null){
				ticket = new TIK(Downloader.getInstance().downloadTicketToByteArray(),tmd.titleID);
			}else{
				ticket = new TIK(key,titleId);
			}
			
			
			Downloader.getInstance().ticket = ticket;
			decryption.init(ticket.getDecryptedKey(),0);
									
			byte[] encryptedFST = Downloader.getInstance().downloadContentToByteArray(tmd.contents[0].ID);
			byte[] decryptedFST = decryption.decrypt(encryptedFST);
			
			FST fst = new FST(decryptedFST,tmd);
			
			long start = new Date().getTime();
			
			System.out.println("Total Size of Content Files: " + ((int)((getTotalContentSize()/1024.0/1024.0)*100))/100.0 +" MB");
			System.out.println("Total Size of Decrypted Files: " + ((int)((fst.getTotalContentSizeInNUS()/1024.0/1024.0)*100))/100.0 +" MB");
			System.out.println("Entries: " + fst.getTotalEntries());
			System.out.println("Entries: " + fst.getFileCount());
			System.out.println("Files in NUSTitle: " + fst.getFileCountInNUS());
			System.out.println("");
			System.out.println("Downloading all files.");
			System.out.println("");
			
			ForkJoinPool pool = ForkJoinPool.commonPool();
			List<TitleDownloader> list = new ArrayList<>();
			for(FEntry f: fst.getFileEntries()){				
				if(!f.isDir() &&  f.isInNUSTitle())
					list.add(new TitleDownloader(f));
			}			
			pool.invokeAll(list);
			
			long runningTime = new Date().getTime() - start; 
			System.out.println("");
			System.out.println("Done in: " +  runningTime/1000.0 + " seconds");
		
			
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public long getTitleID() {
		return titleID;
	}

	public void setTitleID(long titleID) {
		this.titleID = titleID;
	}

	public long getTotalContentSize() {
		return tmd.getTotalContentSize();
	}

}
