package a2.hash;

import a2.util.ByteConverter;

public final class CRC16 implements Hash
{
    private SunCRC16 crc16 = new SunCRC16();

    @Override
    public void update(int b)
    {
        for (byte i : ByteConverter.convert(b))
            crc16.update(i);
    }

    @Override
    public void update(byte[] b, int off, int len)
    {
        for (byte i : b)
            crc16.update(i);
    }

    @Override
    public byte[] getValue()
    {
        byte[] hashed = ByteConverter.convert((short) crc16.value);
        reset();
        return hashed;
    }

    @Override
    public void reset()
    {
        crc16.reset();
    }

    @Override
    public int size()
    {
        return 2;
    }

    /*
    * Copyright (c) 1994, 1995, Oracle and/or its affiliates. All rights reserved.
    * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
    *
    * This code is free software; you can redistribute it and/or modify it
    * under the terms of the GNU General Public License version 2 only, as
    * published by the Free Software Foundation.  Oracle designates this
    * particular file as subject to the "Classpath" exception as provided
    * by Oracle in the LICENSE file that accompanied this code.
    *
    * This code is distributed in the hope that it will be useful, but WITHOUT
    * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
    * version 2 for more details (a copy is included in the LICENSE file that
    * accompanied this code).
    *
    * You should have received a copy of the GNU General Public License version
    * 2 along with this work; if not, write to the Free Software Foundation,
    * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
    *
    * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
    * or visit www.oracle.com if you need additional information or have any
    * questions.
    */
    private class SunCRC16
    {
        /**
         * The CRC-16 class calculates a 16 bit cyclic redundancy check of a set
         * of bytes. This error detecting code is used to determine if bit rot
         * has occured in a byte stream.
         */

        /**
         * value contains the currently computed CRC, set it to 0 initally
         */
        int value;

        SunCRC16()
        {
            value = 0;
        }

        /**
         * update CRC with byte b
         */
        void update(byte aByte)
        {
            int a, b;

            a = (int) aByte;
            for (int count = 7; count >= 0; count--) {
                a = a << 1;
                b = (a >>> 8) & 1;
                if ((value & 0x8000) != 0) {
                    value = ((value << 1) + b) ^ 0x1021;
                } else {
                    value = (value << 1) + b;
                }
            }
            value = value & 0xffff;
        }

        /**
         * reset CRC value to 0
         */
        void reset()
        {
            value = 0;
        }
    }
}


