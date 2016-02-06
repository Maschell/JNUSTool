package de.mas.jnustool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import de.mas.jnustool.gui.NintendoUpdateServerGUI;
import de.mas.jnustool.util.Downloader;
import de.mas.jnustool.util.ExitException;
import de.mas.jnustool.util.ConversionUtils;

public class NUSClient
{
	public static void main(String[] arguments)
	{
		System.out.println(NintendoUpdateServerGUI.applicationTitle);
		System.out.println();

		try
		{
			parseConfiguration();
		} catch (IOException ioException)
		{
			ioException.printStackTrace();
			System.err.println("Error while reading config! Needs to be:");
			System.err.println("DOWNLOAD URL BASE");
			System.err.println("COMMON KEY");

			return;
		}

		parseArguments(arguments);
	}

	private static void parseArguments(String[] arguments)
	{
		if (arguments.length != 0)
		{
			long titleID = ConversionUtils.StringToLong(arguments[0]);
			String key = null;
			if (arguments.length > 1 && arguments[1].length() == 32)
			{
				key = arguments[1].substring(0, 32);
			}
			NintendoUpdateServerGUI nusGUI;
			try
			{
				nusGUI = new NintendoUpdateServerGUI(new NUSTitle(titleID, key));
			} catch (ExitException e)
			{
				System.out.println("Error: " + e.getMessage());
				return;
			}

			nusGUI.setVisible(true);
		} else
		{
			System.out.println("Need parameters: TITLE_ID [KEY]");
		}
	}

	public static void parseConfiguration() throws IOException
	{
		BufferedReader bufferedReader = new BufferedReader(new FileReader(new File("config")));
		Downloader.URL_BASE = bufferedReader.readLine();
		ConversionUtils.commonKey = ConversionUtils.hexStringToByteArray(bufferedReader.readLine());
		bufferedReader.close();
	}
}