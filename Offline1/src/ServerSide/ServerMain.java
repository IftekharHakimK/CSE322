package ServerSide;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ServerMain {

    static Set<String> onlineStudents = new HashSet<>();
    static Set<String> signedStudents = new HashSet<>();

    public static void main(String [] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(6666);
        while (true)
        {
            Socket socket = serverSocket.accept();
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in=new DataInputStream(socket.getInputStream());
            String studentID=in.readUTF();
            if(onlineStudents.contains(studentID))
            {
                out.writeUTF("Login failed");
                continue;
            }
            out.writeUTF("Login successful");
            Thread session = new Session(socket,studentID,out,in);
            session.start();
        }
    }
}
