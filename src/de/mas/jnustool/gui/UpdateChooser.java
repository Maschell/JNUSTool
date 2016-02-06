package de.mas.jnustool.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import de.mas.jnustool.Progress;
import de.mas.jnustool.ProgressUpdateListener;
import de.mas.jnustool.Starter;
import de.mas.jnustool.util.NUSTitleInformation;

public class UpdateChooser extends JPanel {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	JTextArea output;
    JList<?> list; 
    JTable table;
    String newline = "\n";
    ListSelectionModel listSelectionModel;
    public UpdateChooser(JFrame window) {
        super(new BorderLayout());        
        setSize(800, 600);

        Collections.sort(list_);
        
        output_.add(list_.get(0));
        String[] columnNames = { "TitleID", "Region", "Name" };
        String[][] tableData = new String[list_.size()][];
        int i = 0;
        for(NUSTitleInformation n: list_){
        	tableData[i] = new String[3];
        	tableData[i][0] = n.getTitleIDAsString();
        	tableData[i][1] = n.getRegionAsRegion().toString();    
        	tableData[i][2] = n.getLongnameEN();
        	i++;
        }
        
 
        table = new JTable(tableData, columnNames);
        
   

        //table.setModel(tableModel);
        for (int c = 0; c < table.getColumnCount(); c++)
        {
            Class<?> col_class = table.getColumnClass(c);
            table.setDefaultEditor(col_class, null);        // remove editor
        }
        
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(1).setPreferredWidth(50);
        table.getColumnModel().getColumn(2).setPreferredWidth(350);
        
        listSelectionModel = table.getSelectionModel();
        listSelectionModel.addListSelectionListener(new SharedListSelectionHandler());
      

        	
	    table.setSelectionModel(listSelectionModel);
	
	    table.addMouseListener(new MouseAdapter() {
	        public void mousePressed(MouseEvent me) {
	            if (me.getClickCount() == 2) {
	            	synchronized (output_) {
	        			window.setVisible(false);
	                	output_.notifyAll();
	                }
	            }
	        }
	    });
	    
	    JScrollPane tablePane = new JScrollPane(table);
        //Build control area (use default FlowLayout).
        JPanel controlPane = new JPanel();

 
        listSelectionModel.setSelectionMode(
                ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
 
        //Build output area.
        output = new JTextArea(1, 10);
        output.setEditable(false);
        JScrollPane outputPane = new JScrollPane(output,
                         ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                         ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
 
        //Do the layout.
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        add(splitPane, BorderLayout.CENTER);
 
        JPanel topHalf = new JPanel();
        topHalf.setLayout(new BoxLayout(topHalf, BoxLayout.LINE_AXIS));
        JPanel tableContainer = new JPanel(new GridLayout(1,1));
        tableContainer.setBorder(BorderFactory.createTitledBorder(
                                                "Table"));
        tableContainer.add(tablePane);
        tablePane.setPreferredSize(new Dimension(420, 130));
        topHalf.setBorder(BorderFactory.createEmptyBorder(5,5,0,5));
        topHalf.add(tableContainer);
 
        topHalf.setMinimumSize(new Dimension(250, 50));
        topHalf.setPreferredSize(new Dimension(200, 110));
        splitPane.add(topHalf);
        JPanel listContainer = new JPanel(new GridLayout(1,1));
        add(listContainer, BorderLayout.NORTH);
        
        JPanel panel = new JPanel();
        add(panel, BorderLayout.SOUTH);
        JButton btnNewButton = new JButton("Open FST");
        panel.add(btnNewButton);
        JProgressBar progressBar;
        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        
        JButton btnDownloadMeta = new JButton("Download META");
        JProgressBar progressBar_1 = new JProgressBar();
        panel.add(progressBar_1);
        progressBar_1.setValue(0);
        Progress progress = new Progress();
        progress.setProgressUpdateListener(new ProgressUpdateListener() {
			
			@Override
			public void updatePerformed(Progress p) {
				progressBar_1.setValue((int)p.statusInPercent());
			}
		});
        
        btnDownloadMeta.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		if(progressBar_1.getValue() == 0 || progressBar_1.getValue() == 100){
	        		progressBar_1.setValue(1);
	        		progress.clear();
	        		new Thread(new Runnable(){
						@Override
						public void run() {
							Starter.downloadMeta(output_,progress);
							JOptionPane.showMessageDialog(window, "Finished");
						}
	        			
	        		}).start();
	        		
        		}else{
        			JOptionPane.showMessageDialog(window, "Operation still in progress, please wait");
        		}        		
        	}
        });
        panel.add(btnDownloadMeta);
        
       
        btnNewButton.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {        		
        		synchronized (output_) {
        			window.setVisible(false);
                	output_.notifyAll();
                }
        	}
        });
 
        JPanel bottomHalf = new JPanel(new BorderLayout());
        bottomHalf.add(controlPane, BorderLayout.PAGE_START);
        bottomHalf.add(outputPane, BorderLayout.CENTER);
        
    }
  
	private static List<NUSTitleInformation> output_;
    static List<NUSTitleInformation> list_;
    public static void createAndShowGUI(List<NUSTitleInformation> list,List<NUSTitleInformation> result) {
        //Create and set up the window.
        JFrame frame = new JFrame("Select the title");
 
        //Create and set up the content pane.
        list_ = list;
        output_ =result;
        UpdateChooser demo = new UpdateChooser(frame);
        demo.setOpaque(true);
        frame.setContentPane(demo);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
       
        //Display the window.
        frame.setSize(610, 600);

        frame.setResizable(false);
        frame.setVisible(true);
		
    }
 
    class SharedListSelectionHandler implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent e) { 
            ListSelectionModel lsm = (ListSelectionModel)e.getSource();
  
            if (lsm.isSelectionEmpty()) {
                
            } else {
                // Find out which indexes are selected.
                int minIndex = lsm.getMinSelectionIndex();
                int maxIndex = lsm.getMaxSelectionIndex();
	            output_.clear();
                for (int i = minIndex; i <= maxIndex; i++) {
                    if (lsm.isSelectedIndex(i)) {
                    	if(!output_.contains(list_.get(i))){
                    		output_.add(list_.get(i));          
                    	}
                    }
                }
            }
            
        }
    }
}
