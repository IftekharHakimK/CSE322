package ClientSide;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class ClientMain {
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Student ID: ");
        String studentID = scanner.next();
        Socket socket = new Socket("localhost",6666);
        System.out.println("Connection established");
        DataOutputStream out=new DataOutputStream(socket.getOutputStream());
        DataInputStream in=new DataInputStream(socket.getInputStream());
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
        }
    }
}
