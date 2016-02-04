package de.mas.jnustool;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import de.mas.jnustool.util.Decryption;
import de.mas.jnustool.util.Downloader;
import de.mas.jnustool.util.ExitException;
import de.mas.jnustool.util.Settings;

public class FEntry
{
	private FST fst;

	public static int DIR_FLAG = 1;
	public static int NOT_IN_NUS_TITLE_FLAG = 0x80;
	public static int EXTRACT_WITH_HASH_FLAG = 0x440;
	public static int CHANGE_OFFSET_FLAG = 0x04;

	private boolean dir = false;
	private boolean in_nus_title = false;
	private boolean extract_withHash = false;

	private String fileName = "";
	private String path = "";
	private long fileOffset = 0L;
	private long fileLength = 0;
	private int contentID = 0;
	private int NUSContentID = 0;
	private List<String> pathList;

	public FEntry(String path, String filename, int contentID, int NUSContentID, long fileOffset, long fileLength, boolean dir,
				  boolean in_nus_title, boolean extract_withHash, List<String> pathList, FST fst)
	{
		setPath(path);
		setFileName(filename);
		setContentID(contentID);
		setFileOffset(fileOffset);
		setFileLength(fileLength);
		setDir(dir);
		setInNusTitle(in_nus_title);
		setExtractWithHash(extract_withHash);
		setNUSContentID(NUSContentID);
		setPathList(pathList);
		this.fst = fst;
	}

	public boolean isDir()
	{
		return dir;
	}

	private void setDir(boolean dir)
	{
		this.dir = dir;
	}

	public boolean isInNUSTitle()
	{
		return in_nus_title;
	}

	private void setInNusTitle(boolean in_nus_title)
	{
		this.in_nus_title = in_nus_title;
	}

	public boolean isExtractWithHash()
	{
		return extract_withHash;
	}

	private void setExtractWithHash(boolean extract_withHash)
	{
		this.extract_withHash = extract_withHash;
	}

	public String getFileName()
	{
		return fileName;
	}

	private void setFileName(String filename)
	{
		this.fileName = filename;
	}

	public String getFullPath()
	{
		return path + fileName;
	}

	private void setPath(String path)
	{
		this.path = path;
	}

	public long getFileOffset()
	{
		return fileOffset;
	}

	private void setFileOffset(long fileOffset)
	{
		this.fileOffset = fileOffset;
	}

	public int getContentID()
	{
		return contentID;
	}

	private void setContentID(int contentID)
	{
		this.contentID = contentID;
	}

	public long getFileLength()
	{
		return fileLength;
	}

	private void setFileLength(long fileLength)
	{
		this.fileLength = fileLength;
	}

	@Override
	public String toString()
	{
		return getFullPath() + " Content ID:" + contentID + " Size: " + fileLength + "MB  Offset: " + fileOffset;
	}

	public int getNUSContentID()
	{
		return NUSContentID;
	}

	private void setNUSContentID(int nusContentID)
	{
		NUSContentID = nusContentID;
	}

	private void createFolder()
	{
		long titleID = getTitleID();
		String[] path = getFullPath().split("/");
		File f = new File(String.format("%016X", titleID));
		if (!f.exists())
		{
			f.mkdir();
		}

		String folder = String.format("%016X", titleID) + "/";
		File folder_;
		for (int i = 0; i < path.length - 1; i++)
		{
			if (!path[i].equals(""))
			{
				folder += path[i] + "/";
				folder_ = new File(folder);
				if (!folder_.exists())
				{
					try
					{
						Files.createDirectory(folder_.toPath());
					} catch (IOException e)
					{
						e.printStackTrace();
					}
				}
			}
		}

	}

	public String getDownloadPath()
	{
		String[] path = getFullPath().split("/");
		String folder = String.format("%016X", getTitleID()) + "/";
		for (int i = 0; i < path.length - 1; i++)
		{
			if (!path[i].equals(""))
			{
				folder += path[i] + "/";
			}
		}
		return folder;
	}

	public void downloadAndDecrypt() throws ExitException
	{
		createFolder();
		long titleID = getTitleID();
		File f = new File(String.format("%016X", titleID) + "/" + getFullPath().substring(1, getFullPath().length()));
		if (f.exists())
		{
			if (f.length() == getFileLength())
			{
				System.out.println("Skipping: " + String.format("%8.2f MB ", getFileLength() / 1024.0 / 1024.0) + getFullPath());
				return;
			}
		}
		try
		{
			if (Settings.useCachedFiles)
			{
				f = new File(getContentPath());
				if (f.exists())
				{
					if (f.length() == fst.getTmd().contents[this.getContentID()].size)
					{
						System.out.println("Decrypting: " + String.format("%8.2f MB ", getFileLength() / 1024.0 / 1024.0) + getFullPath());
						Decryption decrypt = new Decryption(fst.getTmd().getNUSTitle().getTicket());
						decrypt.decrypt(this, getDownloadPath());
						return;
					} else
					{
						if (!Settings.downloadWhenCachedFilesMissingOrBroken)
						{
							System.out.println("Cached content has the wrong size! Please check your: " + getContentPath() + " Downloading not allowed");
							if (!Settings.skipBrokenFiles)
							{
								throw new ExitException("");
							} else
							{
								System.out.println("Ignoring the missing file: " + this.getFileName());
							}
						} else
						{
							System.out.println("Content missing. Downloading the file from the server: " + this.getFileName());
						}

					}
				} else
				{
					if (!Settings.downloadWhenCachedFilesMissingOrBroken)
					{
						System.out.println("Content missing. Downloading not allowed");
						if (!Settings.skipBrokenFiles)
						{
							throw new ExitException("");
						} else
						{
							System.out.println("Ignoring the missing file: " + this.getFileName());
						}
					} else
					{
						System.out.println("Content missing. Downloading the file from the server: " + this.getFileName());
					}
				}
			}
			System.out.println("Downloading: " + String.format("%8.2f MB ", getFileLength() / 1024.0 / 1024.0) + getFullPath());
			Downloader.getInstance().downloadAndDecrypt(this);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public List<String> getPathList()
	{
		return pathList;
	}

	public void setPathList(List<String> pathList)
	{
		this.pathList = pathList;
	}

	public String getContentPath()
	{
		return fst.getTmd().getContentPath() + "/" + String.format("%08X", getNUSContentID()) + ".app";
	}

	public long getTitleID()
	{
		return fst.getTmd().titleID;
	}

	public TIK getTicket()
	{
		return fst.getTmd().getNUSTitle().getTicket();
	}
}