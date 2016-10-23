package de.mas.jnustool.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import de.mas.jnustool.FEntry;
import de.mas.jnustool.Logger;
import de.mas.jnustool.NUSTitle;
import de.mas.jnustool.Progress;
import de.mas.jnustool.ProgressUpdateListener;

public class NUSGUI extends JFrame {

    private static final long serialVersionUID = 4648172894076113183L;
    public static JTextArea output = new JTextArea(1,10);
    public NUSGUI(NUSTitle nus) {
        super();
        this.setResizable(false);
        if(nus.getFst() == null){
            Logger.log("Error: Can't create GUI window without the FST. Please provide a key/title.key. To download the encrpyted files use the -dlEncrypted argument");
            System.exit(-1);
        }
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
        
        JPanel panel_1 = new JPanel();
        panel.add(panel_1, BorderLayout.SOUTH);
        
        JButton btnNewButton = new JButton("Download");
        panel_1.add(btnNewButton);
        
        final JProgressBar progressBar = new JProgressBar();
        panel_1.add(progressBar);
        
        progressBar.setValue(0);
        final Progress progress = new Progress();
        progress.setProgressUpdateListener(new ProgressUpdateListener() {
			
			@Override
			public void updatePerformed(Progress p) {
				progressBar.setValue((int)p.statusInPercent());
			}
		});
        final NUSTitle nuscpy = nus;
        btnNewButton.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) { 
        		if(!progress.isInProgress()){
	        		progress.clear();
	        		progress.operationStart();
	        		new Thread(new Runnable() { public void run() {
	        			
	            		List<FEntry> list = new ArrayList<>();
	                    TreePath[] paths = cbt.getCheckedPaths();
	                    for (TreePath tp : paths) {
	                    	Object obj = tp.getPath()[tp.getPath().length-1];
	                    	if(((DefaultMutableTreeNode)obj).getUserObject() instanceof FEntry){
	                    		list.add((FEntry) ((DefaultMutableTreeNode)obj).getUserObject());                    		
	                    	}
	                    }
	                    nuscpy.decryptFEntries(list, progress);
	        			progress.operationFinish();
	        			Logger.messageBox("Finished");
	        		}}).start();
        		}else{
        			Logger.messageBox("Operation still in progress, please wait");
        		}
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