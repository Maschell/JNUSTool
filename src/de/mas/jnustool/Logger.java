package de.mas.jnustool;

import de.mas.jnustool.gui.NUSGUI;

public class Logger {

	public static void log(String string) {
		NUSGUI.output.append(string + "\n");
		NUSGUI.output.setCaretPosition(NUSGUI.output.getDocument().getLength());
		
	}
	
}
