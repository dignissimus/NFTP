package me.ezeh.nftp.protocol

import java.io.IOException
import java.net.Socket
import java.nio.charset.StandardCharsets

// A new way to deliver files over the internet

class NftpClient(val socket: Socket) : PacketSender() {
    @Throws(IOException::class)
    constructor(ip: String, port: Int) : this(Socket(ip, port))

    fun sayHello(): Boolean {
        return writeByte(Bytes.HANDSHAKE) && readByte() == Bytes.REPLY
    }

    fun respondHello(): Boolean {
        return writeByte(Bytes.REPLY)
    }

    fun readByte(): Int {
        try {
            val r = socket.getInputStream().read().toByte()
            var i = r.toInt()
            if (r < 0) {
                //System.out.println("SMALL: "+r);
                //System.out.println("i: "+(int)r);
                i += 256
                //System.out.println("new: "+r);
                //System.out.println("i: "+i);
            }
            //System.out.println(hashCode() + ": read byte: " + i);
            return i
        } catch (e: IOException) {
            e.printStackTrace()
            System.exit(0)
            return -1
        }

    }

    fun writeByte(b: Int): Boolean {
        //System.out.println(hashCode() + ": wrote byte: " + b);
        return try {
            socket.getOutputStream().write(b)
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }

    }

    fun writeBytes(vararg bytes: Int): Boolean {
        var work = true
        for (b in bytes) {
            work = work && writeByte(b)
        }
        return work
    }

    fun requestFile(name: String): Boolean {

        return writeBytes(*concatIntArrays(intArrayOf(Bytes.REQUEST), createStringPacket(name))) && readByte() == Bytes.OK
    }

    fun downloadFile(name: String): String? {
        val fsb = StringBuilder()
        var more = true
        if (requestFile(name)) {
            //System.out.println("READY TO DOWNLOAD");
            writeBytes(*concatIntArrays(intArrayOf(Bytes.DOWNLOAD), createStringPacket(name)))
        } else {
            return null
        }
        if (readByte() == Bytes.FILE)
            while (more) {
                more = false
                fsb.append(readString())
                val b = readByte()
                if (b == Bytes.CONTINUE) {
                    more = true
                } else if (b == Bytes.END)
                    break
                else if (b == Bytes.TERMINATE) System.exit(0)

            }
        return fsb.toString()
    }

    fun readString(): String {
        val type = readByte()
        val length: Int
        if (type == Bytes.LENGTH)
            length = readByte()
        else
            throw IllegalStateException("Attempted to read LENGTH byte found value '$type' instead")

        return readString(length)
    }

    private fun readString(length: Int): String {
        //System.out.println("l: " + length);
        val sb = StringBuilder()
        for (i in 0 until length) {
            sb.append(String(byteArrayOf(readByte().toByte()), StandardCharsets.US_ASCII))
        }
        return sb.toString()
    }
}

