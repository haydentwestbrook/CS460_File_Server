import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.nio.file.*;
import java.util.Scanner;

/**
 * Created by mkg on 12/5/2015.
 * Client for File Server. To use this program, pass in the root directory at the command line. This will be the starting
 * point for where files get stored that are retrieved from the server.
 */

public class Client {
	private Socket socket;
	private Path rootDir;
	private boolean connected = false;

	public Client(String dir) throws FileNotFoundException {
		/**
		 * Creates a client that will connect to FileServer instance
		 * @param dir = The root directory to store retrieved files
		 * @throws FileNotFoundException
		 */
		// first check if dir is actually a dir and exists
		File rootFile = new File(dir);
		if (!rootFile.exists()) {
			System.out.println("Error: The root directory either does not exist or it is a file");
			throw new FileNotFoundException();
		}
		// if we get here, dir exists, so set it as the root directory
		this.rootDir = Paths.get(dir);
	}

	public boolean isConnected(){
		// tell if client is currently connected
		return connected;
	}

	private boolean connect(String server, int port){
		// connection to server
		// convert server string to SocketAddress
		Inet4Address serverIp = null;
		System.out.print("Connecting to " + server + "...");

		// get the ip address object of what was passed in
		try {
			serverIp = (Inet4Address) Inet4Address.getByName(server);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		try{ // connect to the server using the ip address object
			this.socket = new Socket(serverIp.getHostAddress(), port);
		} catch (Exception e) {
			System.out.println("FAILED");
			System.out.println("Error creating socket to server:");
			e.printStackTrace();
			return false;
		}
		System.out.println("SUCCESS");
		this.connected = true;
		return true;
	}

	/**********************************************************
	 * MAIN
	 **********************************************************/
	public static void main(String[] args) {
		// Check argument passed in by user. args[0] should be local root directory for client.
		if (args.length > 1 || args.length == 0){
			System.out.println("Error: Program takes 1 argument");
			return;
		}
		System.out.println(args[0]);
		// Variable declaration and init
		Scanner in = new Scanner(System.in);	// local input from client side to be sent to server
		String inputStr;
		Client c = null;
		try {
			c = new Client(args[0]);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		/******************************************************
		 * Main Program Loop
		 ******************************************************/
		while (true) {
			System.out.print("fileserver>");
			inputStr = in.nextLine();
			String[] inputArgs = inputStr.split(" ");

			// *** debugging ***
			/*for (String arg : inputArgs){
				System.out.print(arg + " ");
			}
			System.out.println("");*/
			// end debugging

			// get option from user input
			if (inputArgs[0].equals("OPEN")){
				// check user input arg length
				if (inputArgs.length != 3){
					System.out.println("Error: Too many arguments for OPEN\n");
					continue;
				}
				// check if connection is currently active -- no need to try to reconnect
				if (c != null && c.isConnected()) {
					System.out.println("Connection is already open");
					continue;
				}
				// connect to the server
				if (!(c.connect(inputArgs[1], Integer.parseInt(inputArgs[2])))) {
					System.out.println("Error connecting to server");
					continue;
				}
			}
			try {
				BufferedReader fromServer = new BufferedReader(new InputStreamReader(c.socket.getInputStream()));
				DataOutputStream toServer = new DataOutputStream(c.socket.getOutputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
