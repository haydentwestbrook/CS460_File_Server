import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

/**
 * Created by hayden on 12/5/15.
 */
public class CS460_File_Server {

    public static int PORT_NUMBER = 23657;
    public static String root = "/home/hayden/Desktop/";

    public static void main(String[] args) {

        System.out.println("Starting server.");

        try {
            ServerSocket socket= new ServerSocket(PORT_NUMBER);
            while(true) {
                handleClient(socket.accept());
            }

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket client) throws Exception {

        System.out.println("Accepted client.");
        BufferedReader fromClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
        DataOutputStream toClient = new DataOutputStream(client.getOutputStream());

        while(true) {
            String[] response = fromClient.readLine().split(" ");
            System.out.print("Response: ");
            for (String str : response) {
                System.out.print(str + " ");
            }
            System.out.println();

            String command = response[0];
            String[] args = Arrays.copyOfRange(response, 1, response.length);

            if (command.equals("GET")) {
                getFile(args, toClient);
            }
            if (command.equals("CLOSE")) {
                System.out.println("Closing connection.");
                client.close();
                return;
            }
        }
    }

    private static void getFile(String[] args, DataOutputStream toClient) throws Exception {
        System.out.println("Getting file: " + root + args[0]);

        File file = new File(root + args[0]);
        try {
            FileInputStream fileStream = new FileInputStream(file);
            toClient.writeChars("DATA 200 OK \r\ncontent-length: " + fileStream.available() + "\r\n");
            int data;
            while((data = fileStream.read()) != -1) {
                toClient.write(data);
            }
            System.out.println("File sent successfully.");

        } catch(Exception e) {
            System.out.println("File not found.");
            toClient.writeChars("DATA 404 Not found \r\n");
        }
    }
}
