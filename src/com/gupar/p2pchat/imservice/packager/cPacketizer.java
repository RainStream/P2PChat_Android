package com.gupar.p2pchat.imservice.packager;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class cPacketizer {
	private int m_PacketType;
	private ByteBuffer m_Buffer;

	public cPacketizer(int a_PacketType) {
		m_Buffer = ByteBuffer.allocate(1024*4);
		m_PacketType = a_PacketType;
		WriteVarInt32(a_PacketType);
	}

	public byte[] getData() {
		ByteBuffer Buffer = ByteBuffer.allocate(1024*4);
		WriteVarInt32(Buffer, m_Buffer.position());

		Buffer.put(m_Buffer.array(), 0, m_Buffer.position());
		Buffer.flip();
		
		byte[] buf = new byte[Buffer.limit()];
		Buffer.get(buf);
		
		return buf;
	}

	public void WriteBool(boolean a_Value) {
		m_Buffer.put(a_Value == true ? (byte) 0x01 : (byte) 0x00);
	}

	public void WriteBEUInt8(byte a_Value) {
		m_Buffer.put(a_Value);
	}

	public void WriteBEInt8(byte a_Value) {
		m_Buffer.put(a_Value);
	}

	public void WriteBEInt16(short a_Value) {
		m_Buffer.putShort(a_Value);
	}

	public void WriteBEUInt16(short a_Value) {
		m_Buffer.putShort(a_Value);
	}

	public void WriteBEInt32(int a_Value) {
		m_Buffer.putInt(a_Value);
	}

	public void WriteBEUInt32(int a_Value) {
		m_Buffer.putInt(a_Value);
	}

	public void WriteBEInt64(long a_Value) {
		m_Buffer.putLong(a_Value);
	}

	public void WriteBEUInt64(long a_Value) {
		m_Buffer.putLong(a_Value);
	}

	public void WriteBEFloat(float a_Value) {
		m_Buffer.putFloat(a_Value);
	}

	public void WriteBEDouble(double a_Value) {
		m_Buffer.putDouble(a_Value);
	}

	public void WriteBuf(byte[] a_Data, int a_Size) {
		m_Buffer.put(a_Data, 0, a_Size);
	}

	public void WriteString(String a_Value) {
		byte[] strig = null;
		try {
			strig = a_Value.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		WriteVarInt32(strig.length);
		WriteBuf(strig, strig.length);
	}

	public void WriteVarInt32(int a_Value) {
		WriteVarInt32(m_Buffer, a_Value);
	}

	public void WriteVarInt32(ByteBuffer buffer, int a_Value) {
		// A 32-bit integer can be encoded by at most 5 bytes:
		byte b[] = new byte[5];
		int idx = 0;
		do {
			b[idx] = (byte) ((a_Value & 0x7f) | ((a_Value > 0x7f) ? 0x80 : 0x00));
			a_Value = a_Value >> 7;
			idx++;
		} while (a_Value > 0);

		buffer.put(b, 0, idx);
	}

	public int GetPacketType() {
		return m_PacketType;
	}
}
