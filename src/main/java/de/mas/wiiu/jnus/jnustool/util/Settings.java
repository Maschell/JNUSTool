package de.mas.wiiu.jnus.jnustool.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import de.mas.wiiu.jnus.jnustool.Logger;

public class Settings {
	public static boolean downloadContent = false;
	public static boolean useCachedFiles = true;
	public static boolean downloadWhenCachedFilesMissingOrBroken = true;
	public static boolean skipBrokenFiles = false;
	public static boolean skipExistingFiles = true;
	public static boolean skipExistingTMDTICKET = true;
	public static boolean DL_ALL_VERSIONS = false;
	public static String  FILELIST_NAME = "filelist.txt";
	public static boolean  logToPrintLn = true;
    public static String updateCSVPath = "updateinfos.csv";
    public static boolean updateCSVFromServer = true;
    public static String updateListVersionURL = "";
    public static String updateListURL = "%d";
    public static boolean deepHashCheck = true;
    
    public static void readConfig() throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(new File("config")));     
        Downloader.URL_BASE =  in.readLine();   
        String commonkey = in.readLine();
        if(commonkey.length() != 32){
            Logger.messageBox("CommonKey length is wrong");
            Logger.log("Commonkey length is wrong");
            System.exit(1);
        }
        Util.commonKey =  Util.hexStringToByteArray(commonkey);
        Settings.updateCSVPath =  in.readLine();
        Settings.updateListVersionURL = in.readLine();
        Settings.updateListURL = in.readLine();
        in.close();
    }
}
