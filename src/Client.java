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
			this.inStream = new BufferedReader(new InputStreamReader(this.socket.getInputStream(), "UTF-8"));
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
	 * @param sourceDir The file location on the server we are getting (This **MUST** exist on the server!!)
	 * @param destDir the destination directory we are copying that file to
	 * @return boolean
	 *****************************************************************/
	public boolean get(String sourceDir, String destDir){
		// for now just get the buffer and dump it
		try {
			this.outStream.writeBytes("GET " + sourceDir + " \r\n"); // send command to server to get file
			System.out.println("Input from server Dump:");
			String line = this.inStream.readLine();					// read first line, which is basic response from server
			System.out.println(line);
			if (line.contains("DATA 200 OK")) {
				// read next line and get content length
				// then create a file of that length (?)
				System.out.println("*** getting file length...***");
				line = this.inStream.readLine();
				System.out.println(line);

				// parse line for length
				int fileLength = 0;
				for (String s : line.split(" ")) {
					try {
						fileLength = Integer.parseInt(s);
					} catch (NumberFormatException e) {
						continue;
					}
				}
				if (fileLength == 0){
					System.out.println("Error: file length was zero...no file retrieved ?");
					return false;
				}
				System.out.println("Verify length: " + fileLength);
			}
			else { // DATA 404 was returned
				System.out.println("Error getting file");
				return false;
			}
		} catch (IOException e) {
			System.out.println("Error: Problem with get method");
			e.printStackTrace();
			return false;
		}
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

			else if (inputArgs[0].equals("GET")) {
				if (c == null || !c.isConnected()) {
					System.out.println("You are not connected to the file server. Use OPEN <server> <port>");
				}
				else {
					c.get(inputArgs[1], inputArgs[2]);
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
