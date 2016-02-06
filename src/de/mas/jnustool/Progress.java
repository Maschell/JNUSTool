package de.mas.jnustool;

import java.util.ArrayList;
import java.util.List;

public class Progress {
	private long total;
	private long current;
	private ProgressUpdateListener progressUpdateListener = null;
	List<Progress> children = new ArrayList<>();
	Progress father = null;

	
	public long getTotalOfSingle() {
		return total;
	}

	public void setTotal(long total) {
		this.total = total;
	}

	public long getCurrentOfSingle() {
		return current;
	}

	public void setCurrent(long current) {
		this.current = current;
		update();	
	}

	public void addCurrent(int i) {		
		if(this.current + i > getTotalOfSingle()){
			setCurrent(getTotalOfSingle());
		}else{
			setCurrent(getCurrent() + i);
		}
		
		
	}
	
	private void update() {		
		if(father != null) father.update();
		
		if(progressUpdateListener !=  null){			
				progressUpdateListener.updatePerformed(this);			
		}
	}

	public void add(Progress progress) {
		progress.setFather(this);
		children.add(progress);		
	}

	private void setFather(Progress progressListener) {
		this.father =  progressListener;		
	}

	public long getCurrent() {
		long tmp = getCurrentOfSingle();
		for(Progress p : children){
			tmp +=p.getCurrent();
		}
		return tmp;
	}

	public long getTotal() {
		long tmp = getTotalOfSingle();
		for(Progress p : children){
			tmp +=p.getTotal();
		}
		return tmp;
	}
	
	public void setProgressUpdateListener(ProgressUpdateListener progressUpdateListener) {
		this.progressUpdateListener = progressUpdateListener;
	}

	public void clear() {
		setCurrent(0);
		setTotal(0);
		children = new ArrayList<>();
	}

	public int statusInPercent() {		
		return (int) ((getCurrent()*1.0)/(getTotal()*1.0)*100);
	}

	public void finish() {
		setCurrent(getTotalOfSingle());		
	}
	
}
