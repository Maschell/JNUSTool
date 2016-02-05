package de.mas.jnustool.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import de.mas.jnustool.FEntry;
import de.mas.jnustool.NUSTitle;
import de.mas.jnustool.util.Settings;

public class NUSGUI extends JFrame {

    private static final long serialVersionUID = 4648172894076113183L;
    public static JTextArea output = new JTextArea(1,10);
    public NUSGUI(NUSTitle nus,Settings mode) {
        super();
        this.setResizable(false);
        setSize(600, 768);
       setTitle(String.format("%016X", nus.getTitleID()));
        getContentPane().setLayout(null);
        
        JSplitPane splitPane = new JSplitPane();
        splitPane.setBounds(0, 0, 594, 726);
        splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        getContentPane().add(splitPane, BorderLayout.NORTH);
        JScrollPane qPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        	      JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
       
        this.getContentPane().add(splitPane);
        final JCheckBoxTree cbt = new JCheckBoxTree(nus);
        qPane.setViewportView(cbt);
        
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(0, 0));
        panel.add(qPane);
        splitPane.setLeftComponent(panel);
        
        JButton btnNewButton = new JButton("Download");
        panel.add(btnNewButton, BorderLayout.SOUTH);
       
        btnNewButton.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) { 
        		new Thread(new Runnable() { public void run() {
        			
            		List<FEntry> list = new ArrayList<>();
                    TreePath[] paths = cbt.getCheckedPaths();
                    for (TreePath tp : paths) {
                    	Object obj = tp.getPath()[tp.getPath().length-1];
                    	if(((DefaultMutableTreeNode)obj).getUserObject() instanceof FEntry){
                    		list.add((FEntry) ((DefaultMutableTreeNode)obj).getUserObject());                    		
                    	}
                    }
                  
        			nus.decryptFEntries(list);
        		}}).start();
        		
        	}
        });
        JScrollPane outputPane = new JScrollPane(output,
                         ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                         ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);	
        
        splitPane.setRightComponent(outputPane);
        
        splitPane.setDividerLocation(0.7);
        splitPane.setResizeWeight(0.7);

        splitPane.setEnabled(false);
        
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
    }
}