package com.itmecasey.barelyquesting.core.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** The wire format: a message is one byte id followed by fields. Strings are UTF-8 with an int length (no 64 KiB limit). */
public final class Wire {
    private Wire() {
    }

    /** Builds a message. */
    public static final class Out {
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private final DataOutputStream out = new DataOutputStream(bytes);

        public Out(int id) {
            try {
                out.writeByte(id);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        public Out str(String s) {
            try {
                byte[] b = s.getBytes(StandardCharsets.UTF_8);
                out.writeInt(b.length);
                out.write(b);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            return this;
        }

        public Out bool(boolean v) {
            try {
                out.writeBoolean(v);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            return this;
        }

        public Out i(int v) {
            try {
                out.writeInt(v);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            return this;
        }

        public Out blob(byte[] b) {
            try {
                out.writeInt(b.length);
                out.write(b);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            return this;
        }

        public byte[] done() {
            return bytes.toByteArray();
        }
    }

    /** Reads a message. Throws {@link IllegalArgumentException} on malformed data, so a hostile client cannot crash the server. */
    public static final class In {
        private final DataInputStream in;
        private final int id;
        private final int size;

        public In(byte[] message) {
            this.size = message.length;
            this.in = new DataInputStream(new ByteArrayInputStream(message));
            try {
                this.id = in.readUnsignedByte();
            } catch (IOException e) {
                throw new IllegalArgumentException("empty message");
            }
        }

        public int id() {
            return id;
        }

        public String str() {
            try {
                int n = in.readInt();
                if (n < 0 || n > size) {
                    throw new IllegalArgumentException("bad string length");
                }
                byte[] b = new byte[n];
                in.readFully(b);
                return new String(b, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }

        public boolean bool() {
            try {
                return in.readBoolean();
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }

        public int i() {
            try {
                return in.readInt();
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }

        public byte[] blob() {
            try {
                int n = in.readInt();
                if (n < 0 || n > size) {
                    throw new IllegalArgumentException("bad blob length");
                }
                byte[] b = new byte[n];
                in.readFully(b);
                return b;
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    public static byte[] gzip(String text) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (GZIPOutputStream gz = new GZIPOutputStream(bytes)) {
                gz.write(text.getBytes(StandardCharsets.UTF_8));
            }
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String gunzip(byte[] data) {
        try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(data))) {
            return new String(gz.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
