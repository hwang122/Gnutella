package interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

import peer.MsgData;

/**
 * Interface of Peer
 */
public interface PeerInterface extends Remote {
	
	public byte[] obtain(String filename) throws RemoteException;
	
	public void query(MsgData msg, long TTL, String filename) throws RemoteException;
	
	public void hitQuery(MsgData msg, long TTL, String filename, String PeerID) throws RemoteException;
}
