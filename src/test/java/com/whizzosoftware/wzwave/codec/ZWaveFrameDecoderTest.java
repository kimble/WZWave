/*******************************************************************************
 * Copyright (c) 2013 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.wzwave.codec;

import com.whizzosoftware.wzwave.commandclass.VersionCommandClass;
import com.whizzosoftware.wzwave.frame.*;
import io.netty.buffer.ByteBuf;
import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static io.netty.buffer.Unpooled.wrappedBuffer;
import static org.junit.Assert.*;

/**
 * Test for the ZWaveFrameDecoder.
 *
 * @author Dan Noguerol
 */
public class ZWaveFrameDecoderTest {

    @Test
    public void testACK() throws Exception {
        Session session = new Session();
        session.decode(0x06);
        session.assertOutput().hasSize(1);

        ACK ack = session.get(0, ACK.class);
        assertNotNull(ack);
    }

    @Test
    public void testNAK() throws Exception {
        Session session = new Session();
        session.decode(0x15);
        session.assertOutput().hasSize(1);

        NAK nak = session.get(0, NAK.class);
        assertNotNull(nak);
    }

    @Test
    public void testCAN() throws Exception {
        Session session = new Session();
        session.decode(0x18);
        session.assertOutput().hasSize(1);

        CAN cancel = session.get(0, CAN.class);
        assertNotNull(cancel);
    }

    @Test
    public void testACKPlusMessage() throws Exception {
        Session session = new Session();
        session.decode(0x06, 0x01, 0x10, 0x01, 0x15, 0x5A, 0x2D, 0x57, 0x61, 0x76, 0x65, 0x20, 0x32, 0x2E, 0x37, 0x38, 0x00, 0x01, 0x9B);
        session.assertOutput().hasSize(2);

        session.get(0, ACK.class);
        Version parsedVersion = session.get(1, Version.class);

        Assertions.assertThat(parsedVersion.getLibraryVersion())
                .as("Parsed library version")
                .startsWith("Z-Wave 2.78");
    }

    @Test
    public void testPartialMessage() throws Exception {
        Session session = new Session();
        ByteBuf in = session.decode(0x06, 0x01, 0x10, 0x01);
        assertEquals(1, in.readerIndex());
        assertEquals(0x01, in.getByte(in.readerIndex()));
        session.assertOutput().hasSize(1);

        session.decode(0x01, 0x10, 0x01, 0x15, 0x5A, 0x2D, 0x57, 0x61, 0x76, 0x65, 0x20, 0x32, 0x2E, 0x37, 0x38, 0x00, 0x01, 0x9B);
        session.assertOutput().hasSize(2);
        session.get(0, ACK.class);
        session.get(1, Version.class);
    }

    @Test
    public void testGetVersionResponse() throws Exception {
        Session session = new Session();
        session.decode(0x01, 0x10, 0x01, 0x15, 0x5a, 0x2d, 0x57, 0x61, 0x76, 0x65, 0x20, 0x32, 0x2e, 0x37, 0x38, 0x00, 0x01, 0x9b);
        session.assertOutput().hasSize(1);

        Version version = session.get(0, Version.class);
        assertEquals("Z-Wave 2.78\u0000", version.getLibraryVersion());
        assertEquals((byte) 0x01, version.getLibraryType());
    }

    @Test
    public void testGetMemoryId() throws Exception {
        Session session = new Session();
        session.decode(0x01, 0x08, 0x01, 0x20, 0x01, 0x6a, 0x2d, 0xec, 0x01, 0x7d);
        session.assertOutput().hasSize(1);

        MemoryGetId mgid = session.get(0, MemoryGetId.class);
        assertEquals(-20, (int) mgid.getHomeId());
        assertEquals((byte) 1, (byte) mgid.getNodeId());

        // TODO
    }

    @Test
    public void testGetSUCNodeId() throws Exception {
        Session session = new Session();
        session.decode(0x01, 0x04, 0x01, 0x56, 0x00, 0xac);
        GetSUCNodeId obj = session.get(0, GetSUCNodeId.class);
        assertEquals(0, obj.getSucNodeId());
    }

    @Test
    public void testGetInitData() throws Exception {
        Session session = new Session();
        session.decode(0x01, 0x25, 0x01, 0x02, 0x05, 0x00, 0x1d, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0x01,  0xc0);
        session.assertOutput().hasSize(1);

        InitData id = session.get(0, InitData.class);
        assertEquals(2, id.getNodes().size());

        // TODO
    }

    @Test
    public void testNodeInformation() throws Exception {
        Session session = new Session();
        session.decode(0x01, 0x09, 0x01, 0x41, 0x92, 0x16, 0x00, 0x02, 0x02, 0x01, 0x33);
        session.assertOutput().hasSize(1);
        NodeProtocolInfo pi = session.get(0, NodeProtocolInfo.class);

        assertTrue(pi.isListening());
        assertTrue(pi.isBeaming());
        assertFalse(pi.isRouting());
        assertEquals(40000, pi.getMaxBaudRate());
        assertEquals(3, pi.getVersion());
        assertFalse(pi.hasSecurity());
        assertEquals(0x02, pi.getBasicDeviceClass());
        assertEquals(0x02, pi.getGenericDeviceClass());
        assertEquals(0x01, pi.getSpecificDeviceClass());
    }

    @Test
    public void testSendData1() throws Exception {
        Session session = new Session();
        session.decode(0x01, 0x04, 0x01, 0x13, 0x01, 0xe8);
        session.assertOutput().hasSize(1);

        SendData sd = session.get(0, SendData.class);
        assertFalse(sd.hasCallbackId());
        assertNull(sd.getCallbackId());
        assertTrue(sd.hasRetVal());
        assertEquals((byte) 0x01, (byte) sd.getRetVal());
    }

    @Test
    public void testRequestNodeInfo() throws Exception {
        Session session = new Session();
        session.decode(0x01, 0x04, 0x01, 0x60, 0x01, 0x9b);
        session.assertOutput().hasSize(1);
        RequestNodeInfo rni = session.get(0, RequestNodeInfo.class);
        assertTrue(rni.wasSuccessfullySent());
    }

    @Test
    public void testApplicationUpdate() throws Exception {
        Session session = new Session();
        session.decode(0x01, 0x06, 0x00, 0x49, 0x81, 0x00, 0x00, 0x31);
        session.assertOutput().hasSize(1);
        ApplicationUpdate au = session.get(0, ApplicationUpdate.class);
        assertNotNull(au);
    }

    @Test
    public void testGetRoutingInfo() throws Exception {
        Session session = new Session();
        session.decode(0x01, 0x20, 0x01, 0x80, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x5c);
        session.assertOutput().hasSize(1);
        GetRoutingInfo gri = session.get(0, GetRoutingInfo.class);

        assertEquals(0x02, gri.getNodeMask()[0]);
        for (int i = 1; i < 29; i++) {
            assertEquals(0x00, gri.getNodeMask()[i]);
        }
    }

    @Test
    public void testSendCommandResponse() throws Exception {
        Session session = new Session();
        session.decode(0x01, 0x04, 0x01, 0x13, 0x01, 0xe8);
        session.assertOutput().hasSize(1);

        SendData sd = session.get(0, SendData.class);
        assertTrue(sd.hasRetVal());
        assertEquals(Byte.valueOf((byte) 0x01), sd.getRetVal());
        assertEquals(DataFrameType.RESPONSE, sd.getType());
    }

    @Test
    public void testSendCommandRequestCallback() throws Exception {
        Session session = new Session();
        session.decode(0x01, 0x05, 0x00, 0x13, 0x02, 0x00, 0xeb);
        session.assertOutput().hasSize(1);

        SendData sd = session.get(0, SendData.class);
        assertTrue(sd.hasCallbackId());
        assertEquals(Byte.valueOf((byte) 0x02), sd.getCallbackId());
        assertEquals(DataFrameType.REQUEST, sd.getType());
    }

    @Test
    public void testExtraneousPrefixBytes() throws Exception {
        Session session = new Session();
        session.decode(0x02, 0x03, 0x04, 0x01, 0x05, 0x00, 0x13, 0x02, 0x00, 0xeb);
        session.assertOutput().hasSize(1);

        SendData sd = session.get(0, SendData.class);
        assertTrue(sd.hasCallbackId());
        assertEquals(Byte.valueOf((byte) 0x02), sd.getCallbackId());
        assertEquals(DataFrameType.REQUEST, sd.getType());
    }

    @Test
    public void testGetNodeVersion() throws Exception {
        Session session = new Session();
        session.decode(0x01, 0x0d, 0x00, 0x04, 0x00, 0x0e, 0x07, 0x86, 0x12, 0x06, 0x03, 0x28, 0x03, 0x19, 0x5c);
        session.assertOutput().hasSize(1);

        ApplicationCommand ach = session.get(0, ApplicationCommand.class);
        assertEquals(0x00, ach.getStatus());
        assertEquals((byte) 14, ach.getNodeId());
        assertEquals(VersionCommandClass.ID, ach.getCommandClassId());
//        Version v = new Version(ach.getCommandClassBytes());
//        assertEquals("6", v.getLibrary());
//        assertEquals("3.25", v.getApplication());
//        assertEquals("3.40", v.getProtocol());
    }

    @Test
    public void testTwoFramesAcrossTwoReads() throws Exception {
        Session session = new Session();
        session.decode(0x01, 0x0d, 0x00, 0x04, 0x00, 0x0e, 0x07, 0x86, 0x12, 0x06, 0x03, 0x28, 0x03, 0x19, 0x5c, 0x01, 0x0d);
        session.decode(0x00, 0x04, 0x00, 0x0e, 0x07, 0x86, 0x12, 0x06, 0x03, 0x28, 0x03, 0x19, 0x5c);
        session.assertOutput().hasSize(2);

        ApplicationCommand ac1 = session.get(0, ApplicationCommand.class);
        ApplicationCommand ac2 = session.get(1, ApplicationCommand.class);

        assertNotNull(ac1);
        assertNotNull(ac2);
    }

    @Test
    public void testRandom2() throws Exception {
        Session session = new Session();
        session.decode(0x01, 0x25, 0x01, 0x02, 0x05, 0x00, 0x1d, 0x01, 0x00, 0x00, 0x00, 0xc0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0x01, 0x02);
    }

    @Test
    public void testFuncCallback() throws Exception {
        Session session = new Session();
        session.decode(0x01, 0x09, 0x00, 0x13, 0x06, 0x02, 0x25, 0x02, 0x05, 0x01, 0xC2);
        session.decode(0x01, 0x04, 0x01, 0x13, 0x01, 0xE8);
        session.decode(0x01, 0x05, 0x00, 0x13, 0x01, 0x00, 0xE8);

        session.assertOutput().hasSize(3);
    }


    private class Session {

        private final ZWaveFrameDecoder decoder = new ZWaveFrameDecoder();
        private final List<Object> out = new ArrayList<Object>();

        private ByteBuf decode(int... ints) throws Exception {
            byte[] bytes = new byte[ints.length];
            for (int i = 0; i < ints.length; i++) {
                bytes[i] = (byte) ints[i];
            }

            ByteBuf buffer = wrappedBuffer(bytes);
            decoder.decode(null, buffer, out);
            return buffer;
        }

        AbstractListAssert<?, ? extends List<?>, Object> assertOutput() {
            return Assertions.assertThat(out).as("Decoded output");
        }

        <T> T get(int index, Class<T> expectedType) {
            Object obj = out.get(index);

            Assertions.assertThat(obj)
                    .as("Object decoded at index %d", index)
                    .isInstanceOf(expectedType);

            return expectedType.cast(obj);
        }

    }

}
