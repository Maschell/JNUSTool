package de.mas.jnustool.gui;

import de.mas.jnustool.FEntry;
import de.mas.jnustool.FEntryDownloader;
import de.mas.jnustool.NUSTitle;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

public class NintendoUpdateServerGUI extends JFrame
{
	public static String applicationName = "JNUSTool";
	public static double version = 0.02;
	public static String author = "Maschell";
	public static String applicationTitle = applicationName + " v" + version + " by " + author;

	private JCheckBoxTree checkBoxTree;

	private NUSTitle title;

	public NintendoUpdateServerGUI(NUSTitle title)
	{
		this.title = title;

		setFrameProperties();
		addCheckBoxTree();
		addDownloadButton();
	}

	private void addDownloadButton()
	{
		JButton downloadButton = new JButton("Download");
		downloadButton.addActionListener(e -> new Thread(this::downloadMultiThreaded).start());
		getContentPane().add(downloadButton, BorderLayout.SOUTH);
	}

	private void addCheckBoxTree()
	{
		checkBoxTree = new JCheckBoxTree(title);
		JScrollPane checkBoxTreeScrollPane = new JScrollPane(checkBoxTree,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		this.getContentPane().add(checkBoxTreeScrollPane);
	}

	private void setFrameProperties()
	{
		setTitle(applicationTitle);
		setSize(800, 600);
		getContentPane().setLayout(new BorderLayout(0, 0));
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	}

	private void downloadMultiThreaded()
	{
		ForkJoinPool pool = ForkJoinPool.commonPool();
		List<FEntryDownloader> list = new ArrayList<>();
		TreePath[] paths = checkBoxTree.getCheckedPaths();
		for (TreePath path : paths)
		{
			Object obj = path.getPath()[path.getPath().length - 1];
			if (((DefaultMutableTreeNode) obj).getUserObject() instanceof FEntry)
			{
				FEntry f = (FEntry) ((DefaultMutableTreeNode) obj).getUserObject();
				if (!f.isDir() && f.isInNUSTitle())
				{
					list.add(new FEntryDownloader(f));
				}
			}
		}
		pool.invokeAll(list);
		System.out.println("Done!");
	}
}