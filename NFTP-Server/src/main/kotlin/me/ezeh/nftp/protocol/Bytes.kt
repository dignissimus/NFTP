package me.ezeh.nftp.protocol

object Bytes {
    val HANDSHAKE = 0x0
    val REPLY = 0x1
    val LENGTH = 0x2
    val REQUEST = 0x3
    val TEST = 0xff
    val OK = 0xc8//200
    val MISSING = 0x4
    val TERMINATE = 0x5
    val CONTINUE = 0xc
    val DOWNLOAD = 0xd
    val FILE = 0xf
    val END = 0x0
}

