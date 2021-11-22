package ServerSide;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Session extends Thread {
    Socket socket;
    String studentID;
    DataOutputStream out;
    DataInputStream in;
    Session(Socket socket, String studentID, DataOutputStream out, DataInputStream in)
    {
        this.socket=socket;
        this.studentID=studentID;
        this.out=out;
        this.in=in;
        ServerMain.onlineStudents.add(studentID);
        ServerMain.signedStudents.add(studentID);
        ServerMain.activeSessions.add(this);
    }

    @Override
    public void run() {
        try
        {
            while (true)
            {
                String choice=in.readUTF();
                if(choice.equalsIgnoreCase("Users"))
                {
                    ArrayList<String> list = new ArrayList<>();
                    for(var u:ServerMain.signedStudents)
                    {
                        if(ServerMain.onlineStudents.contains(u))
                            list.add(u+" (Online)");
                        else
                            list.add(u+" (Offline)");
                    }
                    out.writeUTF(list.toString());
                }
                else if(choice.equalsIgnoreCase("Logout"))
                {
                    ServerMain.onlineStudents.remove(studentID);
                    ServerMain.activeSessions.remove(this);
                    out.writeUTF("Logged out");
                    socket.close();
                    return;
                }
            }
        }
        catch (Exception e)
        {
            System.out.println("ALERT: "+studentID+" disconnected/failed transmission");
            ServerMain.onlineStudents.remove(studentID);
            ServerMain.activeSessions.remove(this);
        }
    }
}
