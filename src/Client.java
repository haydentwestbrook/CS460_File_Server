import java.io.*;
import java.net.*;
import java.nio.Buffer;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Created by mkg on 12/5/2015.
 * Client for File Server. To use this program, pass in the root directory at the command line. This will be the starting
 * point for where files get stored that are retrieved from the server.
 */

public class Client {
	public static int MAX_BYTES = 1024*64;
	private Socket socket = null;
	private byte[] inStream = null;			// stream to read from the server on
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

		try { // setup output stream
			this.inStream = new byte[MAX_BYTES];
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
			this.socket.getInputStream().read(this.inStream, 0, this.inStream.length);	// read bytes from server

			// get header information
			String headerStr = new String(this.inStream, Charset.forName("UTF-8"));
			String[] headerStrArray = headerStr.split("\r\n");

			// parse header information and write the file
			if (headerStrArray[0].contains("DATA 200 OK")) {
				// parse line for length
				int fileLength = 0;
				for (String s : headerStrArray[1].split(" ")) {
					try {
						fileLength = Integer.parseInt(s);
					} catch (NumberFormatException e) {
						continue;	// keep going if result is not a number
					}
				}
				if (fileLength == 0){ // if fileLength is still 0, either no file was retrieved or problem with header
					System.out.println("Error: file length was zero...no file retrieved ?");
					return false;
				}
				else { // fileLength was some positive number so we can make a file
					this.socket.getInputStream().read(this.inStream, 0, fileLength);	// read bytes from server into inStream
					byte[] tmp = Arrays.copyOf(this.inStream, fileLength);				// copy part of inStream to tmp buffer

					// write the file
					String fileAbsPath = this.rootDir + destDir;	// construct the destination path string
					Path filePath = Paths.get(fileAbsPath);			// convert string to Path object to make a file
					try {
						Files.write(filePath, tmp);					// write the file
						System.out.println("File copied successfully");
					} catch (Exception e){
						System.out.println("Error: Problem writing file");
						e.printStackTrace();
						return false;
					}
				}
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
				int port = 0;
				try { // check that user entered a number and not text as port number
					port = Integer.parseInt(inputArgs[2]);
				} catch (NumberFormatException e) {
					System.out.println("Error: port argument must be a number");
					continue;
				}
				if (!(c.connect(inputArgs[1], port))) {
					System.out.println("Error connecting to server");
				}
			}

			else if (inputArgs[0].equals("GET")) {
				if (c == null || !c.isConnected()) {
					System.out.println("You are not connected to the file server. Use OPEN <server> <port>");
				}
				else {
					c.get(inputArgs[1], inputArgs[2]);
					Arrays.fill(c.inStream, (byte)0);
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
