package de.mas.jnustool;

import java.util.concurrent.Callable;

public class FEntryDownloader implements Callable<Integer>
{
	FEntry f;
	Progress progress = null;
	public void setTitle(FEntry f){
		 this.f = f;
	}
	public FEntryDownloader(FEntry f,Progress fatherProgress){
		setTitle(f);
		createProgressListener(fatherProgress);
	}
 
  
	private void createProgressListener(Progress fatherProgress) {
		if(fatherProgress != null){
			progress = new Progress();
			progress.setTotal(f.getFileLength());
			fatherProgress.add(progress);	
		}
	}
	@Override
	public Integer call() throws Exception {
		f.downloadAndDecrypt(progress);
		return null;
	}

}
