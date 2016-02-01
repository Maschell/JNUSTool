
public class Content {
	
	int 	ID;						//	0	 0xB04
	short	index;					//	4    0xB08
	short 	type;					//	6	 0xB0A
	long	size;					//	8	 0xB0C
	byte[]	SHA2 = new byte[32];	//  16    0xB14
	
	
	public Content(int ID, short index, short type, long size, byte[] SHA2) {
		this.ID = ID;
		this.index = index;
		this.type = type;
		this.size = size;
		this.SHA2 = SHA2;
	}
	@Override
	public String toString(){		
		return "ID: " + ID +" index: " + index + " type: " + type + " size: " + size + " SHA2: " + Util.ByteArrayToString(SHA2); 
	}
}
