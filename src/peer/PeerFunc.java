package peer;

import interfaces.PeerInterface;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class PeerFunc {
	/**
	 * Interface of Server
	 */
	public static PeerInterface fileService;
	
	/**
	 * Constructor, initialization
	 */
	public PeerFunc() {
	}

	/**
	 * Get local host's IP address and name as peer ID
	 */
	public void intialize(){
		try{
			//store local machines IP and name
			InetAddress addr = InetAddress.getLocalHost();
			PeerInfo.local.IP = addr.getHostAddress();
			PeerInfo.local.name = addr.getHostName();
			PeerInfo.local.ID = "rmi://" + PeerInfo.local.IP + ":" + PeerInfo.rmi.port + "/share";
			System.out.println("Local RMI address is " + PeerInfo.local.ID);
			//1 means star topology, 2 means 2d mesh topology
			@SuppressWarnings("resource")
			Scanner scan = new Scanner(System.in);
			System.out.println("Enter the topology you want to use:\n1. Star topology\n2. 2D-Mesh topology");
			PeerInfo.local.Structure = scan.nextInt();
			if(PeerInfo.local.Structure == 1)
			{
				System.out.println("Star topology is used.");
				if(PeerInfo.rmi.port == 9000)
				{
					//PeerInfo.rmi.portList.length
					for(int i = 1; i < 16; i++)
						PeerInfo.local.neighborList.add("rmi://" + PeerInfo.local.IP + ":" +
								PeerInfo.rmi.portList[i] + "/share");					
				}
				else
				{
					PeerInfo.local.neighborList.add("rmi://" + PeerInfo.local.IP + ":" +
							PeerInfo.rmi.portList[0] + "/share");
				}
			}
			else if(PeerInfo.local.Structure == 2)
			{
				System.out.println("2D-Mesh topology is used.");
				
				int temp = PeerInfo.rmi.port - 9000;
				if(temp%4 != 0)
					PeerInfo.local.neighborList.add("rmi://" + PeerInfo.local.IP + ":" +
							PeerInfo.rmi.portList[temp-1] + "/share");
				if(temp%4 != 3)
					PeerInfo.local.neighborList.add("rmi://" + PeerInfo.local.IP + ":" +
							PeerInfo.rmi.portList[temp+1] + "/share");
				if(temp-4 >= 0)
					PeerInfo.local.neighborList.add("rmi://" + PeerInfo.local.IP + ":" +
							PeerInfo.rmi.portList[temp-4] + "/share");
				if(temp+4 <= 15)
					PeerInfo.local.neighborList.add("rmi://" + PeerInfo.local.IP + ":" +
							PeerInfo.rmi.portList[temp+4] + "/share");
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Register file in local machine
	 * @param IP
	 * @param filename
	 */
	public void register(String filename){
		FileWriter writer = null;
		try {
			writer = new FileWriter("./log.txt", true);
			//add file to local file list
			PeerInfo.local.fileList.add(filename);
			//write the action to log file
			System.out.println("File " + filename + " is registered!");
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String time = df.format(new Date());
			writer.write(time + "\t\tFile " + filename 
					+ " is registered!\r\n");
			writer.close();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * remove related file information
	 * @param ID
	 * @param filename
	 */
	public void unregister(String filename){
		FileWriter writer = null;
		try{
			writer = new FileWriter("./log.txt", true);
			//write the action to log file
			System.out.println("File " + filename + " is unregistered!");
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String time = df.format(new Date());
			writer.write(time + "\t\tFile " + filename 
					+ " is unregistered!\r\n");
			writer.close();
		}catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Get search service
	 */
	public void search(MsgData msg, long TTL, String filename){
		PeerInfo.dest.destList.clear();
		try{
			//forward query to all its neighbors
			for(int i = 0; i < PeerInfo.local.neighborList.size(); i++)
			{
				try {
					fileService = (PeerInterface)Naming.lookup(PeerInfo.local.neighborList.get(i));
					fileService.query(msg, PeerInfo.local.TTL, filename);
				} catch (NotBoundException e) {
					e.printStackTrace();
				}
			}			
		}
		catch(RemoteException e){
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public boolean checkFound(String filename)
	{
		boolean found = false;
		FileWriter writer = null;
		
		if(PeerInfo.dest.destList.size() != 0)
			found = true;
		else
		{
			try {
				System.out.println("File is not found!");
				writer = new FileWriter("./log.txt", true);
				DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				String time = df.format(new Date());
				writer.write(time + "\t\tRequesting File " + filename 
						+ " is not found!\r\n");
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return found;
	}
	
	/**
	 * Get the file from server(client)
	 * using service obtain(String) from server
	 */
	public void download(String filename){
		FileWriter writer = null;
		try {
			PeerInfo.dest.destination = PeerInfo.dest.destList.get(0);
			writer = new FileWriter("./log.txt", true);
			fileService = (PeerInterface)Naming.lookup(PeerInfo.dest.destination);
			//call the method writeFile to write file
			writeFile(filename, fileService.obtain(filename));
			//write action to log file
			String downloadPath = PeerInfo.local.path.replaceAll("\\\\\\\\", "\\\\");
			downloadPath += (File.separator + filename);
			System.out.println("File has been downloaded at " + downloadPath);
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String time = df.format(new Date());
			writer.write(time + "\t\tFile " + filename 
					+ " is downloaded at " + downloadPath + "!\r\n");
			writer.close();
		} catch (MalformedURLException|RemoteException|NotBoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Since RMI don't support FileInputStream, I convert the file into byte[],
	 * thus the file must be converted from byte[] to original file
	 * @param filename
	 * @param fileContent
	 */
	public void writeFile(String filename, byte[] fileContent){
		FileOutputStream outStr = null;
		BufferedOutputStream buffer = null;
		try{
			outStr = new FileOutputStream(new File(PeerInfo.local.path + "/" + filename));
			buffer = new BufferedOutputStream(outStr);
			buffer.write(fileContent);
			buffer.flush();
			buffer.close();
		}
		catch(FileNotFoundException e){
			System.out.println("Fail to find file!");
			e.printStackTrace();
		}
		catch(IOException e){
			System.out.println("Fail to write file content!");
			e.printStackTrace();
		}
		finally{
			try{
				buffer.close();
				outStr.close();
			}
			catch(IOException e){
				e.printStackTrace();
			}
		}
	}
}
