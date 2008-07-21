/*
 * Created on June 5, 2008
 *
 * Copyright (c) @year@ by ETH Zurich
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY ETH ZURICH AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL ETH ZURICH
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 */

package ch.ethz.permafrozer;

import java.io.FileWriter;
import java.io.IOException;

import net.tinyos1x.message.Dump;
import net.tinyos1x.message.Message;
import net.tinyos1x.message.MessageListener;
import net.tinyos1x.message.MoteIF;
import net.tinyos1x.packet.BuildSource;
import net.tinyos1x.packet.PhoenixSource;
import net.tinyos1x.util.PrintStreamMessenger;

/**
 * 
 * 
 * @author <a href=mailto:yuecel@tik.ee.ethz.ch>Mustafa Yuecel</a>
 */
public class SimpleDozerSFFileWriter implements MessageListener
{
	final static String FILENAME = "dozer.log";
	
	FileWriter writer;
	
	public SimpleDozerSFFileWriter()
	{
		System.out.print("establish connection: ");
		
        MoteIF mif = null;
		PhoenixSource source = BuildSource.makePhoenix(BuildSource.makePacketSource(System.getenv("MOTECOM")), 
				PrintStreamMessenger.err);
		if (source != null) {
			source.setResurrection();
			mif = new MoteIF(source);
		}
		if (mif == null) {
			System.err.println("Invalid packet source (check your MOTECOM environment variable)");
			System.exit(2);
		}

		System.out.println("connected.");
		
		System.out.print("open log file: ");

		try
		{
			writer = new FileWriter(FILENAME, true);
		}
		catch (IOException e)
		{
			System.err.println("Cannot open file: " + FILENAME);
			System.exit(3);
		}
		
		System.out.println("opened.");
		
        mif.registerListener(new DozerBaseStatusMsg(), this);
		mif.registerListener(new DozerDataMsg(), this);
        mif.registerListener(new DozerAdcMux1Msg(), this);
        mif.registerListener(new DozerAdcMux2Msg(), this);
        mif.registerListener(new DozerAdcComDiffMsg(), this);
        mif.start();
        
        System.out.println("message handler started...");
        System.out.println();
	}

	public void messageReceived(int dest_addr, Message msg)
	{
		String s;
		
		if (msg instanceof DozerBaseStatusMsg)
		{
			s = "base status";
			if (msg.dataLength() != DozerBaseStatusMsg.DEFAULT_MESSAGE_SIZE)
			{
				System.out.print("received " + s + " packet with invalid length: ");
				Dump.printPacket(System.out, msg.dataGet());
				return;
			}
		}
		else if (msg instanceof DozerDataMsg)
		{
			s = "data";
			if (msg.dataLength() != DozerDataMsg.DEFAULT_MESSAGE_SIZE)
			{
				System.out.print("received " + s + " packet with invalid length: ");
				Dump.printPacket(System.out, msg.dataGet());
				return;
			}
		} 
		else if (msg instanceof DozerAdcMux1Msg)
		{
			s = "AdcMux1";
			if (msg.dataLength() != DozerAdcMux1Msg.DEFAULT_MESSAGE_SIZE)
			{
				System.out.print("received " + s + " packet with invalid length: ");
				Dump.printPacket(System.out, msg.dataGet());
				return;
			}
		}
		else if (msg instanceof DozerAdcMux2Msg)
		{
			s = "AdcMux2";
			if (msg.dataLength() != DozerAdcMux2Msg.DEFAULT_MESSAGE_SIZE)
			{
				System.out.print("received " + s + " packet with invalid length: ");
				Dump.printPacket(System.out, msg.dataGet());
				return;
			}
		}
		else if (msg instanceof DozerAdcComDiffMsg)
		{
			s = "AdcComDiff";
			if (msg.dataLength() != DozerAdcComDiffMsg.DEFAULT_MESSAGE_SIZE)
			{
				System.out.print("received " + s + " packet with invalid length: ");
				Dump.printPacket(System.out, msg.dataGet());
				return;
			}
		}
		else
		{
			s = "unknown";
		}
		
		byte[] bytes = msg.dataGet();
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < bytes.length; i++)
		{
			buf.append(String.format(" %02x", bytes[i]));
		}
		s += " payload 0x" + String.format("%02x", msg.amType()) + " (" + MsgFormatter.DF.format(System.currentTimeMillis()) + "):" + buf.toString() + "\n" + msg;

		System.out.print(s);

		try
		{
			writer.append(s);
			writer.flush();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public static void main(String[] args)
	{
		if (args.length > 0) {
			System.err.println("usage: java ch.ethz.permafrozer.SimpleDozerSFListener");
			System.exit(1);
		}
		
		new SimpleDozerSFFileWriter();
	}
}
