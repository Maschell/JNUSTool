package de.mas.jnustool;

import java.util.Collection;
import java.util.TreeMap;

import javax.swing.tree.DefaultMutableTreeNode;
/**
 * A Class that represents a Directory of a NUSTitle file table.
 * Every directory can hold other directories and Objects that implements the IHasName Interface. Build in a tree structure.
 * @author Maschell
 *
 */
public class Directory<T extends IHasName> {
	String name = "";
	TreeMap<String,Directory<T>> folder = new TreeMap<>();	
	TreeMap<String,T> files = new TreeMap<>();
	
	/**
	 * 
	 * @param name Name of this Directory
	 */
	public Directory(String name){
		setName(name);
	}
	
	/**
	 * Checks if a sub directory with the given name exits
	 * @param name Name of the sub directory
	 * @return true is the sub directory exists
	 */
	public boolean containsFolder(String name){
		return folder.containsKey(name);
	}
	/**
	 * Returns a sub directory with the given name
	 * or {@code null} if this directory contains no mapping for the name.
	 * @param name Name of the sub directory
	 * @return The sub directory or null
	 */
	public Directory<T> getFolder(String name){
		return folder.get(name);
	}
	
	/**
	 * Adds a sub directory
	 * @param subDirectory the sub directory. 
	 * @return the previous directory associated with the same name, or
     *         {@code null} if there was no directoy with this name.
	 */
	public Directory<T> addFolder(Directory<T> subDirectory){
		return folder.put(subDirectory.getName(),subDirectory);
	}
	
	/**
	 * Checks if a object with the given name exits. This will NOT check the sub directories
	 * @param name name of the object
	 * @return true is a object with the same name exists.
	 */
	public boolean containsFile(String name){
		return files.containsKey(name);
	}
	
	public T getFile(String s){
		return files.get(s);
	}
	
	public T addFile(T s){
		return files.put(s.getName(),s);
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}	
	
	public Collection<Directory<T>> getFolder() {
		return  folder.values();
	}

	public  Collection<T> getFiles() {
		return  files.values();
	}

	public void setFiles(TreeMap<String, T> files) {
		this.files = files;
	}
	
	public DefaultMutableTreeNode getNodes(){
		DefaultMutableTreeNode node = new DefaultMutableTreeNode(getName());
		
		for(Directory<T> f: getFolder()){			
			node.add(f.getNodes());
		}		
		
		for(T f: getFiles()){
			node.add(new DefaultMutableTreeNode(f));
		}
		return node;
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(name + ":" + "\n");
		for(Directory<T> d : folder.values()){
			sb.append(d + "\n");		
		}
		for(String s : files.keySet()){
			sb.append(s + "\n");			
		}
		return sb.toString();
	}
	

}
