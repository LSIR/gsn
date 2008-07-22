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
public class SimpleDozerSFListener implements MessageListener
{	
	public SimpleDozerSFListener()
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
		
		mif.registerListener(new DozerBaseStatusMsg(), this);
        mif.registerListener(new DozerHealthMsg(), this);        
        mif.registerListener(new DozerAdcMux1Msg(), this);
        mif.registerListener(new DozerAdcMux2Msg(), this);
        mif.registerListener(new DozerAdcComDiffMsg(), this);
        mif.registerListener(new DozerDigitalDCXMsg(), this);
        mif.start();
        
        System.out.println("message handler started...");
        System.out.println();
	}

	public void messageReceived(int dest_addr, Message msg)
	{
		System.out.print(MsgFormatter.DF.format(System.currentTimeMillis()) + " - ");
		if (msg instanceof DozerBaseStatusMsg)
		{
			if (msg.dataLength() != DozerBaseStatusMsg.DEFAULT_MESSAGE_SIZE)
			{
				System.out.print("received " + "base status" + " packet with invalid length (" +
					msg.dataLength() + "-" + DozerBaseStatusMsg.DEFAULT_MESSAGE_SIZE +"): ");
				Dump.printPacket(System.out, msg.dataGet());
				System.out.println();
				return;
			}
			System.out.print("base status");
		}
		else if (msg instanceof DozerHealthMsg)
		{
			if (msg.dataLength() != DozerHealthMsg.DEFAULT_MESSAGE_SIZE)
			{
				System.out.print("received " + "data" + " packet with invalid length (" + 
						msg.dataLength() + "-" + DozerHealthMsg.DEFAULT_MESSAGE_SIZE +"): ");
				Dump.printPacket(System.out, msg.dataGet());
				System.out.println();
				return;
			}
			System.out.print("data");
		}
		else if (msg instanceof DozerAdcMux1Msg)
		{
			if (msg.dataLength() != DozerAdcMux1Msg.DEFAULT_MESSAGE_SIZE)
			{
				System.out.print("received " + "AdcMux1" + " packet with invalid length (" + 
						msg.dataLength() + "-" + DozerAdcMux1Msg.DEFAULT_MESSAGE_SIZE +"): ");
				Dump.printPacket(System.out, msg.dataGet());
				System.out.println();
				return;
			}
			System.out.print("AdcMux1");
		}
		else if (msg instanceof DozerAdcMux2Msg)
		{
			if (msg.dataLength() != DozerAdcMux2Msg.DEFAULT_MESSAGE_SIZE)
			{
				System.out.print("received " + "AdcMux2" + " packet with invalid length (" + 
						msg.dataLength() + "-" + DozerAdcMux2Msg.DEFAULT_MESSAGE_SIZE +"): ");
				Dump.printPacket(System.out, msg.dataGet());
				System.out.println();
				return;
			}
			System.out.print("AdcMux2");
		}
		else if (msg instanceof DozerAdcComDiffMsg)
		{
			if (msg.dataLength() != DozerAdcComDiffMsg.DEFAULT_MESSAGE_SIZE)
			{
				System.out.print("received " + "AdcComDiff" + " packet with invalid length (" + 
						msg.dataLength() + "-" + DozerAdcComDiffMsg.DEFAULT_MESSAGE_SIZE +"): ");
				Dump.printPacket(System.out, msg.dataGet());
				System.out.println();
				return;
			}
			System.out.print("AdcComDiff");
		}
		else if (msg instanceof DozerDigitalDCXMsg)
		{
			if (msg.dataLength() != DozerDigitalDCXMsg.DEFAULT_MESSAGE_SIZE)
			{
				System.out.print("received " + "DigitalDCX" + " packet with invalid length (" + 
						msg.dataLength() + "-" + DozerDigitalDCXMsg.DEFAULT_MESSAGE_SIZE +"): ");
				Dump.printPacket(System.out, msg.dataGet());
				System.out.println();
				return;
			}
			System.out.print("DigitalDCX");
		}
		else
		{
			System.out.print("received " + "unknown" + " packet with unknown length (" + msg.dataLength() + "): ");
			Dump.printPacket(System.out, msg.dataGet());
			System.out.println();
			return;
		}
		System.out.print(" payload (0x" + String.format("%02x", msg.amType()) + "): ");
		Dump.printPacket(System.out, msg.dataGet());
		System.out.println();
		System.out.print(msg);
	}

	public static void main(String[] args)
	{
		if (args.length > 0) {
			System.err.println("usage: java ch.ethz.permafrozer.SimpleDozerSFListener");
			System.exit(1);
		}
		
		new SimpleDozerSFListener();
	}
}
