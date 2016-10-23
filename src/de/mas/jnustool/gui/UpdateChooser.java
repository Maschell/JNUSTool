package de.mas.jnustool.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellEditor;

import de.mas.jnustool.Logger;
import de.mas.jnustool.Progress;
import de.mas.jnustool.ProgressUpdateListener;
import de.mas.jnustool.Starter;
import de.mas.jnustool.util.NUSTitleInformation;
import de.mas.jnustool.util.Settings;

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
    public UpdateChooser(final JFrame window) {
        super(new BorderLayout());        
        setSize(800, 650);

        Collections.sort(list_);
        
        output_.add(list_.get(0));
        String[] columnNames = { "TitleID", "Region", "Name","version"};
        String[][] tableData = new String[list_.size()][];
        int i = 0;
        HashMap<Integer,JComboBox<String>> comboboxes = new HashMap<>();
        for(NUSTitleInformation n: list_){
        	tableData[i] = new String[4];
        	tableData[i][0] = n.getTitleIDAsString();
        	tableData[i][1] = n.getRegionAsRegion().toString();    
        	tableData[i][2] = n.getLongnameEN();
        	tableData[i][3] = n.getLatestVersion();        	
        	
        	JComboBox<String> comboBox = new JComboBox<>();
            for(String v : n.getAllVersionsAsString()){
            	comboBox.addItem(v);
            }
            final int position = i;
            comboBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                	list_.get(position).setSelectedVersion((String) e.getItem());
                }
            });
            comboboxes.put(i,comboBox);
            
            i++;
        }
        
 
        table = new JTable(tableData, columnNames);
        
          
        EachRowEditor rowEditor = new EachRowEditor(table);
		
		for (Entry<Integer, JComboBox<String>> entry : comboboxes.entrySet())
		{
			rowEditor.setEditorAt(entry.getKey(), new DefaultCellEditor(entry.getValue()));
		}

        
        table.getColumn("version").setCellEditor(rowEditor);
   

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
        JButton btnDownloadEncrypted = new JButton("Download Encrypted Files");
        final JProgressBar progressBar_1 = new JProgressBar();
        panel.add(progressBar_1);
        progressBar_1.setValue(0);
        final Progress progress = new Progress();
        progress.setProgressUpdateListener(new ProgressUpdateListener() {
			
			@Override
			public void updatePerformed(Progress p) {
				progressBar_1.setValue((int)p.statusInPercent());
			}
		});
        
        
        btnDownloadMeta.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		if(!progress.isInProgress()){
	        		progress.clear();
	        		progress.operationStart();
	        		new Thread(new Runnable(){
						@Override
						public void run() {
							progressBar_1.setValue(0);
							progress.clear();
							Starter.downloadMeta(output_,progress);
							progress.operationFinish();
							Logger.messageBox("Finished");							
						}
	        			
	        		}).start();
	        		
        		}else{
        			Logger.messageBox("Operation still in progress, please wait");
        		}        		
        	}
        });
        panel.add(btnDownloadMeta);
        
        btnDownloadEncrypted.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		if(!progress.isInProgress()){
	        		progress.clear();
	        		progress.operationStart();
	        		new Thread(new Runnable(){
						@Override
						public void run() {
							progressBar_1.setValue(0);
							progress.clear();
							if(Settings.DL_ALL_VERSIONS){
								Starter.downloadEncryptedAllVersions(output_,progress);
							}else{
								Starter.downloadEncrypted(output_,progress);
							}
							progress.operationFinish();
							Logger.messageBox("Finished");							
						}
	        			
	        		}).start();
	        		
        		}else{
        			Logger.messageBox("Operation still in progress, please wait");
        		}        		
        	}
        });
        panel.add(btnDownloadEncrypted);
       
        btnNewButton.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {  
        		if(!progress.isInProgress()){
	        		synchronized (output_) {
	        			window.setVisible(false);
	                	output_.notifyAll();
	                }
        		}
        	}
        });
        
        table.addMouseListener(new MouseAdapter() {
	        public void mousePressed(MouseEvent me) {
	        	if(!progress.isInProgress()){
	            if (me.getClickCount() == 2) {
	            	synchronized (output_) {
	        			window.setVisible(false);
	                	output_.notifyAll();
	                }
	            }
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
        frame.setSize(672, 600);

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
    
    /**
     * each row TableCellEditor
     * 
     * @version 1.1 09/09/99
     * @author Nobuo Tamemasa
     */

	class EachRowEditor implements TableCellEditor {
	@SuppressWarnings("rawtypes")
	protected Hashtable editors;

	protected TableCellEditor editor, defaultEditor;
		
	JTable table;
	
	/**
	* Constructs a EachRowEditor. create default editor
	* 
	* @see TableCellEditor
	* @see DefaultCellEditor
	*/
	
    @SuppressWarnings("rawtypes")
	public EachRowEditor(JTable table) {
    	this.table = table;
    	editors = new Hashtable();
    	defaultEditor = new DefaultCellEditor(new JTextField());
    }

      /**
       * @param row
       *            table row
       * @param editor
       *            table cell editor
       */
      @SuppressWarnings("unchecked")
	  public void setEditorAt(int row, TableCellEditor editor) {
        editors.put(new Integer(row), editor);
      }

      public Component getTableCellEditorComponent(JTable table, Object value,
          boolean isSelected, int row, int column) {
        //editor = (TableCellEditor)editors.get(new Integer(row));
        //if (editor == null) {
        //  editor = defaultEditor;
        //}
        return editor.getTableCellEditorComponent(table, value, isSelected,
            row, column);
      }

      public Object getCellEditorValue() {
        return editor.getCellEditorValue();
      }

      public boolean stopCellEditing() {
        return editor.stopCellEditing();
      }

      public void cancelCellEditing() {
        editor.cancelCellEditing();
      }

      public boolean isCellEditable(EventObject anEvent) {
        selectEditor((MouseEvent) anEvent);
        return editor.isCellEditable(anEvent);
      }

      public void addCellEditorListener(CellEditorListener l) {
        editor.addCellEditorListener(l);
      }

      public void removeCellEditorListener(CellEditorListener l) {
        editor.removeCellEditorListener(l);
      }

      public boolean shouldSelectCell(EventObject anEvent) {
        selectEditor((MouseEvent) anEvent);
        return editor.shouldSelectCell(anEvent);
      }

      protected void selectEditor(MouseEvent e) {
        int row;
        if (e == null) {
          row = table.getSelectionModel().getAnchorSelectionIndex();
        } else {
          row = table.rowAtPoint(e.getPoint());
        }
        editor = (TableCellEditor) editors.get(new Integer(row));
        if (editor == null) {
          editor = defaultEditor;
        }
      }
    }
}
