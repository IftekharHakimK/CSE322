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
    static int CUR_BUF=0;
    static int MIN_CHUNK_SIZE=1<<5;
    static int MAX_CHUNK_SIZE=1<<10;
    static Set<String> onlineStudents = new HashSet<>();
    static Set<String> signedStudents = new HashSet<>();
    static Set<Session> activeSessions = new HashSet<>();
    static int fileCount=0;
    static HashMap<String,String> requests=new HashMap<>(); //key: ID, value: requester
    static int reqCount=0;

    public static void main(String [] args) throws IOException
    {
        ServerSocket serverfileSocket = new ServerSocket(6666);
        ServerSocket serverMessageSocket = new ServerSocket(6667);
        while (true)
        {
            Socket fileSocket = serverfileSocket.accept();
            Socket messageSocket = serverMessageSocket.accept();

            fileSocket.setReceiveBufferSize(MAX_BUFFER_SIZE);
            fileSocket.setSendBufferSize(MAX_BUFFER_SIZE);

            DataOutputStream fileOut = new DataOutputStream(fileSocket.getOutputStream());
            DataInputStream fileIn=new DataInputStream(fileSocket.getInputStream());

            DataOutputStream messageOut = new DataOutputStream(messageSocket.getOutputStream());
            DataInputStream messageIn=new DataInputStream(messageSocket.getInputStream());

            String address=fileSocket.getInetAddress().toString();

            int file_port = fileSocket.getPort();
            int message_port = messageSocket.getPort();
            String studentID=messageIn.readUTF();

            System.out.println(studentID+" connected with address: "+address+" port: "+file_port +" and "+message_port);
            if(onlineStudents.contains(studentID))
            {
                messageOut.writeUTF("Login failed");
                continue;
            }
            messageOut.writeUTF("Login successful");

            Thread session = new Session(messageSocket,fileSocket,studentID,messageOut,messageIn,fileOut,fileIn);
            session.start();
        }
    }
}
