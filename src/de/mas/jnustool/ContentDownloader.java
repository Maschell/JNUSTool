package de.mas.jnustool;

import java.util.concurrent.Callable;

/**
 * Callable Class to download Contents
 * Has no return value
 * @author Maschell
 *
 */
public class ContentDownloader implements Callable<Integer>
{
	Content content;
	Progress progress = null;
	
	/**
	 * 
	 * @param content: the content that need to be downloaded
	 * @param fatherProgress: father progress that need be informed about the progress. (Can be NULL)
	 */
	public ContentDownloader(Content content,Progress fatherProgress){
		setContent(content);
		createProgress(fatherProgress);
	}
  
	/**
	 * Sets the content that need to be downloaded
	 * @param content
	 */
	public void setContent(Content content){
		 this.content = content;
	}
	
	/**
	 * Creates a new sub process for this content.
	 * @param fatherProgress: the father progress
	 */
	private void createProgress(Progress fatherProgress) {
		if(fatherProgress != null){
			progress = new Progress();
			fatherProgress.add(progress);
			progress.addTotal(this.content.size);			
		}
	}	

	@Override
	public Integer call() throws Exception {
	    try{
	        this.content.download(progress);
	    }catch(Exception e){
	        e.printStackTrace();
	        throw e;
	    }
		return null;
	}

}
