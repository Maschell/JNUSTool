package de.mas.wiiu.jnus.jnustool;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import de.mas.wiiu.jnus.jnustool.gui.NUSGUI;
import de.mas.wiiu.jnus.jnustool.util.Settings;

public class Logger {

	public static void log(String string) {
		NUSGUI.output.append(string + "\n");
		NUSGUI.output.setCaretPosition(NUSGUI.output.getDocument().getLength());
		if(Settings.logToPrintLn) System.out.println(string);		
	}

	public static void messageBox(String string) {
		JOptionPane.showMessageDialog(new JFrame(), string);		
	}
	
}
