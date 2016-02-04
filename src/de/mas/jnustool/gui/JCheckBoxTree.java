package de.mas.jnustool.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.EventListener;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.event.EventListenerList;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import de.mas.jnustool.FEntry;
import de.mas.jnustool.NUSTitle;

/**
 * Based on
 * http://stackoverflow.com/questions/21847411/java-swing-need-a-good-quality-developed-jtree-with-checkboxes/21851201#21851201
 */
public class JCheckBoxTree extends JTree
{
	private static final long serialVersionUID = -4194122328392241790L;

	JCheckBoxTree selfPointer = this;

	// Defining data structure that will enable to fast check-indicate the state of each node
	// It totally replaces the "selection" mechanism of the JTree
	private class CheckedNode
	{
		boolean isSelected;
		boolean hasChildren;
		boolean allChildrenSelected;

		public CheckedNode(boolean isSelected_, boolean hasChildren_, boolean allChildrenSelected_)
		{
			isSelected = isSelected_;
			hasChildren = hasChildren_;
			allChildrenSelected = allChildrenSelected_;
		}
	}

	HashMap<TreePath, CheckedNode> nodesCheckingState;
	HashSet<TreePath> checkedPaths = new HashSet<TreePath>();

	// Defining a new event type for the checking mechanism and preparing event-handling mechanism
	protected EventListenerList listenerList = new EventListenerList();

	public class CheckChangeEvent extends EventObject
	{
		private static final long serialVersionUID = -8100230309044193368L;

		public CheckChangeEvent(Object source)
		{
			super(source);
		}
	}

	public interface CheckChangeEventListener extends EventListener
	{
		void checkStateChanged(CheckChangeEvent event);
	}

	void fireCheckChangeEvent(CheckChangeEvent evt)
	{
		Object[] listeners = listenerList.getListenerList();
		for (int i = 0; i < listeners.length; i++)
		{
			if (listeners[i] == CheckChangeEventListener.class)
			{
				((CheckChangeEventListener) listeners[i + 1]).checkStateChanged(evt);
			}
		}
	}

	// Override
	public void setModel(TreeModel newModel)
	{
		super.setModel(newModel);
		resetCheckingState();
	}

	// New method that returns only the checked paths (totally ignores original "selection" mechanism)
	public TreePath[] getCheckedPaths()
	{
		return checkedPaths.toArray(new TreePath[checkedPaths.size()]);
	}

	private void resetCheckingState()
	{
		nodesCheckingState = new HashMap<>();
		checkedPaths = new HashSet<>();
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) getModel().getRoot();
		if (node == null)
		{
			return;
		}
		addSubtreeToCheckingStateTracking(node);
	}

	// Creating data structure of the current model for the checking mechanism
	private void addSubtreeToCheckingStateTracking(DefaultMutableTreeNode node)
	{
		TreeNode[] path = node.getPath();
		TreePath tp = new TreePath(path);
		CheckedNode cn = new CheckedNode(false, node.getChildCount() > 0, false);
		nodesCheckingState.put(tp, cn);
		for (int i = 0; i < node.getChildCount(); i++)
		{
			addSubtreeToCheckingStateTracking((DefaultMutableTreeNode) tp.pathByAddingChild(node.getChildAt(i)).getLastPathComponent());
		}
	}

	// Overriding cell renderer by a class that ignores the original "selection" mechanism
	// It decides how to show the nodes due to the checking-mechanism
	private class CheckBoxCellRenderer extends JPanel implements TreeCellRenderer
	{
		private static final long serialVersionUID = -7341833835878991719L;
		JCheckBox checkBox;

		public CheckBoxCellRenderer()
		{
			super();
			this.setLayout(new BorderLayout());
			checkBox = new JCheckBox();
			add(checkBox, BorderLayout.CENTER);
			setOpaque(false);
		}

		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value,
													  boolean selected, boolean expanded, boolean leaf, int row,
													  boolean hasFocus)
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
			Object obj = node.getUserObject();
			TreePath tp = new TreePath(node.getPath());
			CheckedNode cn = nodesCheckingState.get(tp);
			if (cn == null)
			{
				return this;
			}
			checkBox.setSelected(cn.isSelected);
			if (obj instanceof FEntry)
			{
				FEntry f = (FEntry) obj;
				checkBox.setText(f.getFileName());
			} else
			{
				checkBox.setText(obj.toString());
			}

			checkBox.setOpaque(cn.isSelected && cn.hasChildren && !cn.allChildrenSelected);
			return this;
		}
	}

	public JCheckBoxTree(NUSTitle nus)
	{
		super();


		setModel(new DefaultTreeModel(nus.getFst().getFSTDirectory().getNodes()));

		// Disabling toggling by double-click
		this.setToggleClickCount(0);
		// Overriding cell renderer by new one defined above
		CheckBoxCellRenderer cellRenderer = new CheckBoxCellRenderer();
		this.setCellRenderer(cellRenderer);

		// Overriding selection model by an empty one
		DefaultTreeSelectionModel defaultTreeSelectionModel = new DefaultTreeSelectionModel()
		{
			private static final long serialVersionUID = -8190634240451667286L;

			// Totally disabling the selection mechanism
			public void setSelectionPath(TreePath path)
			{
			}

			public void addSelectionPath(TreePath path)
			{
			}

			public void removeSelectionPath(TreePath path)
			{
			}

			public void setSelectionPaths(TreePath[] pPaths)
			{
			}
		};

		// Calling checking mechanism on mouse click
		this.addMouseListener(new MouseListener()
		{
			public void mouseClicked(MouseEvent arg0)
			{
				TreePath tp = selfPointer.getPathForLocation(arg0.getX(), arg0.getY());
				if (tp == null)
				{
					return;
				}
				boolean checkMode = !nodesCheckingState.get(tp).isSelected;
				checkSubTree(tp, checkMode);
				updatePredecessorsWithCheckMode(tp, checkMode);
				// Firing the check change event
				fireCheckChangeEvent(new CheckChangeEvent(new Object()));
				// Repainting tree after the data structures were updated
				selfPointer.repaint();
			}

			public void mouseEntered(MouseEvent arg0)
			{
			}

			public void mouseExited(MouseEvent arg0)
			{
			}

			public void mousePressed(MouseEvent arg0)
			{
			}

			public void mouseReleased(MouseEvent arg0)
			{
			}
		});

		this.setSelectionModel(defaultTreeSelectionModel);
	}

	// When a node is checked/unchecked, updating the states of the predecessors
	protected void updatePredecessorsWithCheckMode(TreePath tp, boolean check)
	{
		TreePath parentPath = tp.getParentPath();
		// If it is the root, stop the recursive calls and return
		if (parentPath == null)
		{
			return;
		}
		CheckedNode parentCheckedNode = nodesCheckingState.get(parentPath);
		DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) parentPath.getLastPathComponent();
		parentCheckedNode.allChildrenSelected = true;
		parentCheckedNode.isSelected = false;
		for (int i = 0; i < parentNode.getChildCount(); i++)
		{
			TreePath childPath = parentPath.pathByAddingChild(parentNode.getChildAt(i));
			CheckedNode childCheckedNode = nodesCheckingState.get(childPath);
			// It is enough that even one subtree is not fully selected
			// to determine that the parent is not fully selected
			if (!childCheckedNode.allChildrenSelected)
			{
				parentCheckedNode.allChildrenSelected = false;
			}
			// If at least one child is selected, selecting also the parent
			if (childCheckedNode.isSelected)
			{
				parentCheckedNode.isSelected = true;
			}
		}
		if (parentCheckedNode.isSelected)
		{
			checkedPaths.add(parentPath);
		} else
		{
			checkedPaths.remove(parentPath);
		}
		// Go to upper predecessor
		updatePredecessorsWithCheckMode(parentPath, check);
	}

	// Recursively checks/un-checks a subtree
	protected void checkSubTree(TreePath tp, boolean check)
	{
		CheckedNode cn = nodesCheckingState.get(tp);
		cn.isSelected = check;
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) tp.getLastPathComponent();
		for (int i = 0; i < node.getChildCount(); i++)
		{
			checkSubTree(tp.pathByAddingChild(node.getChildAt(i)), check);
		}
		cn.allChildrenSelected = check;
		if (check)
		{
			checkedPaths.add(tp);
		} else
		{
			checkedPaths.remove(tp);
		}
	}
}