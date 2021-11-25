package Student;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class ClientMain {
    static int MAX_BUFFER_SIZE=524288000;
    static int MIN_CHUNK_SIZE=100000;
    static int MAX_CHUNK_SIZE=200000;

    static DataOutputStream fileOut,messageOut;
    static DataInputStream fileIn,messageIn;
    static Socket fileSocket,messageSocket;
    static Scanner scanner;


    public static boolean upload(String filename, String mode)
    {
        try
        {
            File file = new File(filename);
            if(file.exists())
            {
                fileOut.writeUTF(filename);
                fileOut.writeUTF(mode);
                long f=file.length();
                String fS=String.valueOf(f);
                fileOut.writeUTF(fS);

                String reply=fileIn.readUTF().toString();
                if(reply.equalsIgnoreCase("Not allowed"))
                {
                    System.out.println("Transmission not allowed");
                    return false;
                }
                int chunk_size= Integer.valueOf(fileIn.readUTF());
                String fileID=fileIn.readUTF();
                byte[] array = Files.readAllBytes(Paths.get(file.getPath()));

                int numberOfChunks=(int)Math.ceil(array.length*1.0/chunk_size);
                int off=0;
                System.out.println("tot: "+file.length());
                for(int i=1;i<=numberOfChunks;i++)
                {
                    if(off+chunk_size<=array.length)
                    {
                        fileOut.write(array,off,chunk_size);
                        off+=chunk_size;
                    }
                    else
                    {
                        fileOut.write(array,off,array.length-off);
                        off=array.length;
                    }
                    fileOut.flush();
                    System.out.println("Sent "+String.format("%.2f",off*100.0/array.length)+"%");
                    try {
                        fileSocket.setSoTimeout(3000);
                        String ack = fileIn.readUTF();
                        fileOut.writeUTF("Ok");
                        fileSocket.setSoTimeout(0);
                    }catch (SocketTimeoutException e)
                    {
                        System.out.println("Timed out");
                        fileOut.writeUTF("Timedout");
                        return false;
                    }
                }
                fileOut.writeUTF("Completed");
                System.out.println("Upload completed");
                return true;
            }
            else
            {
                System.out.println("Wrong file name");
                return false;
            }
        }
        catch (Exception e)
        {
            System.out.println("Failed");
            return false;
        }
    }

    public static void download(String filename)
    {
        try
        {
            String reply=fileIn.readUTF();
            System.out.println(reply);
            if(reply.equals("File exists"))
            {
                int filesize = Integer.valueOf(fileIn.readUTF());
                System.out.println(filesize);
                System.out.println("check: "+filesize+" "+fileIn.available() +" "+fileSocket.getReceiveBufferSize());
                if (filesize + fileIn.available() <= fileSocket.getReceiveBufferSize())
                    fileOut.writeUTF("Allowed");
                else {
                    fileOut.writeUTF("Not allowed");
                    return;
                }
                int chunk_size= MAX_CHUNK_SIZE;
                byte[] array=new byte[filesize];
                int numberOfChunks=(int)Math.ceil(array.length*1.0/chunk_size);
                int off=0;
                for (int i = 1; i <= numberOfChunks; i++) {
                    if (off + chunk_size <= filesize) {
                        fileIn.read(array, off, chunk_size);
                        off += chunk_size;
                    } else {
                        fileIn.read(array, off, filesize - off);
                        off = filesize;
                    }
                    //System.out.println("Downloaded "+String.format("%.2f",off*100.0/array.length)+"%");
                }
                if(off==filesize)
                {
                    fileOut.writeUTF("Download completed");
                    Path path = Paths.get(filename);
                    Files.write(path, array);
                    System.out.println("Download completed");
                }
            }
        }catch (Exception e)
        {
            System.out.println("Failed");
        }
    }

    public static void lookUpUsers() throws IOException {
        messageOut.writeUTF("users");
        String reply=messageIn.readUTF();
        System.out.println(reply);
    }
    public static void logout() throws IOException {
        messageOut.writeUTF("logout");
        String reply=messageIn.readUTF();
        System.out.println(reply);
        messageSocket.close();
        fileSocket.close();
        return;
    }
    public static void lookmyfiles() throws IOException {
        messageOut.writeUTF("lookmyfiles");
        String reply="Public files: "+messageIn.readUTF()+"\n"+
                "Private files: "+messageIn.readUTF();
        System.out.println(reply);
    }
    public static void lookfrom () throws IOException {
        messageOut.writeUTF("lookfrom");
        System.out.print("Owner: ");
        String owner=scanner.next();
        messageOut.writeUTF(owner);
        String re="Public files of "+owner+": "+messageIn.readUTF();
        System.out.println(re);
    }
    public static void request() throws IOException {
        messageOut.writeUTF("request");
        System.out.print("Put your request message: ");

        String message=scanner.next();
        System.out.println("Placed: "+message);
        messageOut.writeUTF(message);
    }
    public static void message() throws IOException {
        messageOut.writeUTF("message");
        System.out.println("Your unread messages here (recent first): \n"+messageIn.readUTF());
    }


    public static void main(String[] args) throws IOException {
        scanner = new Scanner(System.in);
        System.out.print("Student ID: ");
        String studentID = scanner.next();

        fileSocket = new Socket("localhost", 6666);
        messageSocket = new Socket("localhost", 6667);

        fileSocket.setSendBufferSize(MAX_BUFFER_SIZE);
        fileSocket.setReceiveBufferSize(MAX_BUFFER_SIZE);

        System.out.println("Connection established");

        fileOut=new DataOutputStream(fileSocket.getOutputStream());
        fileIn=new DataInputStream(fileSocket.getInputStream());
        messageOut=new DataOutputStream(messageSocket.getOutputStream());
        messageIn=new DataInputStream(messageSocket.getInputStream());

        messageOut.writeUTF(studentID);
        String msg=messageIn.readUTF();
        System.out.println(msg);
        if(msg.equalsIgnoreCase("Login failed"))
        {
            fileSocket.close();
            messageSocket.close();
            return;
        }


        Thread fileThread = null;
        while (true)
        {
            System.out.print("Command: ");
            String choice=scanner.next();
            if(choice.equalsIgnoreCase("Users"))
            {
                lookUpUsers();
            }
            else if(choice.equalsIgnoreCase("Logout"))
            {
                logout();
                return;
            }
            else if(choice.equalsIgnoreCase("Lookmyfiles"))
            {
                lookmyfiles();
            }
            else if(choice.equalsIgnoreCase("Lookfrom"))
            {
                lookfrom();
            }
            else if(choice.equalsIgnoreCase("Downloadmyfile"))
            {
                if(fileThread!=null&&fileThread.isAlive()==true)
                {
                    System.out.println("File upload-download status is busy, try later");
                    continue;
                }

                String filename,type;
                System.out.print("Filename: ");
                filename=scanner.next();
                System.out.print("Type: ");
                type=scanner.next();
                messageOut.writeUTF("downloadmyfile");
                fileOut.writeUTF(filename);
                fileOut.writeUTF(type);

                fileThread = new Thread(() ->
                {
                    download(filename);
                });
                fileThread.start();
            }
            else if(choice.equalsIgnoreCase("Downloadfrom"))
            {
                if(fileThread!=null&&fileThread.isAlive()==true)
                {
                    System.out.println("File upload-download status is busy, try later");
                    continue;
                }
                String owner,filename;
                System.out.print("Owner: ");
                owner=scanner.next();
                System.out.print("Filename: ");
                filename=scanner.next();
                messageOut.writeUTF("downloadfrom");

                fileThread = new Thread(() ->
                {
                    try {
                        fileOut.writeUTF(owner);
                        fileOut.writeUTF(filename);
                        download(filename);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                fileThread.start();
            }
            else if(choice.equalsIgnoreCase("Upload"))
            {
                if(fileThread!=null&&fileThread.isAlive()==true)
                {
                    System.out.println("File upload-download status is busy, try later");
                    continue;
                }
                String filename,mode;
                System.out.print("Filename: ");
                filename=scanner.next();
                System.out.print("Mode: ");
                mode=scanner.next();
                messageOut.writeUTF("upload");
                fileThread = new Thread(()->
                {
                    upload(filename,mode);
                });
                fileThread.start();
            }
            else if(choice.equalsIgnoreCase("Request"))
            {
                request();
            }
            else if(choice.equalsIgnoreCase("Message"))
            {
                message();
            }
            else if(choice.equalsIgnoreCase("Serve"))
            {
                if(fileThread!=null&&fileThread.isAlive()==true)
                {
                    System.out.println("File upload-download status is busy, try later");
                    continue;
                }

                System.out.print("ReqID: ");
                String reqID=scanner.next();
                messageOut.writeUTF("Checkreq");
                messageOut.writeUTF(reqID);
                String re=messageIn.readUTF();
                if(re.equalsIgnoreCase("Request is valid"))
                {
                    String filename;
                    System.out.print("Filename: ");
                    filename=scanner.next();
                    messageOut.writeUTF("Upload");
                    fileThread = new Thread(()->
                    {
                        boolean f=upload(filename,"public");
                           /* if(f)
                            {
                                fileOut.writeUTF("Notify");
                                fileOut.writeUTF(reqID);
                            }*/
                    });
                    fileThread.start();

                }
                else
                {
                    System.out.println("Request does not exists");
                }

            }
        }
    }
}

