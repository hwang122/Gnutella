package peer;

import interfaces.PeerInterface;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Implementation of File sharing within peers
 */
public class PeerImpl extends UnicastRemoteObject implements PeerInterface {

	private static final long serialVersionUID = -120810884040066791L;
	
	public static PeerInterface fileService;

	protected PeerImpl() throws RemoteException {
	}
	
	/**
	 * obtain(String) method
	 * Since RMI don't support FileInputStream, I convert the file into
	 * byte[] array and return this array to client
	 * And to make thread safe while there are multiple clients using obtain
	 * method, I create thread pool and using Callable to improve the performance
	 */
	@Override
	public byte[] obtain(String filename) throws RemoteException {
		String filePath = PeerInfo.local.path + "/" + filename;
		byte[] byteFile = null;
		//create a thread pool
		ExecutorService execPool = Executors.newCachedThreadPool();
		
		Callable<byte[]> call = new ShareCall(filePath);
		Future<byte[]> result = execPool.submit(call);
		try {
			int length = result.get().length;
			byteFile = new byte[length];
			System.arraycopy(result.get(), 0, byteFile, 0, length);
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		finally{
			execPool.shutdown();
		}
		
		return byteFile;
	}
	
	/**
	 * implements Callabe interface
	 * return byte[]
	 */
	class ShareCall implements Callable<byte[]>{
		private String filename = null;
		
		public ShareCall(String filename) {
			setFilename(filename);
		}

		@Override
		public byte[] call() throws Exception {
			//use fileToByte method to convert file to byte[]
			return fileToByte(getFilename());
		}

		public String getFilename() {
			return filename;
		}

		public void setFilename(String filename) {
			this.filename = filename;
		}
		
	}
	
	/**
	 * Key method, used to convert file to byte[]
	 * Since RMI don't support FileInputStream, this is one way
	 * to transfer the file
	 * @param filename
	 * @return
	 */
	public byte[] fileToByte(String filename){
		byte[] byteFile = null;
		FileInputStream inputStream = null;
		BufferedInputStream bufferStream = null;
		try{
			File file = new File(filename);
			byteFile = new byte[(int)file.length()];
			inputStream = new FileInputStream(file);
			bufferStream = new BufferedInputStream(inputStream);
			bufferStream.read(byteFile);
			
			bufferStream.close();
		}
		catch(FileNotFoundException e){
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally{
			try {
				bufferStream.close();
				inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}		
		}
		return byteFile;
	}
	
	/**
	 * Search the filename in local file list and forward it
	 * to all its neighbors
	 */
	@Override
	public void query(MsgData msg, long TTL, String filename)
			throws RemoteException {
		//count time in the network, 100ms per hop, TTL-100
		queryRun run = new queryRun(msg, TTL - 100, filename);
		Thread thread = new Thread(run);
		thread.start();
		thread= null;
	}
	
	class queryRun implements Runnable
	{
		private MsgData msg;
		private long TTL;
		private String filename;

		public queryRun(MsgData msg, long TTL, String filename) {
			setMsg(msg);
			setTTL(TTL);
			setFilename(filename);
		}

		@Override
		public void run() {
			boolean flag = true;
			//check whether this message has been handled
			for(int i = 0; i < PeerInfo.local.queryList.size(); i++)
			{
				if(getMsg().getPeerID().equals(PeerInfo.local.queryList.get(i).getPeerID())
				&&getMsg().getSeqNumber() == PeerInfo.local.queryList.get(i).getSeqNumber())
					flag = false;
			}
			
			if(getTTL() > 0 && flag && getMsg().getPeerID() != PeerInfo.local.ID)
			{
				String rmiAddress = null;
				long start = System.currentTimeMillis();
				int hop = getMsg().getHop() + 1;
				getMsg().setHop(hop);
				
				//remove old query
				if(PeerInfo.local.queryList.size() > 10)
					PeerInfo.local.queryList.remove(0);
				//add this query to local query list
				PeerInfo.local.queryList.add(getMsg());
				
				for(int i = 0; i < PeerInfo.local.fileList.size(); i++)
				{
					//if file found, return the rmi address
					if(PeerInfo.local.fileList.get(i).equals(getFilename()))
					{
						rmiAddress = PeerInfo.local.ID;
						break;
					}
				}
				
				long end = System.currentTimeMillis();
				long duration = end - start;
				if(rmiAddress != null)
				{
					try {
						fileService = (PeerInterface)Naming.lookup(getMsg().getPeerID());
						fileService.hitQuery(getMsg(), getTTL()-duration, getFilename(), rmiAddress);
					} catch (MalformedURLException | NotBoundException | RemoteException e) {
						e.printStackTrace();
					}
					
				}
				
				//forward this search to all its neighbors
				for(int i = 0; i < PeerInfo.local.neighborList.size(); i++)
				{
					try {
						fileService = (PeerInterface)Naming.lookup(PeerInfo.local.neighborList.get(i));
						fileService.query(getMsg(), getTTL()-duration, getFilename());
					} catch (MalformedURLException | NotBoundException | RemoteException e) {
						e.printStackTrace();
					}
				}
			}
		}

		public MsgData getMsg() {
			return msg;
		}

		public void setMsg(MsgData msg) {
			this.msg = msg;
		}

		public long getTTL() {
			return TTL;
		}

		public void setTTL(long tTL) {
			TTL = tTL;
		}

		public String getFilename() {
			return filename;
		}

		public void setFilename(String filename) {
			this.filename = filename;
		}
		
	}

	@Override
	public void hitQuery(MsgData msg, long TTL, String filename, String PeerID)
			throws RemoteException {
		//count time in the network, 100ms per hop, TTL-100
		hitqueryRun run = new hitqueryRun(msg, TTL, filename, PeerID);
		Thread thread = new Thread(run);
		thread.start();
		thread = null;
	}
	
	class hitqueryRun implements Runnable
	{
		private MsgData msg;
		private long TTL;
		private String filename;
		private String PeerID;
		
		public hitqueryRun(MsgData msg, long TTL, String filename, String PeerID) {
			setMsg(msg);
			//The hit query need another hop to go back
			setTTL(TTL - msg.getHop()*100);
			setFilename(filename);
			setPeerID(PeerID);
		}

		@Override
		public void run() {
			FileWriter writer = null;
			try{
				writer = new FileWriter("./log.txt", true);
				
				PeerInfo.dest.destList.add(getPeerID());
				long duration = PeerInfo.local.TTL - getTTL();
				System.out.println("From " + getPeerID() + ", find file " + getFilename()
									+ ", using time " + duration + " ms");
				
				DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				String time = df.format(new Date());
				writer.write(time + "\t\tRequesting File " + getFilename() 
						+ " is found! Using Time " + duration +" ms\r\n");
				writer.close();
			} 
			catch (IOException e) {
				e.printStackTrace();
			}
		}

		public MsgData getMsg() {
			return msg;
		}

		public void setMsg(MsgData msg) {
			this.msg = msg;
		}

		public String getPeerID() {
			return PeerID;
		}

		public void setPeerID(String peerID) {
			PeerID = peerID;
		}

		public long getTTL() {
			return TTL;
		}

		public void setTTL(long tTL) {
			TTL = tTL;
		}

		public String getFilename() {
			return filename;
		}

		public void setFilename(String filename) {
			this.filename = filename;
		}
	}
}
