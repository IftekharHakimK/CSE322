package Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
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
    Socket fileSocket,messageSocket;
    String studentID;
    DataOutputStream messageOut,fileOut;
    DataInputStream messageIn,fileIn;
    Stack<String> messages = new Stack<>();

    Session(Socket fileSocket,Socket messageSocket, String studentID, DataOutputStream messageOut,
            DataInputStream messageIn,DataOutputStream fileOut, DataInputStream fileIn) throws SocketException {

        this.fileSocket=fileSocket;
        this.messageSocket=messageSocket;
        this.fileSocket.setSendBufferSize(ServerMain.MAX_BUFFER_SIZE);
        this.fileSocket.setReceiveBufferSize(ServerMain.MAX_BUFFER_SIZE);
        this.studentID=studentID;
        this.messageOut=messageOut;
        this.messageIn=messageIn;
        this.fileOut=fileOut;
        this.fileIn=fileIn;

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
                fileOut.writeUTF("File exists");

                long f = file.length();
                String fS = String.valueOf(f);
                fileOut.writeUTF(fS);

                String reply = fileIn.readUTF();
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
                        fileOut.write(array, off, chunk_size);
                        off += chunk_size;
                    } else {
                        fileOut.write(array, off, array.length - off);
                        off = array.length;
                    }
                    fileOut.flush();
                    System.out.println("Sent " + String.format("%.2f", off * 100.0 / array.length) + "%");

                }
                String fin = fileIn.readUTF();
                System.out.println(fin);
            } else fileOut.writeUTF("No such file");
        }catch (Exception e)
        {
            System.out.println("ALERT: "+studentID+" disconnected/failed transmission");
        }
    }
    public void upload()
    {
        try
        {
            String filename = fileIn.readUTF();
            String mode = fileIn.readUTF();
            System.out.println(mode);
            int filesize = Integer.valueOf(fileIn.readUTF());
            System.out.println(filesize);
            System.out.println(filesize + ServerMain.CUR_BUF <= ServerMain.MAX_BUFFER_SIZE);
            if (filesize + ServerMain.CUR_BUF <= ServerMain.MAX_BUFFER_SIZE && (mode.equals("public") || mode.equals("private")))
            {
                fileOut.writeUTF("Allowed");
            }
            else {
                fileOut.writeUTF("Not allowed");
                return;
            }


            int chunk_size = new Random().nextInt(ServerMain.MAX_CHUNK_SIZE) + ServerMain.MIN_CHUNK_SIZE;
            int numberOfChunks = (int) Math.ceil(filesize * 1.0 / chunk_size);

            fileOut.writeUTF(String.valueOf(chunk_size));
            String fileID="File_" + ServerMain.fileCount;
            fileOut.writeUTF(fileID);
            ServerMain.fileCount++;

            byte[] array = new byte[filesize];

            int off = 0;
            boolean broken = false;

            ServerMain.CUR_BUF+=chunk_size;

            for (int i = 1; i <= numberOfChunks; i++) {
                if (off + chunk_size <= filesize) {
                    fileIn.read(array, off, chunk_size);
                    off += chunk_size;
                } else {
                    fileIn.read(array, off, filesize - off);
                    off = filesize;
                }
                System.out.println("off: "+off);
                fileOut.writeUTF("Acknowledged");
                String reply = fileIn.readUTF();
                if (reply.equalsIgnoreCase("Timedout")) {
                    broken = true;
                    break;
                }
            }
            String fin=fileIn.readUTF();
            if (!broken&&fin.equalsIgnoreCase("Completed")) {
                System.out.println(studentID + " uploaded " + filename + " (fileID: " + fileID + ")");
                Path path = Paths.get("src/Server/" + studentID + "/" + mode + "/" + filename);
                Files.write(path, array);

                String reqID = fileIn.readUTF();
                if (!reqID.equalsIgnoreCase("none"))
                {
                    inform(reqID);
                }
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
    public void inform(String reqID)
    {
        String requester=ServerMain.requests.get(reqID);
        for(var u:ServerMain.activeSessions)
        {
            if(u.studentID.equals(requester))
            {
                u.messages.add("Request "+reqID+" was served by "+studentID);
                break;
            }
        }
    }

    @Override
    public void run() {
        try
        {
            while (true)
            {
                String choice=messageIn.readUTF();
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
                    messageOut.writeUTF(list.toString());
                }
                else if(choice.equalsIgnoreCase("Logout"))
                {
                    ServerMain.onlineStudents.remove(studentID);
                    ServerMain.activeSessions.remove(this);
                    messageOut.writeUTF("Logged out");
                    fileSocket.close();
                    messageSocket.close();
                    return;
                }
                else if(choice.equalsIgnoreCase("Lookmyfiles"))
                {
                    ArrayList<String> filenames = new ArrayList<>();
                    File[] files = new File("src/Server/" + studentID + "/public").listFiles();
                    for (var u : files) {
                        filenames.add(u.getName());
                    }
                    messageOut.writeUTF(filenames.toString());

                    filenames = new ArrayList<>();
                    files = new File("src/Server/" + studentID + "/private").listFiles();
                    for (var u : files) {
                        filenames.add(u.getName());
                    }
                    messageOut.writeUTF(filenames.toString());
                }
                else if (choice.equalsIgnoreCase("Lookfrom"))
                {
                    String owner=messageIn.readUTF();
                    ArrayList<String> filenames = new ArrayList<>();
                    File[] files = new File("src/Server/" + owner + "/public").listFiles();
                    for (var u : files) {
                        filenames.add(u.getName());
                    }
                    messageOut.writeUTF(filenames.toString());
                }
                else if(choice.equalsIgnoreCase("Downloadmyfile"))
                {
                    Thread temp = new Thread(()->
                    {
                        try {
                            String filename, type;
                            filename = fileIn.readUTF();
                            type = fileIn.readUTF();
                            download(studentID, filename, type);
                        }
                        catch (IOException e){}
                    });
                    temp.start();
                }
                else if(choice.equalsIgnoreCase("Downloadfrom"))
                {
                    Thread temp = new Thread(()->
                    {
                        try
                        {
                            String owner,filename;
                            owner=fileIn.readUTF();
                            filename=fileIn.readUTF();
                            download(owner,filename,"public");
                        }
                        catch (IOException e) { }
                    });
                    temp.start();
                }
                else if(choice.equalsIgnoreCase("Upload")) {
                    Thread temp = new Thread(()->
                    {
                        upload();
                    });
                    temp.start();
                }
                else if(choice.equalsIgnoreCase("Request"))
                {
                    String message=messageIn.readUTF();
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
                    messageOut.writeUTF(re);
                    messages.clear();
                }
                else if(choice.equalsIgnoreCase("checkreq"))
                {
                    String reqID=messageIn.readUTF();
                    if(ServerMain.requests.containsKey(reqID))
                        messageOut.writeUTF("Request is valid");
                    else
                        messageOut.writeUTF("Request does not exists");
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
