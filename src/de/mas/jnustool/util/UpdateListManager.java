package de.mas.jnustool.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.mas.jnustool.FEntry;
import de.mas.jnustool.Logger;
import de.mas.jnustool.NUSTitle;

public class UpdateListManager {
    public static List<NUSTitleInformation> getTitles() {
        boolean preSettings = Settings.logToPrintLn;
        Logger.log("Loading "+ Settings.updateCSVPath);
        List<NUSTitleInformation> updatelist = readUpdateCSV();
        if(Settings.updateCSVFromServer){
            Logger.log("Getting a new updatelist from the server");
            trustAllCerts();
            int version = getUpdateListVersion();
            Logger.log("Downloading updatelist v" + version);
            List<NUSTitleInformation> newupdatelist = getUpdateList(version);
            Settings.logToPrintLn = false;
            updatelist =  new ArrayList<>(checkUpdateList(updatelist,newupdatelist));
            Settings.logToPrintLn = preSettings;
            
            if(newVersions.get() + newTitle.get() > 0){
                Logger.log("Got " + (newVersions.get() + newTitle.get()) + " new updates.");
                Logger.log("New versions: "+ newVersions.get());
                Logger.log("New titles  : "+ newTitle.get());
                try {
                    Writer fr = new BufferedWriter(new OutputStreamWriter(
                            new FileOutputStream(Settings.updateCSVPath), "UTF-8"));
                    BufferedWriter bw = new BufferedWriter(fr);
                    for(NUSTitleInformation title : updatelist){
                            bw.write(title.toString() +"\r\n");
                    }
                    bw.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                Logger.log("Wrote them to " + Settings.updateCSVPath);
                Logger.messageBox("Updated the " + Settings.updateCSVPath + "!");
            }else{
                Logger.log("Everything is up to date!");
            }
        }
        return updatelist;
    }
    static AtomicInteger count = new AtomicInteger(0);
    static AtomicInteger newVersions = new AtomicInteger(0);
    static AtomicInteger newTitle = new AtomicInteger(0);
    
    private static Collection<NUSTitleInformation> checkUpdateList(List<NUSTitleInformation> oldList,List<NUSTitleInformation> newList){
        Map<Long,NUSTitleInformation> csvListMap = new HashMap<>();
        for(NUSTitleInformation n : oldList){
            if(!csvListMap.containsKey(n.getTitleID())){
                csvListMap.put(n.getTitleID(),n);
            }
        }
        
        List<NUSTitleInformation> needsDownload = new ArrayList<>();
        for(NUSTitleInformation newTitleInfo : newList){
            if((newTitleInfo.getTitleID()| 0x0000000E00000000L) != newTitleInfo.getTitleID())continue;
            if(csvListMap.containsKey(newTitleInfo.getTitleID()| 0x0000000E00000000L)){
                NUSTitleInformation existing = csvListMap.get(newTitleInfo.getTitleID()| 0x0000000E00000000L);
                int existingVersion = existing.getVersion();
                int newVersion = newTitleInfo.getVersion();
                if(newVersion > existingVersion){
                    existing.addVersion(newVersion);
                    newVersions.incrementAndGet();
                    System.out.println("Found new update: " + newTitleInfo.getTitleIDAsString() + " now v" + newVersion + " instead of v" + existingVersion);
                }else{
                    //System.out.println("No new version for" + existing.getLongnameEN());
                }
            }else{
                newTitle.incrementAndGet();
                System.out.println("Found new update: " + newTitleInfo.getTitleIDAsString()+ " v" + newTitleInfo.getVersion());
                needsDownload.add(newTitleInfo);      
            }
        }
        ForkJoinPool pool = ForkJoinPool.commonPool();
        List<Callable<NUSTitleInformation>> tasks = new ArrayList<>();
        final int size = needsDownload.size();
        
        for(NUSTitleInformation n : needsDownload){
            final NUSTitleInformation test = n;
            
            tasks.add(new Callable<NUSTitleInformation>() {
                @Override
                public NUSTitleInformation call() throws Exception {
                    NUSTitleInformation newinformation = null;
                    NUSTitle foo = new NUSTitle(test.getTitleID()| 0x0000000E00000000L, -1, null);
                    FEntry testa = foo.getFst().metaFENtry;
                    byte[] metaxml = null;
                   
                    if(testa != null){
                        try {
                            metaxml = testa.downloadAsByteArray();
                            InputStream stream = new ByteArrayInputStream(metaxml);
                            newinformation = foo.readMeta(stream);
                            newinformation.setTitleID(newinformation.getTitleID() | 0x0000000E00000000L); //Force update
                            newinformation.addVersion(test.getVersion());
                            System.out.println(String.format("( %03d of %03d) ", count.incrementAndGet(),size) +"Downloaded information for " + test.getTitleIDAsString());
                            stream.close();
                        } catch (IOException e) {
                            System.out.println("Downloading information for " + test.getTitleIDAsString() + " failed");
                        }                    
                    }     
                    return newinformation;
                }
            });     
        }
        for(Future<NUSTitleInformation> future :pool.invokeAll(tasks)){
            NUSTitleInformation newinfo = null;
            try {
                newinfo = future.get();
            } catch (InterruptedException | ExecutionException  e) {
                System.out.println("Downloading information failed");
                continue;
            } 
            if(newinfo != null){
                csvListMap.put(newinfo.getTitleID(), newinfo);
            }
        }
        return csvListMap.values();
        
            
    }
    private static void trustAllCerts() {
      //Lets ignore the cert warning.
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager(){
            public X509Certificate[] getAcceptedIssuers(){return null;}
            public void checkClientTrusted(X509Certificate[] certs, String authType){}
            public void checkServerTrusted(X509Certificate[] certs, String authType){}
        }};

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());            
        } catch (Exception e) {
            ;
        }
    }
    private static int getUpdateListVersion(){
        try {
            byte[] version_xml = Downloader.getInstance().downloadHTTPSFileToByteArray(Settings.updateListVersionURL);
            InputStream stream = new ByteArrayInputStream(version_xml);
            int version = readVersionNumber(stream);
            stream.close();
            return version;
        } catch (IOException e1) {
            return 0;
        }
    }
    private static List<NUSTitleInformation> getUpdateList(int version) {
        List<NUSTitleInformation> list = new ArrayList<>();
        try {
            byte[] update_list = Downloader.getInstance().downloadHTTPSFileToByteArray(String.format(Settings.updateListURL, version));
            if(update_list == null){
                return list;
            }
            InputStream stream = new ByteArrayInputStream(update_list);
            list = parseUpdateListXML(stream);
            stream.close();            
        } catch (IOException e1) {
            System.err.println("Error while downloading the updatelist!");
            e1.printStackTrace();
        }      
        return list;
    }
    
    public static List<NUSTitleInformation> parseUpdateListXML(InputStream bis) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        List<NUSTitleInformation> list = new ArrayList<>();
        try {
            builder = factory.newDocumentBuilder();
            Document document = builder.parse(bis);
            NodeList titles = document.getElementsByTagName("title");
            for(int i = 0;i<titles.getLength();i++){
                Node title = titles.item(i);
                if(title.getChildNodes().getLength() < 2) continue;
                String titleID = title.getChildNodes().item(0).getTextContent();
                String version = title.getChildNodes().item(1).getTextContent();
                NUSTitleInformation nusTitle = new NUSTitleInformation();
                nusTitle.setTitleID(Util.StringToLong(titleID));
                nusTitle.addVersion(version);
                list.add(nusTitle);
            }
            
        } catch (ParserConfigurationException | SAXException | IOException e) {
            Logger.log("Error while parsing the updatelist");
        }
        return list;
    }
    
    public static int readVersionNumber(InputStream bis) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
            Document document = builder.parse(bis);
            return Integer.parseInt(document.getElementsByTagName("version").item(0).getTextContent());            
        } catch (ParserConfigurationException | SAXException | IOException e) {
            Logger.log("Error while parsing the meta files");
        }
        return 0;
    }

    @SuppressWarnings("resource")
    private static List<NUSTitleInformation> readUpdateCSV() {
        if(Settings.updateCSVPath == null) return null;
        BufferedReader in = null;
        List<NUSTitleInformation> list = new ArrayList<>();
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(Settings.updateCSVPath)), "UTF-8"));
            String line;
            while((line = in.readLine()) != null){
                String[] infos = line.split(";");
                if(infos.length != 8) {
                    Logger.messageBox("Updatelist is broken! A new updatelist will be created.");
                    Logger.log("Updatelist is broken! A new updatelist will be created.");
                    return list;
                }
                long titleID = Util.StringToLong(infos[0].replace("-", ""));
                int region = Integer.parseInt(infos[1]);
                String  content_platform = infos[2];
                String  company_code = infos[3];
                String  product_code = infos[4];
                String  ID6 = infos[5];
                String  longnameEN = infos[6];
                String[]  versions = infos[7].split(",");               
                NUSTitleInformation info = new NUSTitleInformation(titleID, longnameEN, ID6, product_code, content_platform, company_code, region,versions);
                
                list.add(info);
            }
            in.close();
        } catch (IOException | NumberFormatException e) {
            try {
                if(in != null)in.close();
            } catch (IOException e1) {
            }
            Logger.messageBox("Updatelist is broken or missing. A new updatelist will be created.");
            Logger.log("Updatelist is broken! A new updatelist will be created.");
            return new ArrayList<>();
        }
        return list;        
    }
}
