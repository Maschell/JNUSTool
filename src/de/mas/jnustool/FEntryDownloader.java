package de.mas.jnustool;

import java.util.concurrent.Callable;

public class FEntryDownloader implements Callable<Integer>
{
	FEntry f;
	public void setTitle(FEntry f){
		 this.f = f;
	}
	public FEntryDownloader(FEntry f){
		setTitle(f);
	}
 
  
	@Override
	public Integer call() throws Exception {
		f.downloadAndDecrypt();
		return null;
	}

}
