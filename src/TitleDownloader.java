import java.util.concurrent.Callable;

public class TitleDownloader implements Callable<Integer>
{
	
	FEntry f;
	public void setTitle(FEntry f){
		 this.f = f;
	}
	public TitleDownloader(FEntry f){
		setTitle(f);
	}
 
  
@Override
public Integer call() throws Exception {
	f.download();
	return null;
}

}
