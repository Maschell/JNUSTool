package de.mas.jnustool.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import de.mas.jnustool.Settings;
import de.mas.jnustool.FEntry;
import de.mas.jnustool.NUSTitle;
import de.mas.jnustool.TitleDownloader;

public class NUSGUI extends JFrame {

    private static final long serialVersionUID = 4648172894076113183L;

    public NUSGUI(NUSTitle nus,Settings mode) {
        super();
        setSize(800, 600);
        getContentPane().setLayout(new BorderLayout(0, 0));
       
        final JCheckBoxTree cbt = new JCheckBoxTree(nus);
        JScrollPane qPane = new JScrollPane(cbt,
        	      JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        	      JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        this.getContentPane().add(qPane);
        
        
        JButton btnNewButton = new JButton("Download");
        btnNewButton.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) { 
        		new Thread(new Runnable() { public void run() { 
        			ForkJoinPool pool = ForkJoinPool.commonPool();
            		List<TitleDownloader> list = new ArrayList<>();
        			
        			
                    TreePath[] paths = cbt.getCheckedPaths();
                    for (TreePath tp : paths) {
                    	Object obj = tp.getPath()[tp.getPath().length-1];
                    	if(((DefaultMutableTreeNode)obj).getUserObject() instanceof FEntry){
                    		FEntry f = (FEntry) ((DefaultMutableTreeNode)obj).getUserObject();
                    		if(!f.isDir() &&  f.isInNUSTitle())
                    			list.add(new TitleDownloader(f));
                    	}
                    }
                    pool.invokeAll(list);
                    System.out.println("Done!");
        		}}).start();
        		
        	}
        });
        getContentPane().add(btnNewButton, BorderLayout.SOUTH);
        
        /*cbt.addCheckChangeEventListener(new JCheckBoxTree.CheckChangeEventListener() {
            public void checkStateChanged(JCheckBoxTree.CheckChangeEvent event) {
                System.out.println("event");
                TreePath[] paths = cbt.getCheckedPaths();
                for (TreePath tp : paths) {
                    for (Object pathPart : tp.getPath()) {
                        System.out.print(pathPart + ",");
                    }                   
                    System.out.println();
                }
            }           
        });*/
        
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
    }
}