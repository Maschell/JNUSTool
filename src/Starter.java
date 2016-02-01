import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Starter {

	public static void main(String[] args) {
		try {
			readConfig();		
		} catch (IOException e) {
			System.err.println("Error while reading config! Needs to be:");
			System.err.println("DOWNLOAD URL BASE");
			System.err.println("COMMONKEY");
			return;
		}
		if(args.length != 0){
			long titleID = Util.StringToLong(args[0]);
			String key = null;
			if( args.length > 1 && args[1].length() == 32){
				key = args[1].substring(0, 32);
			}
			new NUSTitle(titleID, key);
		}else{
			System.out.println("Need parameters: TITLEID [KEY]");
		}
		
	}

	private static void readConfig() throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(new File("config")));		
		Downloader.URL_BASE =  in.readLine();		
		Util.commonKey =  Util.hexStringToByteArray(in.readLine());
		in.close();
		
	}

}
