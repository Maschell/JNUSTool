package de.mas.jnustool;

import java.util.Collection;
import java.util.TreeMap;

import javax.swing.tree.DefaultMutableTreeNode;

public class Directory {
	String name = "";
	TreeMap<String,Directory> folder = new TreeMap<>();	
	TreeMap<String,FEntry> files = new TreeMap<>();
	
	public Directory get(String s){
		return folder.get(s);
	}
	
	public Directory(String name){
		setName(name);
	}

	public boolean containsFolder(String s){
		return folder.containsKey(s);
	}
	
	public Directory getFolder(String s){
		return folder.get(s);
	}
	
	public Directory addFolder(Directory s){
		return folder.put(s.getName(),s);
	}
	public boolean containsFile(String s){
		return files.containsKey(s);
	}
	
	public FEntry getFile(String s){
		return files.get(s);
	}
	
	public FEntry addFile(FEntry s){
		return files.put(s.getFileName(),s);
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	
	public Collection<Directory> getFolder() {
		return  folder.values();
	}



	public  Collection<FEntry> getFiles() {
		return  files.values();
	}

	public void setFiles(TreeMap<String, FEntry> files) {
		this.files = files;
	}

	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(name + ":" + "\n");
		for(Directory d : folder.values()){
			sb.append(d + "\n");		
		}
		for(String s : files.keySet()){
			sb.append(s + "\n");			
		}
		return sb.toString();
	}
	
	public DefaultMutableTreeNode getNodes(){
		DefaultMutableTreeNode node = new DefaultMutableTreeNode(getName());
		
		for(Directory f: getFolder()){			
			node.add(f.getNodes());
		}		
		
		for(FEntry f: getFiles()){
			node.add(new DefaultMutableTreeNode(f));
		}
		return node;
	}
	

}
