package de.mas.jnustool.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class NUSTitleInformation implements Comparable<NUSTitleInformation>, Serializable{
	private static final long serialVersionUID = 1L;
	
	private long titleID;
	private String longnameEN;
	private String ID6;
	private String product_code;
	private String content_platform;
	private String company_code;
	private int region;
	private byte[] key;
	private ArrayList<Integer> versions = new ArrayList<>();
	
	private String selectedVersion = "latest";
	
	public enum Region{
		EUR,
		USA,
		JAP,
		UKWN
	}

	public NUSTitleInformation(long titleID, String longnameEN, String ID6, String product_code,String content_platform,String company_code,int region, String[] versions) {
		setTitleID(titleID);
		setLongnameEN(longnameEN);
		setID6(ID6);	
		setProduct_code(product_code);
		setCompany_code(company_code);
		setContent_platform(content_platform);
		setRegion(region);
		for(String s : versions){
			if(s != null){
				this.versions.add(Integer.parseInt(s));
			}
		}
	}

	public NUSTitleInformation() {
		// TODO Auto-generated constructor stub
	}

	public NUSTitleInformation(long titleID, String longnameEN, String ID6, String product_code,String content_platform,String company_code,int region) {
		this(titleID, longnameEN, ID6, product_code,content_platform,company_code,region,new String[1]);
	}

	public Region getRegionAsRegion() {		
		switch (region) {
        	case 1:  return Region.JAP;                 
        	case 2:  return  Region.USA;
        	case 4:  return  Region.EUR;
        	default: return  Region.UKWN;
		}
	}

	public String getContent_platform() {
		return content_platform;
	}

	public void setContent_platform(String content_platform) {
		this.content_platform = content_platform;
	}

	public String getCompany_code() {
		return company_code;
	}

	public void setCompany_code(String company_code) {
		this.company_code = company_code;
	}

	public String getProduct_code() {
		return product_code;
	}

	public void setProduct_code(String product_code) {
		this.product_code = product_code;
	}

	public long getTitleID() {
		return titleID;
	}

	public void setTitleID(long titleID) {
		this.titleID = titleID;
	}

	public String getLongnameEN() {
		return longnameEN;
	}

	public void setLongnameEN(String longnameEN) {
		this.longnameEN = longnameEN;
	}

	public String getID6() {
		return ID6;
	}

	public void setID6(String iD6) {
		ID6 = iD6;
	}	
	
	public int getRegion() {
		return region;
	}

	public void setRegion(int region) {
		this.region = region;
	}

	public String getTitleIDAsString() {
		return String.format("%08X-%08X", titleID>>32,titleID<<32>>32);
		
	}
	
	@Override
	public String toString(){
		String result =  getTitleIDAsString() + ";" + region +";" + getContent_platform() + ";" + getCompany_code() + ";"+ getProduct_code()+ ";" + getID6() + ";" + getLongnameEN();
		for(Integer i :versions){
			result += ";" + i;
		}
		//result += ";" + getSelectedVersion();
		return result;
	}

	@Override
	public int compareTo(NUSTitleInformation o) {
		return getLongnameEN().compareTo(o.getLongnameEN());
	}

	public void init(NUSTitleInformation n) {
		setTitleID(n.getTitleID());
		setRegion(n.region);
		setCompany_code(n.company_code);
		setContent_platform(n.content_platform);
		setID6(n.ID6);
		setLongnameEN(n.longnameEN);
		setProduct_code(n.product_code);
		setKey(n.key);
	}

	public byte[] getKey() {
		return key;
	}

	public void setKey(byte[] key) {
		this.key = key;
	}
	
	@Override
	public boolean equals(Object o){		
		return titleID == ((NUSTitleInformation)o).titleID;
	}

	public String getLatestVersion() {
		String result = "latest";
		if(versions != null && !versions.isEmpty()){
			result = versions.get(versions.size()-1) + "";
		}
		return result;
	}

	public List<Integer> getAllVersions() {
		return versions;
	}
	
	public List<String> getAllVersionsAsString() {
		List<String> list = new ArrayList<>();
		if(versions != null && !versions.isEmpty()){
			for(Integer v: versions){
				list.add(v + "");
			}			
		}
		list.add("latest");
		return list;
	}

	public void setSelectedVersion(String string) {
		this.selectedVersion = string;		
	}
	
	public int getSelectedVersion() {
		int version = -1;
		if(this.selectedVersion == "latest"){
			version = -1;
		}else{
			try{
				version = Integer.parseInt(this.selectedVersion);
			}catch(Exception e){
				
			}
		}		
		return version;		
	}
}
