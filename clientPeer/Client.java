package clientPeer;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;
import java.io.IOException;
import java.net.Socket;

import org.apache.commons.io.FileUtils;





import lib.FileMetaData;
import lib.NetworkAddress;
import lib.P2pFile;
import lib.Packet;
import lib.Peer;
import lib.SenderReceiver;
import requestReceiver.RequestReceiver;
import requestReceiver.Requester;



public class Client {
	
	static boolean debugFlag=true;
	static int requestReceiverPortNumber = 4576;
	static String selfIpAddress;
	static File currentDirectory;
	
	private Socket socketToServer;
	
	Client(String serverIP, int portNumber){
		// Set self IP Address
		Client.selfIpAddress = NetworkAddress.getIPAddress("enp0s8");
		
		// Connect to the Server and ask for host names available to store files.
		System.out.println("Connecting to server at "+serverIP+":"+portNumber);
		
		// Create socket to server
		socketToServer = new SenderReceiver().returnSocketTo(serverIP, portNumber);
		
		// Create Payload to send to the server
		String portNumberPayload = new Packet(0, Client.selfIpAddress+":"+Client.requestReceiverPortNumber).getPayload();//preparePayLoad(0, fileInfo); // FileNames is Option 0:
//		String portNumberPayload = new Packet(, Client.selfIpAddress+":"+Client.requestReceiverPortNumber).getPayload();//preparePayLoad(0, fileInfo); // FileNames is Option 0:
		// send payload via TCP to server
		new SenderReceiver().sendMesssageViaTCPOn(socketToServer, portNumberPayload);
		
		// Receive Ok from the indexServer
		Packet type1Packet = new Packet(new SenderReceiver().receiveMessageViaTCPOn(socketToServer));
		if(type1Packet.getType() != 1){
			System.err.println("Incorrect packet recieved: "+type1Packet);
			System.exit(-1);
		}
		
		// interact with user
		currentDirectory = new File(System.getProperty("user.dir"));
		try {
			main_menu();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// go to command line interface
	}
	
	ArrayList<Peer> getAvailablePeerList() {
		String portNumberPayload = new Packet(2, "").getPayload();//preparePayLoad(0, fileInfo); // FileNames is Option 0:
		// send payload via TCP to server
		new SenderReceiver().sendMesssageViaTCPOn(socketToServer, portNumberPayload);
		
		ArrayList<Peer> peerList = new ArrayList<Peer>();
		
		// receive list of all IP Addresses and Port Numbers from Server 
		String peerListString = new Packet(new SenderReceiver().receiveMessageViaTCPOn(socketToServer)).getData();
		
		System.out.println("Server said:"+peerListString);
		
		String[] peerArr = peerListString.split(";");
		
		Peer self = new Peer(Client.selfIpAddress, Client.requestReceiverPortNumber);
		
		for(int i=0; i<peerArr.length; i++){
			Peer newPeer = new Peer(peerArr[i]);
			if( newPeer.equals(self) == false){
				peerList.add(newPeer);
			}
		}
		
		// Available list
		System.out.println("Here is the list of all Peers online right now:"+peerList);
		return peerList;
	}

	private void main_menu() throws IOException {
		//assuming space seperators
		Scanner in = new Scanner(System.in);
		Pattern cd = Pattern.compile("^cd (.*)$"); 				//change directory
		Pattern mkdir = Pattern.compile("^mkdir (.*)$");		//make directory
		Pattern mv = Pattern.compile("^mv (.*)$");			//move (local_file remote_file)
		Pattern mvb = Pattern.compile("^mvb (.*)$");		//move back (remote_file local_file)
		Pattern rm = Pattern.compile("^rm (.*)$");				//remove
		Pattern rename = Pattern.compile("^rename (.*) (.*)$");	//rename (current_remote_file_name new_remote_file_name)
		Matcher m;
		String consoleString = "";
		//FileMetadata currentDirectory= new FileMetadata();

		System.out.println("commands supported:cd,mkdir,mv,mvb,rm,rename,pwd,help,exit");
		while (!consoleString.equals("exit")){
			// contains the main menu options
			System.out.println(">");
			consoleString = in.nextLine();

			m = cd.matcher(consoleString);
			if(m.find()) {
				String fileName= m.group(1);
				if (debugFlag) System.out.println("cd command called with parameter "+ fileName);
				cd(fileName);
				continue;
			}
			m = mkdir.matcher(consoleString);
			if(m.find()) {
				String fileName= m.group(1);
				if (debugFlag) System.out.println("mkdir command called with parameter "+ m.group(1));
				mkdir(fileName);
				continue;
			}
			m = mv.matcher(consoleString);
			if(m.find()) {
				String localFileName= m.group(1);
				if (debugFlag) System.out.println("mv command called with parameter "+ m.group(1));
				mv(localFileName);
				continue;
			}
			m = mvb.matcher(consoleString);
			if(m.find()) {
				String localFileName= m.group(1);
				if (debugFlag) System.out.println("mvb command called with parameter "+ m.group(1));
				mvb(localFileName);
				continue;
			}
			m = rm.matcher(consoleString);
			if(m.find()) {
				String fileName= m.group(1);
				if (debugFlag) System.out.println("rm command called with parameter "+ m.group(1));
				rm(fileName);
				continue;
			}		
			m = rename.matcher(consoleString);
			if(m.find()) {
				String fileName= m.group(1);
				String newName= m.group(2);
				if (debugFlag) System.out.println("rename command called with parameter "+ m.group(1)+", "+ m.group(2));
				rename(fileName,newName);
				continue;
			}
			if (consoleString.equals("exit")){
				System.out.println("exiting now.");
				//System.exit(0);
				break;
			}
			if (consoleString.equals("pwd")){
				System.out.println(currentDirectory.getAbsolutePath());
				continue;
			}
			if (consoleString.equals("help")){
				helpMessage();
				continue;
			}
			//catchall
			System.out.println("command "+consoleString+"not recognized, please try again.");
		
		}

		in.close();
	}


	private void helpMessage() {
		System.out.println("list of commands");

		System.out.println("cd .* 			//change directory");
		System.out.println("mkdir .*		//make directory");
		System.out.println("mv .*		//move file onto remote servers (local_file remote_file)");
		System.out.println("mvb .*		//move file back to local client(remote_file local_file)");
		System.out.println("rm .*			//remove from remote servers");
		System.out.println("rename .* .*		//rename (current_remote_file_name new_remote_file_name)");
		System.out.println("pwd			//shows current directory");
		System.out.println("help			//shows this message");
		System.out.println("exit			//exits");
		
		
	}

	private void rename(String fileName, String newName) {
		// TODO Auto-generated method stub
		FileMetaData fmd= FileMetaData.getFileMetadata(getAbsolutePath(fileName+FileMetaData.METADATA_FILE_ENDING));
		File oldfile = new File(getAbsolutePath(fileName+FileMetaData.METADATA_FILE_ENDING));
		fmd.setFileName(getAbsolutePath(newName+FileMetaData.METADATA_FILE_ENDING));
		FileMetaData.StoreFileMetaDataFile(getAbsolutePath(fileName), fmd);
		oldfile.delete();
	}

	private void rm(String fileName) {
		// TODO Auto-generated method stub
		String absolutePath= getAbsolutePath(fileName+FileMetaData.METADATA_FILE_ENDING);
		File localFile = new File(absolutePath);
		if (!localFile.exists()){
			System.err.println("file not found, exiting");
			return;
		}
		P2pFile p2pf = new P2pFile(absolutePath);
		Requester requestObject = new Requester();
		requestObject.deleteFile(p2pf);

	}

	private void mvb(String localFileName) {
		// TODO Auto-generated method stub
		String absolutePath= getAbsolutePath(localFileName+FileMetaData.METADATA_FILE_ENDING);
		File localFile = new File(absolutePath);
		if (!localFile.exists()){
			System.err.println("file not found, exiting");
			return;
		}
		P2pFile p2pf = new P2pFile(absolutePath);
		Requester requestObject = new Requester();
		requestObject.fetchFile(p2pf);
		String outputLocation = System.getProperty("user.home")+File.separatorChar+"files"+File.separatorChar+localFileName;
		try { //reading the byte array and saving to a file.
			FileUtils.writeByteArrayToFile(new File(outputLocation), p2pf.getCoalescedBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void mv(String localFileName) {
		// TODO Auto-generated method stub
		String absolutePath= getAbsolutePath(localFileName);
		File localFile = new File(absolutePath);
		if (!localFile.exists()){
			System.err.println("file not found, exiting");
			return;
		}
		P2pFile p2pf = new P2pFile(absolutePath);
		ArrayList<Peer> availablePeerList = getAvailablePeerList();
		Requester requestObject = new Requester();
		requestObject.pushFile(p2pf, availablePeerList);
		localFile.delete();
		
	}

	private void mkdir(String fileName) {
		// TODO Auto-generated method stub
		String childPath = getAbsolutePath(fileName);
		File newDir=new File(childPath);
		boolean status = newDir.mkdir();
		if (status){
			currentDirectory = newDir;
		}
		else {
			System.out.println("directory creation failed:"+childPath);
		}
		System.out.println(currentDirectory.getAbsolutePath());
	}

	private void cd(String fileName) throws IOException {
		// TODO Auto-generated method stub
		if (fileName.equals("..")||fileName.equals("../")){
			File parent = currentDirectory.getParentFile();
			currentDirectory = parent;
			System.out.println(currentDirectory.getAbsolutePath());
		}
		else if (fileName.contains(""+File.separatorChar)){ //absolute path
			System.out.println("absolute paths not supported yet.");
		}
		else { //relative path
			String childPath = getAbsolutePath(fileName);
			File child = new File(childPath);
			if (child.isDirectory()){
				currentDirectory = child;
			}
			else {
				System.out.println("not a directory:"+child.getAbsolutePath());
			}
			System.out.println(currentDirectory.getAbsolutePath());
		}
		
	}

	private String getAbsolutePath(String fileName) {
		if (fileName.startsWith(File.separatorChar+"")){//already absolute
			return fileName;
		}
		else{
			return currentDirectory.getAbsolutePath()+File.separatorChar+fileName;	
		}
	}

	
	public static void main(String[] args) {
		try {
			String[] connectionInfo = new URLReader().getConnectionString().split(":");
			new RequestReceiver(requestReceiverPortNumber).start();
			Client c =new Client(connectionInfo[0],Integer.parseInt(connectionInfo[1]));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
