package de.mas.jnustool;

import java.util.concurrent.Callable;

public class FEntryDownloader implements Callable<Integer>
{
	private FEntry fEntry;

	public void setTitle(FEntry fEntry)
	{
		this.fEntry = fEntry;
	}

	public FEntryDownloader(FEntry fEntry)
	{
		setTitle(fEntry);
	}

	@Override
	public Integer call() throws Exception
	{
		fEntry.downloadAndDecrypt();
		return null;
	}
}