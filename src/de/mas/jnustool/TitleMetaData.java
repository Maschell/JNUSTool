package de.mas.jnustool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;

import de.mas.jnustool.util.Downloader;
import de.mas.jnustool.util.ExitException;
import de.mas.jnustool.util.Settings;
import de.mas.jnustool.util.ConversionUtils;

public class TitleMetaData
{
	int signatureType;                                    // 0x000
	byte[] signature = new byte[0x100];        // 0x004
	byte[] issuer = new byte[0x40];            // 0x140
	byte version;                                        // 0x180
	byte CACRLVersion;                                    // 0x181
	byte signerCRLVersion;                                // 0x182
	long systemVersion;                                    // 0x184
	long titleID;                                        // 0x18C
	int titleType;                                        // 0x194
	short groupID;                                        // 0x198
	byte[] reserved = new byte[62];            // 0x19A
	int accessRights;                                    // 0x1D8
	short titleVersion;                                    // 0x1DC
	short contentCount;                                    // 0x1DE
	short bootIndex;                                        // 0x1E0
	byte[] SHA2 = new byte[32];            // 0x1E4
	ContentInfo[] contentInfoArray = new ContentInfo[64];    // 0x1E4
	Content[] contents;                                        // 0x1E4


	private NUSTitle nus;

	private long totalContentSize;

	public TitleMetaData(File tmd) throws IOException
	{
		parse(tmd);
		setTotalContentSize();
	}

	public TitleMetaData(byte[] downloadTMDToByteArray) throws IOException
	{
		if (downloadTMDToByteArray != null)
		{
			File tempFile = File.createTempFile("bla", "blubb");
			FileOutputStream fos = new FileOutputStream(tempFile);
			fos.write(downloadTMDToByteArray);
			fos.close();
			parse(tempFile);
			setTotalContentSize();
		} else
		{
			System.err.println("Invalid TMD");
			throw new IllegalArgumentException("Invalid TMD");
		}
	}

	private void parse(File tmd) throws IOException
	{
		RandomAccessFile f = new RandomAccessFile(tmd, "r");
		f.seek(0);
		this.signatureType = f.readInt();

		f.read(signature, 0, 0x100);
		f.seek(0x140);
		f.read(issuer, 0, 0x40);

		f.seek(0x180);
		this.version = f.readByte();
		this.CACRLVersion = f.readByte();
		this.signerCRLVersion = f.readByte();
		f.seek(0x184);
		this.systemVersion = f.readLong();
		this.titleID = f.readLong();
		this.titleType = f.readInt();
		this.groupID = f.readShort();
		f.seek(0x19A);
		f.read(reserved, 0, 62);
		f.seek(0x1D8);
		this.accessRights = f.readInt();
		this.titleVersion = f.readShort();
		this.contentCount = f.readShort();
		this.bootIndex = f.readShort();
		f.seek(0x1E4);
		f.read(SHA2, 0, 32);
		f.seek(0x204);

		short indexOffset;
		short commandCount;

		for (int i = 0; i < 64; i++)
		{
			f.seek(0x204 + (0x24 * i));
			indexOffset = f.readShort();
			commandCount = f.readShort();
			byte[] buffer = new byte[0x20];    //  16    0xB14
			f.read(buffer, 0, 0x20);
			this.contentInfoArray[i] = new ContentInfo(indexOffset, commandCount, buffer);
		}
		this.contents = new Content[contentCount];

		int ID;                        //	0	 0xB04
		short index;                    //	4    0xB08
		short type;                    //	6	 0xB0A
		long size;                    //	8	 0xB0C


		for (int i = 0; i < contentCount; i++)
		{
			f.seek(0xB04 + (0x30 * i));
			ID = f.readInt();
			index = f.readShort();
			type = f.readShort();
			size = f.readLong();
			byte[] buffer = new byte[0x20];    //  16    0xB14
			f.read(buffer, 0, 0x20);

			this.contents[i] = new Content(ID, index, type, size, buffer);
		}
		f.close();
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("signatureType:		").append(signatureType).append("\n");
		sb.append("signature:		").append(ConversionUtils.ByteArrayToString(signature)).append("\n");
		sb.append("issuer:			").append(ConversionUtils.ByteArrayToString(issuer)).append("\n");
		sb.append("version:		").append(version).append("\n");
		sb.append("CACRLVersion:		").append(CACRLVersion).append("\n");
		sb.append("signerCRLVersion:	").append(signerCRLVersion).append("\n");
		sb.append("systemVersion:		").append(String.format("%8X", systemVersion)).append("\n");
		sb.append("titleID:		").append(String.format("%8X", titleID)).append("\n");
		sb.append("titleType:		").append(titleType).append("\n");
		sb.append("groupID:		").append(groupID).append("\n");
		sb.append("reserved:		").append(ConversionUtils.ByteArrayToString(reserved)).append("\n");
		sb.append("accessRights:		").append(accessRights).append("\n");
		sb.append("titleVersion:		").append(titleVersion).append("\n");
		sb.append("contentCount:		").append(contentCount).append("\n");
		sb.append("bootIndex:		").append(bootIndex).append("\n");
		sb.append("SHA2:			").append(ConversionUtils.ByteArrayToString(SHA2)).append("\n");
		sb.append("contentInfos:		\n");
		for (int i = 0; i < contents.length - 1; i++)
		{
			sb.append("		").append(contentInfoArray[i]).append("\n");
		}
		sb.append("contents:			\n");
		for (int i = 0; i < contents.length - 1; i++)
		{
			sb.append("		").append(contents[i]).append("\n");
		}
		return sb.toString();
	}

	public void setTotalContentSize()
	{
		this.totalContentSize = 0;
		for (int i = 0; i < contents.length - 1; i++)
		{
			this.totalContentSize += contents[i].size;
		}
	}

	public long getTotalContentSize()
	{
		return totalContentSize;
	}

	public void downloadContents() throws IOException, ExitException
	{
		String tmpPath = getContentPath();
		File f = new File(tmpPath);
		if (!f.exists())
		{
			Files.createDirectory(f.toPath());
		}

		for (Content c : contents)
		{
			if (c != contents[0])
			{
				f = new File(tmpPath + "/" + String.format("%08X", c.ID) + ".app");
				if (f.exists())
				{
					if (f.length() == c.size)
					{
						System.out.println("Skipping Content: " + String.format("%08X", c.ID));
					} else
					{
						if (Settings.downloadWhenCachedFilesMissingOrBroken)
						{
							System.out.println("Content " + String.format("%08X", c.ID) + " is broken. Downloading it again.");
							Downloader.getInstance().downloadContent(titleID, c.ID, tmpPath);
						} else
						{
							if (Settings.skipBrokenFiles)
							{
								System.out.println("Content " + String.format("%08X", c.ID) + " is broken. Ignoring it.");
							} else
							{
								System.out.println("Content " + String.format("%08X", c.ID) + " is broken. Downloading not allowed.");
								throw new ExitException("Content missing.");
							}
						}
					}
				} else
				{
					System.out.println("Download Content: " + String.format("%08X", c.ID));
					Downloader.getInstance().downloadContent(titleID, c.ID, tmpPath);
				}
			}
		}

	}

	public String getContentPath()
	{
		return nus.getContentPath();
	}

	public NUSTitle getNUSTitle()
	{
		return nus;
	}

	public void setNUSTitle(NUSTitle nus)
	{
		this.nus = nus;
	}
}