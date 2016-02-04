package de.mas.jnustool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.mas.jnustool.util.ConversionUtils;

public class FST
{
	private TitleMetaData tmd;
	long totalContentSize = 0L;
	long totalContentSizeInNUS = 0L;

	List<FEntry> fileEntries = new ArrayList<>();

	int totalContentCount = 0;

	int totalEntries = 0;
	int dirEntries = 0;

	private Directory FSTDirectory = new Directory("root");

	private Directory contentDirectory = new Directory("root");

	public FST(byte[] decryptedData, TitleMetaData tmd) throws IOException
	{
		parse(decryptedData, tmd);
		setTmd(tmd);
		buildDirectories();
	}

	private void buildDirectories()
	{
		String contentFolder;
		Directory curContent;
		for (FEntry f : getFileEntries())
		{
			if (!f.isDir() && f.isInNUSTitle())
			{
				contentFolder = String.format("%08X", tmd.contents[f.getContentID()].ID);

				if (!contentDirectory.containsFolder(contentFolder))
				{
					Directory newDir = new Directory(contentFolder);
					contentDirectory.addFolder(newDir);
				}
				curContent = contentDirectory.getFolder(contentFolder);

				Directory current = FSTDirectory;
				int i = 0;

				for (String s : f.getPathList())
				{
					i++;

					//Content
					if (curContent.containsFolder(s))
					{
						curContent = curContent.get(s);
					} else
					{
						Directory newDir = new Directory(s);
						curContent.addFolder(newDir);
						curContent = newDir;
					}
					if (i == f.getPathList().size())
					{
						curContent.addFile(f);
					}

					//FST
					if (current.containsFolder(s))
					{
						current = current.get(s);
					} else
					{
						Directory newDir = new Directory(s);
						current.addFolder(newDir);
						current = newDir;
					}
					if (i == f.getPathList().size())
					{
						current.addFile(f);
					}
				}
			}
		}
	}

	private void parse(byte[] decryptedData, TitleMetaData tmd) throws IOException
	{
		if (!Arrays.equals(Arrays.copyOfRange(decryptedData, 0, 3), new byte[]{0x46, 0x53, 0x54}))
		{
			System.err.println("Not a FST. Maybe a wrong key?");
			throw new IllegalArgumentException("File not a FST");
		}

		this.totalContentCount = ConversionUtils.getIntFromBytes(decryptedData, 8);
		int base_offset = 0x20 + totalContentCount * 0x20;

		this.totalEntries = ConversionUtils.getIntFromBytes(decryptedData, base_offset + 8);
		int nameOff = base_offset + totalEntries * 0x10;

		int level = 0;
		int[] LEntry = new int[16];
		int[] Entry = new int[16];

		for (int i = 0; i < this.totalEntries; i++)
		{
			boolean dir = false;
			boolean in_nus_title = true;
			boolean extract_withHash = false;

			long fileOffset;
			long fileLength;
			int type;
			int contentID;

			String filename;
			String path = "";

			if (level > 0)
			{
				while (LEntry[level - 1] == i)
				{
					level--;
				}
			}

			int offset = base_offset + i * 0x10;

			//getting the type
			type = (int) decryptedData[offset] + 128;
			if ((type & FEntry.DIR_FLAG) == 1)
			{
				dir = true;
			}
			if ((type & FEntry.NOT_IN_NUS_TITLE_FLAG) == 0)
			{
				in_nus_title = false;
			}

			//getting Name
			decryptedData[offset] = 0;
			int nameOff_entry_offset = ConversionUtils.getIntFromBytes(decryptedData, offset);
			int j = 0;
			int nameOff_entry = nameOff + nameOff_entry_offset;
			while (decryptedData[nameOff_entry + j] != 0)
			{
				j++;
			}
			filename = new String(Arrays.copyOfRange(decryptedData, nameOff_entry, nameOff_entry + j));

			//getting offsets. save in two ways
			offset += 4;
			fileOffset = (long) ConversionUtils.getIntFromBytes(decryptedData, offset);
			offset += 4;
			fileLength = ConversionUtils.getIntAsLongFromBytes(decryptedData, offset);
			int nextOffset = (int) fileLength;

			//grabbing flags
			offset += 4;
			int flags = ConversionUtils.getShortFromBytes(decryptedData, offset);
			if ((flags & FEntry.EXTRACT_WITH_HASH_FLAG) > 0)
			{
				extract_withHash = true;
			}
			if ((flags & FEntry.CHANGE_OFFSET_FLAG) == 0)
			{
				fileOffset <<= 5;
			}

			//grabbing content id
			offset += 2;
			contentID = ConversionUtils.getShortFromBytes(decryptedData, offset);

			//remember total size
			this.totalContentSize += fileLength;
			if (in_nus_title)
			{
				this.totalContentSizeInNUS += fileLength;
			}

			List<String> pathList = new ArrayList<>();
			//getting the full path of entry
			if (dir)
			{
				dirEntries++;
				Entry[level] = i;
				LEntry[level++] = nextOffset;
				if (level > 15)    // something is wrong!
				{
					break;
				}
			} else
			{
				StringBuilder stringBuilder = new StringBuilder();
				int k;
				int nameOffOff, nameOff_entryPath;

				for (j = 0; j < level; ++j)
				{
					nameOffOff = ConversionUtils.getIntFromBytes(decryptedData, base_offset + Entry[j] * 0x10);
					k = 0;
					nameOff_entryPath = nameOff + nameOffOff;
					while (decryptedData[nameOff_entryPath + k] != 0)
					{
						k++;
					}
					String temporaryName = new String(Arrays.copyOfRange(decryptedData, nameOff_entryPath, nameOff_entryPath + k));
					if (!temporaryName.equals(""))
					{
						pathList.add(temporaryName);
					}

					stringBuilder.append(temporaryName);
					stringBuilder.append("/");
				}
				path = stringBuilder.toString();
			}

			//add this to the List!
			fileEntries.add(new FEntry(path, filename, contentID, tmd.contents[contentID].ID, fileOffset, fileLength, dir, in_nus_title, extract_withHash, pathList, this));
			//System.out.println(fileEntries.get(i));
		}

	}

	public long getTotalContentSizeInNUS()
	{
		return totalContentSizeInNUS;
	}

	public List<FEntry> getFileEntries()
	{
		return fileEntries;
	}

	public int getTotalEntries()
	{
		return totalEntries;
	}

	@Override
	public String toString()
	{
		return "entryCount: " + totalContentCount + " entries: " + totalEntries;
	}

	public int getFileCount()
	{
		int i = 0;
		for (FEntry f : getFileEntries())
		{
			if (!f.isDir())
			{
				i++;
			}
		}
		return i;
	}

	public int getFileCountInNUS()
	{
		int i = 0;
		for (FEntry f : getFileEntries())
		{
			if (!f.isDir() && f.isInNUSTitle())
			{
				i++;
			}
		}
		return i;
	}

	public Directory getFSTDirectory()
	{
		return FSTDirectory;
	}

	public TitleMetaData getTmd()
	{
		return tmd;
	}

	public void setTmd(TitleMetaData tmd)
	{
		this.tmd = tmd;
	}
}