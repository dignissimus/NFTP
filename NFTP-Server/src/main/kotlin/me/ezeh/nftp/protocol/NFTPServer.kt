package me.ezeh.nftp.protocol

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.ServerSocket
import java.net.SocketException

internal class NftpServer(val serverSocket: ServerSocket) : PacketSender() {
    @Throws(IOException::class)
    constructor(port: Int) : this(ServerSocket(port))

    fun trimIntArray(array: ByteArray, len: Int): ByteArray {
        val newArray = ByteArray(len)
        System.arraycopy(array, 0, newArray, 0, len)
        return newArray
    }

    @Throws(IOException::class)
    fun sendFile(client: NftpClient, file: File) {
        //System.out.println("Sending file: " + file.getName());
        val max = 255
        var left = file.length()
        //System.out.println("length"+left);
        var buffer = ByteArray(max)
        /* // non-efficient way of reading a file
        String temp;
        temp = new BufferedReader(new FileReader(file)).readLine();
        while (temp!=null){
            contents.append(temp);
            new BufferedReader(new FileReader(file)).readLine();
        }*/
        val fis = FileInputStream(file)
        var read = fis.read(buffer)
        //System.out.println("read "+read);
        //System.out.println("buffer = " + Arrays.toString(buffer));
        left -= read.toLong()
        if (buffer.size > read) {
            buffer = trimIntArray(buffer, read)
        }
        client.writeBytes(*concatIntArrays(intArrayOf(Bytes.FILE), createStringPacket(String(buffer))))
        while (left != 0L) {
            read = fis.read(buffer)
            //System.out.println("read "+read);
            //System.out.println("buffer = " + Arrays.toString(buffer));
            left -= read.toLong()
            if (buffer.size > read) {
                buffer = trimIntArray(buffer, read)
            }
            client.writeBytes(*concatIntArrays(intArrayOf(Bytes.CONTINUE), createStringPacket(String(buffer))))
        }
        client.writeByte(Bytes.END)
    }

    @Throws(IOException::class)
    private fun handle(client: NftpClient) {
        var saidHello = false
        while (true) {
            val b = client.readByte()
            if (!saidHello) {
                if (b == Bytes.HANDSHAKE) {
                    saidHello = true
                    println("SAID HELLO!")
                    client.respondHello()
                }
            }
            if (b == Bytes.REQUEST) {
                val name = client.readString()
                //System.out.println("File requested: " + name);
                val file = getFile(name)
                //System.out.println(file.getAbsoluteFile() + " : " + (file.exists() && file.canRead()));
                if (file.exists() && file.canRead()) {
                    client.writeByte(Bytes.OK)
                } else {
                    client.writeByte(Bytes.MISSING)
                }
            }
            if (b == Bytes.DOWNLOAD) {
                val name = client.readString()
                //System.out.println("About to send file: " + name);
                val file = getFile(name)
                sendFile(client, file)
            }

        }

    }

    fun start() {
        println("Server Started!")

        //This is a test client
        Thread {
            val client: NftpClient =
                    try {
                        NftpClient("0.0.0.0", 8888)
                    } catch (exception: IOException) {
                        throw exception
                    }

            println("Test Client on!")
            System.out.println("Server Ready: " + client.sayHello())
            println("test.txt available: " + client.requestFile("test.txt"))
            println("\nDownloading Test File...\n======== Test File ========")
            println(client.downloadFile("test.txt"))
            println("======= End Of File =======")
        }.start()

        while (true) {
            try {
                val client = serverSocket.accept()
                println("New connection!")
                val connection = NftpClient(client)
                Thread {
                    try {
                        handle(connection)
                    } catch (e: SocketException) {
                        println("The client connection was closed unexpectedly")
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }.start()

            } catch (e: SocketException) {
                println("The client connection was closed unexpectedly")
                break
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }

    fun getFile(name: String): File {
        return File(System.getProperty("user.dir") + File.separator + name)
    }
}

