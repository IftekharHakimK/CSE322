package Basic;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class Client {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Socket socket = new Socket("localhost", 6666, InetAddress.getByName("localhost"),62960);
        System.out.println("Connection established");
        System.out.println("Remote address: "+socket.getInetAddress());
        System.out.println("Remote port: " + socket.getPort());
        System.out.println("Local address: "+socket.getLocalAddress());
        System.out.println("Local port: " + socket.getLocalPort());

        // output buffer and input buffer
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

        // read message from server
        String msg = (String) in.readObject();
        System.out.println(msg);

        // write message to server
        out.writeObject("Hello from client");
    }
}
