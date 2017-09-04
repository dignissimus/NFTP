package me.ezeh.nftp.protocol

import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import kotlin.system.exitProcess

object Main {
    private val port = 8080
    @JvmStatic
    fun main(args: Array<String>) {
        try {
            ServerSocket(port).use { testServer ->
                val socketTestResult = testSocket(testServer)
                val protocolTestResult = testProtocol(testServer)
                val fileRequestTestResult = testFileRequest(testServer)
                println("Network Test: " + if (socketTestResult) "Successful" else "Unsuccessful")
                println("Protocol Test: " + if (protocolTestResult) "Successful" else "Unsuccessful")
                println("File Request Test: " + if (fileRequestTestResult) "Successful" else "Unsuccessful")
                if (!(socketTestResult && protocolTestResult && fileRequestTestResult)) {
                    println("Not all tests were successful. Stopping...")
                    exitProcess(0)
                }
                println("Starting Server...")
                val main = NftpServer(testServer)
                main.start()
            }
        } catch (e: IOException) {
            println("Unable to start test server")
            e.printStackTrace()
        }

    }

    private fun testSocket(server: ServerSocket): Boolean {
        return try {
            val client = Socket("0.0.0.0", port)
            val socket = server.accept()
            socket.getOutputStream().write(Bytes.TEST)
            socket.close()
            val received = client.getInputStream().read()
            received == Bytes.TEST
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }

    }

    private fun testProtocol(server: ServerSocket): Boolean {
        return try {
            val client = NftpClient("0.0.0.0", port)
            val conn = NftpClient(server.accept())
            Thread { println("Client Simulation: " + if (client.sayHello()) "Successful" else "Unsuccessful") }.start()
            conn.respondHello()
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }

    }

    private fun testFileRequest(server: ServerSocket): Boolean {
        return try {
            val client = NftpClient("0.0.0.0", port)
            val conn = NftpClient(server.accept())
            Thread { client.requestFile("test") }.start()
            val type = conn.readByte()
            if (type != Bytes.REQUEST) false else conn.readString() == "test"
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }

    }


}
