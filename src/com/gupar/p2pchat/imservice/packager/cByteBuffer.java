package com.gupar.p2pchat.imservice.packager;

public class cByteBuffer
{	
	protected byte[] m_Buffer = null;
	protected int m_BufferSize;  // Total size of the ringbuffer
	
	protected int m_DataStart;  // Where the data starts in the ringbuffer
	protected int m_WritePos;   // Where the data ends in the ringbuffer
	protected int m_ReadPos;    // Where the next read will start in the ringbuffer
	
	public cByteBuffer(int a_BufferSize){
		m_BufferSize = a_BufferSize + 1;
		ClearAll();
	}
	
	public void ClearAll(){
		m_Buffer = new byte[m_BufferSize];		
		m_DataStart = 0;
		m_WritePos = 0;
		m_ReadPos = 0;
	}
	
	/** Writes the bytes specified to the ringbuffer. Returns true if successful, false if not */
	public boolean Write(byte[] a_Bytes,int a_Count){
		CheckValid();

		// Store the current free space for a check after writing:
		int CurFreeSpace = GetFreeSpace();
		int CurReadableSpace = GetReadableSpace();
		int WrittenBytes = 0;
		
		if (CurFreeSpace < a_Count)
		{
			return false;
		}
		assert(m_BufferSize >= m_WritePos);
		int TillEnd = m_BufferSize - m_WritePos;
		int readpos = 0;
		if (TillEnd <= a_Count)
		{
			// Need to wrap around the ringbuffer end
			if (TillEnd > 0)
			{
				System.arraycopy(a_Bytes, readpos, m_Buffer, m_WritePos, TillEnd);

				readpos += TillEnd;
				a_Count -= TillEnd;
				WrittenBytes = TillEnd;
			}
			m_WritePos = 0;
		}
		
		// We're guaranteed that we'll fit in a single write op
		if (a_Count > 0)
		{
			System.arraycopy(a_Bytes, readpos, m_Buffer, m_WritePos, a_Count);
			
			m_WritePos += a_Count;
			WrittenBytes += a_Count;
		}
		
		assert(GetFreeSpace() == CurFreeSpace - WrittenBytes);
		assert(GetReadableSpace() == CurReadableSpace + WrittenBytes);
		return true;
	}
	
	/** Returns the number of bytes that can be successfully written to the ringbuffer */
	public int GetFreeSpace(){
		CheckValid();
		if (m_WritePos >= m_DataStart)
		{
			// Wrap around the buffer end:
			assert(m_BufferSize >= m_WritePos);
			assert((m_BufferSize - m_WritePos + m_DataStart) >= 1);
			return m_BufferSize - m_WritePos + m_DataStart - 1;
		}
		// Single free space partition:
		assert(m_BufferSize >= m_WritePos);
		assert(m_BufferSize - m_WritePos >= 1);
		return m_DataStart - m_WritePos - 1;
	}
	
	/** Returns the number of bytes that are currently in the ringbuffer. Note GetReadableBytes() */
	public int GetUsedSpace(){
		CheckValid();
		assert(m_BufferSize >= GetFreeSpace());
		assert((m_BufferSize - GetFreeSpace()) >= 1);
		return m_BufferSize - GetFreeSpace() - 1;
	}
	
	/** Returns the number of bytes that are currently available for reading (may be less than UsedSpace due to some data having been read already) */
	public int GetReadableSpace(){
		CheckValid();
		if (m_ReadPos > m_WritePos)
		{
			// Wrap around the buffer end:
			assert(m_BufferSize >= m_ReadPos);
			return m_BufferSize - m_ReadPos + m_WritePos;
		}
		// Single readable space partition:
		assert(m_WritePos >= m_ReadPos);
		return m_WritePos - m_ReadPos;
	}
	
	/** Returns the current data start index. For debugging purposes. */
	public int  GetDataStart() { return m_DataStart; }
	
	/** Returns true if the specified amount of bytes are available for reading */
	public boolean CanReadBytes(int a_Count){
		CheckValid();
		return (a_Count <= GetReadableSpace());
	}

	/** Returns true if the specified amount of bytes are available for writing */
	public boolean CanWriteBytes(int a_Count){
		CheckValid();
		return (a_Count <= GetFreeSpace());
	}
	
	/** Reads a_Count bytes into a_Buffer; returns true if successful */
	public boolean ReadBuf(byte []a_Buffer, int a_Count){
//		CheckValid();
//		if(CanReadBytes(a_Count))
//			return false;
		
//		assert(m_BufferSize >= m_ReadPos);
		int BytesToEndOfBuffer = m_BufferSize - m_ReadPos;
		int hasread = 0;
		if (BytesToEndOfBuffer <= a_Count)
		{
			// Reading across the ringbuffer end, read the first part and adjust parameters:
			if (BytesToEndOfBuffer > 0)
			{
				System.arraycopy(m_Buffer, m_ReadPos, a_Buffer, hasread, BytesToEndOfBuffer);
				a_Count -= BytesToEndOfBuffer;
				hasread += BytesToEndOfBuffer;
			}
			m_ReadPos = 0;
		}
		
		// Read the rest of the bytes in a single read (guaranteed to fit):
		if (a_Count > 0)
		{
			System.arraycopy(m_Buffer, m_ReadPos, a_Buffer, hasread, a_Count);
			m_ReadPos += a_Count;
		}
		return true;
	}
	
	public int ReadVarInt()
	{
		CheckValid();
	
		int Value = 0;
        int j = 0;
        byte b0;

        do
        {
            b0 = this.ReadBEInt8();
            Value |= (b0 & 127) << j++ * 7;

            if (j > 5)
            {
                throw new RuntimeException("VarInt too big");
            }
        }
        while ((b0 & 128) == 128);

        return Value;		
	}
	
	 public void writeVarInt(int value)
	    {
	        while ((value & -128) != 0)
	        {
	            this.WriteBEInt8((byte)(value & 127 | 128));
	            value >>>= 7;
	        }

	        this.WriteBEInt8((byte)(value));
	    }
	
	public byte ReadBEInt8(){
		byte []buffer = new byte[1];
		if(ReadBuf(buffer,1)){
			return 	buffer[0];		
		}
		else 
			return (byte)0;
	}
	
	public void WriteBEInt8(byte value){
		byte []buffer = new byte[1];
		buffer[0] = value;
		if(WriteBuf(buffer,1)){
				
		}
	}
	
	public short ReadBEInt16(){
		byte []buffer = new byte[2];
		if(ReadBuf(buffer,2)){
			return 	bytes2Short(buffer);		
		}
		else 
			return 0;
	}
	
	public int ReadBEInt32(){
		byte []buffer = new byte[4];
		if(ReadBuf(buffer,4)){
			return 	bytes2Int(buffer);		
		}
		else 
			return 0;
	}
	
	public boolean ReadBool(){
		return ReadBEInt8() != 0;
	}
	
	public String ReadString(int a_Count)
	{
		byte []buffer = new byte[a_Count];
		if(ReadBuf(buffer,a_Count)){
			return new String(buffer);
		}
		return new String();
	}
	
	public String ReadVarUTF8String()
	{
		int size = ReadVarInt();
		if(size == 0){
			return new String();
		}
		return ReadString(size);
	}
	
	public static long getLong(byte[] bytes)
    {
        return(0xffL & (long)bytes[7]) | (0xff00L & ((long)bytes[6] << 8)) | (0xff0000L & ((long)bytes[5] << 16)) | (0xff000000L & ((long)bytes[4] << 24))
         | (0xff00000000L & ((long)bytes[3] << 32)) | (0xff0000000000L & ((long)bytes[2] << 40)) | (0xff000000000000L & ((long)bytes[1] << 48)) | (0xff00000000000000L & ((long)bytes[0] << 56));
    }
	
    public double ReadDouble()
    {
    	byte [] buf = new byte[8];
    	if(ReadBuf(buf,8))
    	{
    		long lonV = getLong(buf);
    		return Double.longBitsToDouble(lonV);
    	}
		return 0;
    }
	
	
	/** Writes a_Count bytes into a_Buffer; returns true if successful */
	public boolean WriteBuf(byte[] a_Buffer, int a_Count){
		CheckValid();
		if(!CanWriteBytes(a_Count))
			return false;
		
		assert(m_BufferSize >= m_ReadPos);
		int  BytesToEndOfBuffer = m_BufferSize - m_WritePos;
		int readpos = 0;
		if (BytesToEndOfBuffer <= a_Count)
		{
			// Reading across the ringbuffer end, read the first part and adjust parameters:
			System.arraycopy(a_Buffer, readpos, m_Buffer, m_WritePos, BytesToEndOfBuffer);			
			readpos += BytesToEndOfBuffer;
			a_Count -= BytesToEndOfBuffer;
			m_WritePos = 0;
		}
		
		// Read the rest of the bytes in a single read (guaranteed to fit):
		if (a_Count > 0)
		{
			System.arraycopy(a_Buffer, readpos, m_Buffer, m_WritePos, a_Count);	
			m_WritePos += a_Count;
		}
		return true;
	}

	
	/** Removes the bytes that have been read from the ringbuffer */
	public void CommitRead(){
		CheckValid();
		m_DataStart = m_ReadPos;
	}
	
	/** Restarts next reading operation at the start of the ringbuffer */
	public void ResetRead(){
		CheckValid();
		m_ReadPos = m_DataStart;
	}
	
	
	/** Checks if the internal state is valid (read and write positions in the correct bounds) using ASSERTs */
	void CheckValid(){
		assert(m_ReadPos < m_BufferSize);
		assert(m_WritePos < m_BufferSize);
	}
	
	public static short bytes2Short(byte[] b) {
	      return (short) (((b[0] << 8) | b[1] & 0xff));
	}
	
	public static byte [] short2Bytes(short s) { 
		byte []b = new byte[2];
	     b[1] = (byte) (s >> 8); 
	     b[0] = (byte) (s >> 0); 
	     
	     return b;
	} 
	
	public static byte[] int2Bytes(int num) {  
        byte[] byteNum = new byte[4];  
        for (int ix = 0; ix < 4; ++ix) {  
            int offset = 32 - (ix + 1) * 8;  
            byteNum[ix] = (byte) ((num >> offset) & 0xff);  
        }  
        return byteNum;  
    }  
  
    public static int bytes2Int(byte[] byteNum) {  
        int num = 0;  
        for (int ix = 0; ix < 4; ++ix) {  
            num <<= 8;  
            num |= (byteNum[ix] & 0xff);  
        }  
        return num;  
    }  
} 
