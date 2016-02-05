package de.mas.jnustool.util;

public class ByteArrayBuffer {
	public byte[] buffer;
	int lengthOfDataInBuffer;
	
	public ByteArrayBuffer(int length){
		buffer = new byte[(int) length];
	}

	public int getLengthOfDataInBuffer() {
		return lengthOfDataInBuffer;
	}

	public void setLengthOfDataInBuffer(int lengthOfDataInBuffer) {
		this.lengthOfDataInBuffer = lengthOfDataInBuffer;
	}

	public int getSpaceLeft() {
		return buffer.length - getLengthOfDataInBuffer();
	}

	public void addLengthOfDataInBuffer(int bytesRead) {
		lengthOfDataInBuffer += bytesRead;		
	}

	public void resetLengthOfDataInBuffer() {
		setLengthOfDataInBuffer(0);
	}
	
	

}
