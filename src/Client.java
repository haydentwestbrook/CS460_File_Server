import java.io.*;
import java.net.*;
import java.nio.Buffer;
import java.nio.file.*;
import java.util.Scanner;

/**
 * Created by mkg on 12/5/2015.
 * Client for File Server. To use this program, pass in the root directory at the command line. This will be the starting
 * point for where files get stored that are retrieved from the server.
 */

public class Client {
	private Socket socket = null;
	private BufferedReader inStream = null;			// stream to read from the server on
	private DataOutputStream outStream = null;		// stream to write info to the server
	private Path rootDir;							// the root directory where client will be storing files

	/***** Constructor ***********************************************
	 * Creates a client that will connect to FileServer instance
	 * @param dir The root directory to store retrieved files
	 * @throws FileNotFoundException
	 *****************************************************************/
	public Client(String dir) throws FileNotFoundException {
		// first check if dir is actually a dir and exists
		File rootFile = new File(dir);
		if (!rootFile.exists()) {
			System.out.println("Error: The root directory either does not exist or it is a file");
			throw new FileNotFoundException();
		}
		// if we get here, dir exists, so set it as the root directory
		this.rootDir = Paths.get(dir);
	}

	/***** isConnected() *********************************************
	 *  Checks if connnection to server is currently active
	 * @return boolean
	 *****************************************************************/
	public boolean isConnected(){
		// tell if client is currently connected
		if (this.socket == null)
			return false;
		else
			return !this.socket.isClosed();	// I have to use isClosed() because socket.isConnected() doesn't work right
	}

	/***** connect() *************************************************
	 * Makes TCP connection to server
	 * @param server String server hostname (not sure if IP will work yet)
	 * @param port int port number of server
	 * @return boolean -- true if connection worked, false if not
	 *****************************************************************/
	public boolean connect(String server, int port){
		// connection to server
		// convert server string to SocketAddress
		Inet4Address serverIp = null;
		System.out.print("Connecting to " + server + "...");

		try { // get the ip address object of what was passed in
			serverIp = (Inet4Address) Inet4Address.getByName(server);
		} catch (UnknownHostException e) {
			System.out.println("Error: failed to convert hostname to IP");
			e.printStackTrace();
			return false;
		}

		try{ // connect to the server using the ip address object
			this.socket = new Socket(serverIp.getHostAddress(), port);
		} catch (Exception e) {
			System.out.println("FAILED");
			System.out.println("Error creating socket to server:");
			e.printStackTrace();
			return false;
		}

		try { // setup input and output streams
			this.inStream = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
			this.outStream = new DataOutputStream(this.socket.getOutputStream());
		} catch (IOException e) {
			System.out.println("Error: problem creating input and output streams on socket");
			e.printStackTrace();
			return false;
		}
		System.out.println("SUCCESS");
		return true;
	}

	/***** get() *****************************************************
	 * Where most of the work is done -- gets a file from the server
	 * @param toServer InputStreamReader buffer that will be written to and sent to server. Contains GET <source dir>
	 * @param sourceDir
	 * @param destDir
	 * @return boolean
	 *****************************************************************/
	public boolean get(InputStreamReader toServer, String sourceDir, String destDir){
		return true;
	}

	/***** close() ***************************************************
	 * Closes the TCP connection to the server gracefully by sending a CLOSE command to the server before
	 * doing this.socket.close() command. This is because the server will error out due to the connection being null
	 * if we do a close without the server knowing.
	 * @return boolean
	 *****************************************************************/
	public boolean close(){
		// tell server to close and clear out all socket info
		try {
			this.outStream.writeBytes("CLOSE");
			this.socket.close();
			this.inStream.close();
			this.outStream.close();
			// added these null statements because this function wasn't closing properly with just the above 4 lines
			this.socket = null;
			this.inStream = null;
			this.outStream = null;
		} catch (IOException e) {
			System.out.println("Error: writing CLOSE command to server buffer failed");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/*****************************************************************
	 * MAIN
	 *****************************************************************/
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
			if (inputArgs[0].equals("OPEN")) {
				// check user input arg length
				if (inputArgs.length != 3) {
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
				}
			}

			else if (inputArgs[0].equals("CLOSE")) {
				// check that there is a connection to close first
				if (c == null || !c.isConnected()) {
					System.out.println("There is no open connection to close");
				}

				else {
					if (!c.close())
						System.out.println("Error closing connection to server");
					else
						System.out.println("Connection closed");
				}

			}

			else if (inputArgs[0].equalsIgnoreCase("exit")){
				System.out.println("Exiting...");
				// close connection
				if (!(c == null) && c.isConnected())
					c.close();
				break;
			}
		}
	}

}
