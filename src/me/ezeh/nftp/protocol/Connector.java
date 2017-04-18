package me.ezeh.nftp.protocol;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
// A new way to deliver files over the internet

class PacketSender {
    protected int[] concatIntArrays(int[]... arrays) {
        int[] finished = new int[0];
        for (int[] a : arrays) {
            int[] temp = new int[finished.length + a.length];
            //System.out.println("t: " + temp.length + " f: " + finished.length + " a: " + a.length);
            System.arraycopy(finished, 0, temp, 0, finished.length);
            System.arraycopy(a, 0, temp, finished.length, a.length);
            finished = temp;
        }
        return finished;
    }

    protected int[] createStringPacket(String name) {
        byte[] ba = name.getBytes();
        int[] ia = new int[ba.length];
        for (int i = 0; i < ba.length; i++) {
            ia[i] = ba[i];
        }
        int[] o = new int[]{Bytes.LENGTH, ia.length};
        //System.out.println(Arrays.toString(packet));
        return concatIntArrays(o, ia);
    }
}

public class Connector extends PacketSender {
    Socket socket;

    Connector(Socket socket) {
        this.socket = socket;
    }

    Connector(String ip, int port) throws IOException {
        socket = new Socket(ip, port);
    }

    boolean sayHello() {
        return writeByte(Bytes.HANDSHAKE) && readByte() == Bytes.REPLY;
    }

    boolean respondHello() {
        return writeByte(Bytes.REPLY);
    }

    public int readByte() {
        try {
            byte r = (byte) socket.getInputStream().read();
            int i = r;
            if (r < 0) {
                //System.out.println("SMALL: "+r);
                //System.out.println("i: "+(int)r);
                i += 256;
                //System.out.println("new: "+r);
                //System.out.println("i: "+i);
            }
            //System.out.println(hashCode() + ": read byte: " + i);
            return i;
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
            return -1;
        }
    }

    public boolean writeByte(int b) {
        //System.out.println(hashCode() + ": wrote byte: " + b);
        try {
            socket.getOutputStream().write(b);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean writeBytes(int... bytes) {
        boolean work = true;
        for (int b : bytes) {
            work = work && writeByte(b);
        }
        return work;
    }

    public boolean requestFile(String name) {

        return writeBytes(concatIntArrays(new int[]{Bytes.REQUEST}, createStringPacket(name))) && readByte() == Bytes.OK;
    }

    public String downloadFile(String name) {
        StringBuilder fsb = new StringBuilder();
        boolean more = true;
        if (requestFile(name)) {
            //System.out.println("READY TO DOWNLOAD");
            writeBytes(concatIntArrays(new int[]{Bytes.DOWNLOAD}, createStringPacket(name)));
        } else {
            return null;
        }
        if (readByte() == Bytes.FILE)
            while (more) {
                more = false;
                fsb.append(getString());
                int b = readByte();
                if (b == Bytes.CONTINUE) {
                    more = true;
                } else if (b == Bytes.END) break;
                else if (b == Bytes.TERMINATE) System.exit(0);

            }
        return fsb.toString();
    }

    String getString() {
        int type = readByte();
        int length;
        if (type == Bytes.LENGTH)
            length = readByte();
        else return null;

        return getString(length);
    }

    String getString(int length) {
        //System.out.println("l: " + length);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(new String(new byte[]{(byte) readByte()}, StandardCharsets.US_ASCII));
        }
        return sb.toString();
    }
}

class NFTPServer extends PacketSender {
    ServerSocket server;

    NFTPServer(ServerSocket ss) {
        server = ss;
    }

    NFTPServer(int port) throws IOException {
        server = new ServerSocket(port);
    }

    byte[] trimIntArray(byte[] array, int len) {
        byte[] newArray = new byte[len];
        System.arraycopy(array, 0, newArray, 0, len);
        return newArray;
    }

    public void sendFile(Connector client, File file) throws IOException {
        //System.out.println("Sending file: " + file.getName());
        int max = 255;
        long left = file.length();
        //System.out.println("length"+left);
        byte[] buffer = new byte[max];
        /* // non-efficient way of reading a file
        String temp;
        temp = new BufferedReader(new FileReader(file)).readLine();
        while (temp!=null){
            contents.append(temp);
            new BufferedReader(new FileReader(file)).readLine();
        }*/
        FileInputStream fis = new FileInputStream(file);
        int read = fis.read(buffer);
        //System.out.println("read "+read);
        //System.out.println("buffer = " + Arrays.toString(buffer));
        left -= read;
        if (buffer.length > read) {
            buffer = trimIntArray(buffer, read);
        }
        client.writeBytes(concatIntArrays(new int[]{Bytes.FILE}, createStringPacket(new String(buffer))));
        while (left != 0) {
            read = fis.read(buffer);
            //System.out.println("read "+read);
            //System.out.println("buffer = " + Arrays.toString(buffer));
            left -= read;
            if (buffer.length > read) {
                buffer = trimIntArray(buffer, read);
            }
            client.writeBytes(concatIntArrays(new int[]{Bytes.CONTINUE}, createStringPacket(new String(buffer))));
        }
        client.writeByte(Bytes.END);
    }

    private void handle(Connector client) throws IOException {
        boolean saidHello = false;
        while (true) {
            int b = client.readByte();
            if (!saidHello) {
                if (b == Bytes.HANDSHAKE) {
                    saidHello = true;
                    System.out.println("SAID HELLO!");
                    client.respondHello();
                }
            }
            if (b == Bytes.REQUEST) {
                String name = client.getString();
                //System.out.println("File requested: " + name);
                File file = getFile(name);
                //System.out.println(file.getAbsoluteFile() + " : " + (file.exists() && file.canRead()));
                if (file.exists() && file.canRead()) {
                    client.writeByte(Bytes.OK);
                } else {
                    client.writeByte(Bytes.MISSING);
                }
            }
            if (b == Bytes.DOWNLOAD) {
                String name = client.getString();
                //System.out.println("About to send file: " + name);
                File file = getFile(name);
                sendFile(client, file);
            }

        }

    }

    public void start() {
        System.out.println("Server Started!");

        //This is a test client
        new Thread(() -> {
            Connector client = null;
            try {
                client = new Connector("0.0.0.0", 8888);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Test Client on!");
            System.out.println("Server Ready: " + client.sayHello());
            System.out.println("test.txt available: " + client.requestFile("test.txt"));
            System.out.println("\nDownloading Test File...\n======== Test File ========");
            System.out.println(client.downloadFile("test.txt"));
            System.out.println("======= End Of File =======");
        }).start();

        while (true) {
            try {
                Socket client = server.accept();
                System.out.println("New connection!");
                Connector connection = new Connector(client);
                new Thread(() -> {
                    try {
                        handle(connection);
                    } catch (SocketException e) {
                        System.out.println("The client connection was closed unexpectedly");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();

            } catch (SocketException e) {
                System.out.println("The client connection was closed unexpectedly");
                break;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    File getFile(String name) {
        return new File(System.getProperty("user.dir") + File.separator + name);
    }
}


