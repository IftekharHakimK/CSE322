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
    static int MIN_CHUNK_SIZE=1000;
    static int MAX_CHUNK_SIZE=1024;

    static DataOutputStream out;
    static DataInputStream in;
    static Socket socket;
    static Scanner scanner;

    public static boolean upload(String filename, String mode)
    {
        try
        {
            File file = new File(filename);
            if(file.exists())
            {
                out.writeUTF(filename);
                out.writeUTF(mode);
                long f=file.length();
                String fS=String.valueOf(f);
                out.writeUTF(fS);

                String reply=in.readUTF().toString();
                if(reply.equalsIgnoreCase("Not allowed"))
                {
                    System.out.println("Transmission not allowed");
                    return false;
                }
                int chunk_size= Integer.valueOf(in.readUTF());
                String fileID=in.readUTF();
                byte[] array = Files.readAllBytes(
                        Paths.get(file.getPath()));

                int numberOfChunks=(int)Math.ceil(array.length*1.0/chunk_size);
                int off=0;
                System.out.println("tot: "+file.length());
                for(int i=1;i<=numberOfChunks;i++)
                {
                    if(off+chunk_size<=array.length)
                    {
                        out.write(array,off,chunk_size);
                        off+=chunk_size;
                    }
                    else
                    {
                        out.write(array,off,array.length-off);
                        off=array.length;
                    }
                    out.flush();
                    System.out.println("Sent "+String.format("%.2f",off*100.0/array.length)+"%");
                    try {
                        socket.setSoTimeout(3000);
                        String ack = in.readUTF();
                        out.writeUTF("Ok");
                        socket.setSoTimeout(0);
                    }catch (SocketTimeoutException e)
                    {
                        System.out.println("Timed out");
                        out.writeUTF("Timedout");
                        return false;
                    }
                }
                out.writeUTF("Completed");
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
            String reply=in.readUTF();
            System.out.println(reply);
            if(reply.equals("File exists"))
            {
                int filesize = Integer.valueOf(in.readUTF());
                System.out.println(filesize);
                if (filesize <= in.available() + socket.getReceiveBufferSize())
                    out.writeUTF("Allowed");
                else {
                    out.writeUTF("Not allowed");
                    return;
                }
                int chunk_size= MAX_CHUNK_SIZE;
                byte[] array=new byte[filesize];
                int numberOfChunks=(int)Math.ceil(array.length*1.0/chunk_size);
                int off=0;
                for (int i = 1; i <= numberOfChunks; i++) {
                    if (off + chunk_size <= filesize) {
                        in.read(array, off, chunk_size);
                        off += chunk_size;
                    } else {
                        in.read(array, off, filesize - off);
                        off = filesize;
                    }
                    System.out.println("Downloaded "+String.format("%.2f",off*100.0/array.length)+"%");
                }
                if(off==filesize)
                {
                    out.writeUTF("Download completed");
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
    public static void main(String[] args) throws IOException {
        scanner = new Scanner(System.in);
        System.out.print("Student ID: ");
        String studentID = scanner.next();
        System.out.print("Intended Port: ");
        String intendedPort = scanner.next();

        socket = new Socket("localhost", 6666, InetAddress.getByName("localhost"), Integer.parseInt(intendedPort));
        socket.setSendBufferSize(MAX_BUFFER_SIZE);
        socket.setReceiveBufferSize(MAX_BUFFER_SIZE);

        System.out.println("Connection established");
        out=new DataOutputStream(socket.getOutputStream());
        in=new DataInputStream(socket.getInputStream());
        out.writeUTF(studentID);
        String msg=in.readUTF();
        System.out.println(msg);
        if(msg.equalsIgnoreCase("Login failed"))
        {
            return;
        }
        while (true)
        {
            String choice=scanner.next();
            if(choice.equalsIgnoreCase("Users"))
            {
                out.writeUTF(choice);
                String reply=in.readUTF();
                System.out.println(reply);
            }
            else if(choice.equalsIgnoreCase("Logout"))
            {
                out.writeUTF(choice);
                String reply=in.readUTF();
                System.out.println(reply);
                socket.close();
                return;
            }
            else if(choice.equalsIgnoreCase("Lookmyfiles"))
            {
                out.writeUTF(choice);
                String reply="Public files: "+in.readUTF()+"\n"+
                             "Private files: "+in.readUTF();
                System.out.println(reply);
            }
            else if(choice.equalsIgnoreCase("Lookfrom"))
            {
                out.writeUTF(choice);
                System.out.print("Owner: ");
                String owner=scanner.next();
                out.writeUTF(owner);
                String re="Public files of "+owner+": "+in.readUTF();
                System.out.println(re);
            }
            else if(choice.equalsIgnoreCase("Downloadmyfile"))
            {
                out.writeUTF(choice);
                String filename,type;
                System.out.print("Filename: ");
                filename=scanner.next();
                System.out.print("Type: ");
                type=scanner.next();
                out.writeUTF(filename);
                out.writeUTF(type);
                download(filename);
            }
            else if(choice.equalsIgnoreCase("Downloadfrom"))
            {
                out.writeUTF(choice);
                String owner,filename;
                System.out.print("Owner: ");
                owner=scanner.next();
                System.out.print("Filename: ");
                filename=scanner.next();
                out.writeUTF(owner);
                out.writeUTF(filename);
                download(filename);
            }
            else if(choice.equalsIgnoreCase("Upload"))
            {
                out.writeUTF(choice);
                String filename,mode;
                System.out.print("Filename: ");
                filename=scanner.next();
                System.out.print("Mode: ");
                mode=scanner.next();
                upload(filename,mode);
            }
            else if(choice.equalsIgnoreCase("Request"))
            {
                out.writeUTF(choice);
                System.out.print("Put your request message: ");

                String message=scanner.next();
                System.out.println("Placed: "+message);
                out.writeUTF(message);
            }
            else if(choice.equalsIgnoreCase("Message"))
            {
                out.writeUTF(choice);
                System.out.println("Your unread messages here (recent first): \n"+in.readUTF());
            }
            else if(choice.equalsIgnoreCase("Serve"))
            {
                out.writeUTF("Checkreq");
                System.out.print("ReqID: ");
                String reqID=scanner.next();
                out.writeUTF(reqID);
                String re=in.readUTF();
                if(re.equalsIgnoreCase("Request is valid"))
                {
                    String filename,mode;
                    System.out.print("Filename: ");
                    filename=scanner.next();
                    out.writeUTF("Upload");
                    boolean f=upload(filename,"public");
                    if(f)
                    {
                        out.writeUTF("Notify");
                        out.writeUTF(reqID);
                    }
                }
                else
                {
                    System.out.println("Request does not exists");
                    continue;
                }
            }
        }
    }
}

