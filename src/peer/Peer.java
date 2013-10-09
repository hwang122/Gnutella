package peer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import interfaces.PeerInterface;

public class Peer {	
	PeerInterface share;
	
	public Peer() {
		try {
			share = new PeerImpl();
			//check usable port and register the port
			for(int i = 0; i < PeerInfo.rmi.portList.length; i++){
				if(checkPort(i)){
					//register
					PeerInfo.rmi.port = PeerInfo.rmi.portList[i];	
					PeerInfo.rmi.fileSharing = "rmi://localhost:" + 
							PeerInfo.rmi.port + "/share";
					LocateRegistry.createRegistry(PeerInfo.rmi.port);
					break;
				}
				
				if(i == 16){
					System.out.println("All ports are occupied!");
					System.exit(-1);
				}
			}
			//bind
			Naming.bind(PeerInfo.rmi.fileSharing, share);
			System.out.println("File sharing service start!");
		} catch (RemoteException e) {
			e.printStackTrace();
			System.out.println("Fail to create remote object!");
		} catch (MalformedURLException e) {
			e.printStackTrace();
			System.out.println("URL error!");
		} catch (AlreadyBoundException e) {
			e.printStackTrace();
			System.out.println("Service already bound!");
		}
	}
	
	/**
	 * Used to check whether port is occupied
	 * @param i
	 * @return
	 */
	public boolean checkPort(int i){
		boolean flag = false;
		//try to bind the port, if port is occupied, an exception will throw
		try{
			Socket s = new Socket();
			s.bind(new InetSocketAddress(PeerInfo.local.IP, PeerInfo.rmi.portList[i]));
			s.close();
			flag = true;
		}
		catch(Exception e){
			flag = false;
		}
		
		return flag;
	}
	
	/**
	 * Used to change client's default directory
	 */
	public static void changeDirectory(){
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		//Claim the sharing directory
		System.out.println("\nThe default sharing directory is current directory" +
				"\nYou can enter a new directory" + 
				"\nexample: C: or C:\\Directory or ./ or ../ or /share" +
				"\nor enter nothing to use default directory\nEnter here:");
		try {
			String storePath = br.readLine();
			if(!storePath.isEmpty()){
				storePath = storePath.replaceAll("\\\\", "\\\\\\\\");
				Pattern pattern = Pattern.compile("(([a-zA-Z]:)?(\\\\\\\\\\w+)*)||(\\.){0,2}(/(\\w+)*)+");
				Matcher matcher = pattern.matcher(storePath);
						
				while(!matcher.matches()){
					System.out.println("Illegal Path! Enter again:");
					storePath = br.readLine();
					storePath = storePath.replaceAll("\\\\", "\\\\\\\\");
					matcher = pattern.matcher(storePath);
				}
						
				PeerInfo.local.path = storePath;
				//System.out.println(PeerInfo.local.path);
			} 
			else{
				System.out.println("Default directory is used!");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * standard process of using this program
	 * @param peerFunction
	 */
	public static void process1(PeerFunc peerFunction){
		
		String filename = null;
		boolean exit = false;
		long startTime, endTime;
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		Scanner scan = new Scanner(System.in);
		
		while(!exit){
			
			System.out.println("\n1 Register a file\n2 Search a file\n3 Exit");
			switch(scan.nextInt()){
				case 1:{
					System.out.println("Enter the file name:");
					try {
						filename = br.readLine();
						
						//counting running time of registry
						startTime = System.currentTimeMillis();
						peerFunction.register(filename);
						endTime = System.currentTimeMillis();
						System.out.println("Running time of registry: " 
								+ (endTime - startTime) + "ms");
						
					} catch (IOException e) {
						e.printStackTrace();
					}
					break;
				}
				case 2:{
					System.out.println("Enter the file name:");
					try {
						filename = br.readLine();
						
						//counting running time of search
						int seqNumber = PeerInfo.local.seqNumber++;
						int hop = 0;
						MsgData msg = new MsgData(PeerInfo.local.ID, seqNumber, hop);
						startTime = System.currentTimeMillis();
						peerFunction.search(msg, PeerInfo.local.TTL, filename);
						endTime = System.currentTimeMillis();
						System.out.println("Running time of search: " 
								+ (endTime - startTime) + "ms");
						
						Thread.sleep(PeerInfo.local.TTL);
						boolean found = peerFunction.checkFound(filename);
						
						if(found){
							System.out.println("\n1 Download the file\n2 Cancel and back");
							switch(scan.nextInt()){
								case 1:
									//counting running time of download
									startTime = System.currentTimeMillis();
									peerFunction.download(filename);
									//count time in the network, 100ms per hop
									endTime = System.currentTimeMillis() + 100;
									System.out.println("Running time of download: " 
											+ (endTime - startTime) + "ms");
									break;
								case 2:
									break;
								default:
									break;
							}
						}
					} catch (IOException | InterruptedException e) {
						e.printStackTrace();
					}
					break;
				}
				case 3:
					exit = true;
					break;
				default:
					break;	
			}
		}
		
		scan.close();
	}
	
	/**
	 * Compute the average response time per client search 
	 * request by measuring the response time seen by a client
	 * , there will be 1000 sequential requests.
	 */
	public static void process2(PeerFunc peerFuntion){
		long startTime1, startTime2, endTime1, endTime2;
		
		startTime1 = System.currentTimeMillis();
		for(int i = 0; i < 1000; i++){
			peerFuntion.register(i + ".txt");
		}
		endTime1 = System.currentTimeMillis();
		
		int seqNumber = PeerInfo.local.seqNumber++;
		int hop = 0;
		MsgData msg = new MsgData(PeerInfo.local.ID, seqNumber, hop);
		startTime2 = System.currentTimeMillis();
		for(int i = 0; i < 1000; i++){
			peerFuntion.search(msg, PeerInfo.local.TTL, i + ".txt");
		}
		endTime2 = System.currentTimeMillis();
		
		System.out.println("Average running time of registry: " 
				+ (endTime1 - startTime1)/1000 + "ms");
		
		System.out.println("Average running time of search: " 
				+ (endTime2 - startTime2)/1000 + "ms");
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		System.out.println("Initializing...");
		//start peer to peer service
		new Peer();
		
		//start peer function class
		PeerFunc peerFunction = new PeerFunc();
		//get host's ip and name
		peerFunction.intialize();
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		//the client may change the default directory
		changeDirectory();
		
		//monitor the file, automatically update
		new AutoUpdate(peerFunction);
		
		process1(peerFunction);
		//process2(peerFunction);
		
		try {
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("Bye!");
		System.exit(0);
	}
}
