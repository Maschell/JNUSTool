package de.mas.jnustool;

import java.util.concurrent.Callable;

public class ContentDownloader implements Callable<Integer>
{
	Content c;
	Progress progress = null;
	public void setContent(Content c){
		 this.c = c;
	}
	public ContentDownloader(Content f,Progress fatherProgress){
		setContent(f);
		createProgress(fatherProgress);
	}
  
	private void createProgress(Progress fatherProgress) {
		if(fatherProgress != null){
			progress = new Progress();
			fatherProgress.add(progress);
			progress.addTotal(c.size);
			
		}
	}
	
	@Override
	public Integer call() throws Exception {
		c.download(progress);
		return null;
	}

}
