package Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Queue;
import java.util.Random;
import java.util.Stack;

public class Session extends Thread {
    Socket socket;
    String studentID;
    DataOutputStream out;
    DataInputStream in;
    Stack<String> messages = new Stack<>();

    Session(Socket socket, String studentID, DataOutputStream out, DataInputStream in) throws SocketException {

        this.socket=socket;
        this.socket.setSendBufferSize(ServerMain.MAX_BUFFER_SIZE);
        this.socket.setReceiveBufferSize(ServerMain.MAX_BUFFER_SIZE);
        this.studentID=studentID;
        this.out=out;
        this.in=in;

        if(!ServerMain.signedStudents.contains(studentID))
        {
            (new File("src/Server/"+studentID)).mkdir();
            (new File("src/Server/"+studentID+"/public")).mkdir();
            (new File("src/Server/"+studentID+"/private")).mkdir();
        }

        ServerMain.onlineStudents.add(studentID);
        ServerMain.signedStudents.add(studentID);
        ServerMain.activeSessions.add(this);
    }
    public void download(String owner, String filename, String type)
    {
        try {
            File file = new File("src/Server/" + owner + "/" + type + "/" + filename);
            if (file.exists())
            {
                out.writeUTF("File exists");

                long f = file.length();
                String fS = String.valueOf(f);
                out.writeUTF(fS);

                String reply = in.readUTF();
                if (reply.equalsIgnoreCase("Not allowed")) {
                    System.out.println("Transmission not allowed");
                    return;
                }
                int chunk_size = ServerMain.MAX_CHUNK_SIZE;
                byte[] array = Files.readAllBytes(
                        Paths.get(file.getPath()));

                int numberOfChunks = (int) Math.ceil(array.length * 1.0 / chunk_size);
                int off = 0;
                System.out.println("tot: " + file.length());
                for (int i = 1; i <= numberOfChunks; i++) {
                    if (off + chunk_size <= array.length) {
                        out.write(array, off, chunk_size);
                        off += chunk_size;
                    } else {
                        out.write(array, off, array.length - off);
                        off = array.length;
                    }
                    out.flush();
                    System.out.println("Sent " + String.format("%.2f", off * 100.0 / array.length) + "%");

                }
                String fin = in.readUTF();
                System.out.println(fin);
            } else out.writeUTF("No such file");
        }catch (Exception e)
        {
            System.out.println("ALERT: "+studentID+" disconnected/failed transmission");
        }
    }
    public void upload()
    {
        try
        {
            String filename = in.readUTF();
            String mode = in.readUTF();
            System.out.println(mode);
            int filesize = Integer.valueOf(in.readUTF());
            System.out.println(filesize);

            if (filesize + ServerMain.CUR_BUF <= ServerMain.MAX_BUFFER_SIZE && (mode.equals("public") || mode.equals("private")))
            {
                out.writeUTF("Allowed");
            }
            else {
                out.writeUTF("Not allowed");
                return;
            }


            int chunk_size = new Random().nextInt(ServerMain.MAX_CHUNK_SIZE) + ServerMain.MIN_CHUNK_SIZE;
            int numberOfChunks = (int) Math.ceil(filesize * 1.0 / chunk_size);

            out.writeUTF(String.valueOf(chunk_size));
            String fileID="File_" + ServerMain.fileCount;
            out.writeUTF(fileID);
            ServerMain.fileCount++;

            byte[] array = new byte[filesize];

            int off = 0;
            boolean broken = false;

            ServerMain.CUR_BUF+=chunk_size;

            for (int i = 1; i <= numberOfChunks; i++) {
                if (off + chunk_size <= filesize) {
                    in.read(array, off, chunk_size);
                    off += chunk_size;
                } else {
                    in.read(array, off, filesize - off);
                    off = filesize;
                }
                System.out.println("off: "+off);
                out.writeUTF("Acknowledged");
                String reply = in.readUTF();
                if (reply.equalsIgnoreCase("Timedout")) {
                    broken = true;
                    break;
                }
            }
            String fin=in.readUTF();
            if (!broken&&fin.equalsIgnoreCase("Completed")) {
                System.out.println(studentID+" uploaded "+filename+" (fileID: "+fileID+")");
                Path path = Paths.get("src/Server/" + studentID + "/" + mode + "/" + filename);
                Files.write(path, array);

            } else {
                ServerMain.fileCount--;
            }
            ServerMain.CUR_BUF-=chunk_size;
        }
        catch (Exception e)
        {
            System.out.println("ALERT: "+studentID+" disconnected/failed transmission");
        }
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
                else if(choice.equalsIgnoreCase("Lookmyfiles"))
                {
                    ArrayList<String> filenames = new ArrayList<>();
                    File[] files = new File("src/Server/" + studentID + "/public").listFiles();
                    for (var u : files) {
                        filenames.add(u.getName());
                    }
                    out.writeUTF(filenames.toString());

                    filenames = new ArrayList<>();
                    files = new File("src/Server/" + studentID + "/private").listFiles();
                    for (var u : files) {
                        filenames.add(u.getName());
                    }
                    out.writeUTF(filenames.toString());
                }
                else if (choice.equalsIgnoreCase("Lookfrom"))
                {
                    String owner=in.readUTF();
                    ArrayList<String> filenames = new ArrayList<>();
                    File[] files = new File("src/Server/" + owner + "/public").listFiles();
                    for (var u : files) {
                        filenames.add(u.getName());
                    }
                    out.writeUTF(filenames.toString());
                }
                else if(choice.equalsIgnoreCase("Downloadmyfile"))
                {
                    String filename,type;
                    filename=in.readUTF();
                    type=in.readUTF();
                    download(studentID,filename,type);
                }
                else if(choice.equalsIgnoreCase("Downloadfrom"))
                {
                    String owner,filename;
                    owner=in.readUTF();
                    filename=in.readUTF();
                    download(owner,filename,"public");
                }
                else if(choice.equalsIgnoreCase("Upload")) {
                    upload();
                }
                else if(choice.equalsIgnoreCase("Request"))
                {
                    String message=in.readUTF();
                    System.out.println("got "+message);
                    String reqID="ReqID"+ServerMain.reqCount++;
                    ServerMain.requests.put(reqID,studentID);
                    for(var u:ServerMain.activeSessions)
                    {
                        if(!u.studentID.equals(studentID))
                        {
                            u.messages.add("Request from "+studentID+" ("+reqID+"): "+
                                           "Message- "+message);
                        }
                    }
                }
                else if(choice.equalsIgnoreCase("Message"))
                {
                    String re="Messages: \n"+messages.toString();
                    out.writeUTF(re);
                    messages.clear();
                }
                else if(choice.equalsIgnoreCase("checkreq"))
                {
                    String reqID=in.readUTF();
                    if(ServerMain.requests.containsKey(reqID))
                        out.writeUTF("Request is valid");
                    else
                        out.writeUTF("Request does not exists");
                }
                else if(choice.equalsIgnoreCase("Notify"))
                {
                    String reqID=in.readUTF();
                    String requester=ServerMain.requests.get(reqID);
                    System.out.println("w: "+reqID);
                    System.out.println("requester: "+requester);
                    for(var u:ServerMain.activeSessions)
                    {
                        if(u.studentID.equals(requester))
                        {
                            u.messages.add("Request "+reqID+" was served by "+studentID);
                            break;
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            System.out.println("ALERT: "+studentID+" disconnected/failed transmission");
            ServerMain.onlineStudents.remove(studentID);
            ServerMain.activeSessions.remove(this);
            e.printStackTrace();
        }
    }
}
