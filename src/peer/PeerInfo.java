package peer;

import java.util.ArrayList;

public class PeerInfo {
	
	/**
	 * Information about rmi
	 */
	public static final class rmi{
		public static final int[] portList = {9000, 9001, 9002, 9003, 9004, 9005, 9006,
												9007, 9008, 9009, 9010, 9011, 9012, 9013,
												9014, 9015};
		public static int port = 0;
		public static String fileSharing = "";
	}
	
	/**
	 * Local information
	 */
	public static class local{
		public static String IP = "";
		public static String name = "";
		public static String ID = "";
		public static String path = ".";
		public static ArrayList<String> fileList = new ArrayList<String>();
		public static int seqNumber = 0;
		public static final long TTL = 2000L;
		//1 means star topology; 2 means 2D mesh topology
		public static int Structure = 1;
		public static ArrayList<String> neighborList = new ArrayList<String>();
		//keep a list of query message
		public static ArrayList<MsgData> queryList = new ArrayList<MsgData>();
	}
	
	/**
	 * Neighbor(Peer) information
	 */
	public static class dest{
		public static ArrayList<String> destList = new ArrayList<String>();
		public static String destination = "";
		public static String destRmi = "";
		public static String destPath = "";
	}
}
