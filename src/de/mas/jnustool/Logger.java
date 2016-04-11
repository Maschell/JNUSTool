package de.mas.jnustool;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import de.mas.jnustool.gui.NUSGUI;

public class Logger {

	public static void log(String string) {
		NUSGUI.output.append(string + "\n");
		NUSGUI.output.setCaretPosition(NUSGUI.output.getDocument().getLength());
		//System.out.println(string);		
	}

	public static void messageBox(String string) {
		JOptionPane.showMessageDialog(new JFrame(), string);		
	}
	
}
