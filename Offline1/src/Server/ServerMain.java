package Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class ServerMain {
    static int MAX_BUFFER_SIZE=524288000;
    static int MIN_CHUNK_SIZE=1000;
    static int MAX_CHUNK_SIZE=1024;
    static Set<String> onlineStudents = new HashSet<>();
    static Set<String> signedStudents = new HashSet<>();
    static Set<Session> activeSessions = new HashSet<>();
    static int fileCount=0;
    static HashMap<String,String> requests=new HashMap<>(); //key: ID, value: requester
    static int reqCount=0;

    public static void main(String [] args) throws IOException
    {
        ServerSocket serverSocket = new ServerSocket(6666);
        while (true)
        {
            Socket socket = serverSocket.accept();
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in=new DataInputStream(socket.getInputStream());

            String address=socket.getInetAddress().toString();
            int port = socket.getPort();
            String studentID=in.readUTF();
            System.out.println(studentID+" connected with address: "+address+" port: "+port);
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
