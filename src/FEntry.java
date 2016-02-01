
public class FEntry {
	
	public static int DIR_FLAG = 1;
	public static int NOT_IN_NUSTITLE_FLAG = 0x80;
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
	private int NUScontentID = 0;

	
	public FEntry(String path, String filename, int contentID,int NUScontentID, long fileOffset, long fileLength, boolean dir,
			boolean in_nus_title, boolean extract_withHash) {
		setPath(path);
		setFileName(filename);
		setContentID(contentID);
		setFileOffset(fileOffset);
		setFileLength(fileLength);
		setDir(dir);
		setInNusTitle(in_nus_title);
		setExtractWithHash(extract_withHash);
		setNUScontentID(NUScontentID);
		
	}

	public boolean isDir() {
		return dir;
	}

	private void setDir(boolean dir) {
		this.dir = dir;
	}

	public boolean isInNUSTitle() {
		return in_nus_title;
	}

	private void setInNusTitle(boolean in_nus_title) {
		this.in_nus_title = in_nus_title;
	}

	public boolean isExtractWithHash() {
		return extract_withHash;
	}

	private void setExtractWithHash(boolean extract_withHash) {
		this.extract_withHash = extract_withHash;
	}

	public String getFileName() {
		return fileName;
	}

	private void setFileName(String filename) {
		this.fileName = filename;
	}

	public String getPath() {
		return path;
	}
	
	public String getFullPath() {
		return path + fileName;
	}

	private void setPath(String path) {
		this.path = path;
	}

	public long getFileOffset() {
		return fileOffset;
	}

	private void setFileOffset(long fileOffset) {
		this.fileOffset = fileOffset;
	}

	public int getContentID() {
		return contentID;
	}

	private void setContentID(int contentID) {
		this.contentID = contentID;
	}

	public long getFileLength() {
		return fileLength;
	}

	private void setFileLength(long fileLength) {
		this.fileLength = fileLength;
	}
	
	@Override
	public String toString(){		
		return getFullPath() + " Content ID:" + contentID + " Size: " + fileLength +"MB  Offset: " + fileOffset; 
	}

	public int getNUScontentID() {
		return NUScontentID;
	}

	private void setNUScontentID(int nUScontentID) {
		NUScontentID = nUScontentID;
	}

	public void download() {
		Downloader.getInstance().download(this);
		
	}
	
	
}
