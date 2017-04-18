package me.ezeh.nftp.protocol;

public class Bytes {
    public static final int HANDSHAKE = 0x0;
    public static final int REPLY = 0x1;
    public static final int LENGTH = 0x2;
    public static final int REQUEST = 0x3;
    public static final int TEST = 0xff;
    public static final int OK = 0xc8;//200
    public static final int MISSING = 0x4;
    public static final int TERMINATE = 0x5;
    public static final int CONTINUE = 0xc;
    public static final int DOWNLOAD = 0xd;
    public static final int FILE = 0xf;
    public static final int END = 0x0;
}
