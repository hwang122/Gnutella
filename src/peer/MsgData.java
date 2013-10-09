package peer;

import java.io.Serializable;

public class MsgData implements Serializable{

	private static final long serialVersionUID = -546870466725417687L;
	private String PeerID;
	private int seqNumber;
	private int hop;
	
	public MsgData(String PeerID, int seqNumber, int hop) {
		setPeerID(PeerID);
		setSeqNumber(seqNumber);
		setHop(hop);
	}

	public String getPeerID() {
		return PeerID;
	}

	public void setPeerID(String peerID) {
		PeerID = peerID;
	}

	public int getSeqNumber() {
		return seqNumber;
	}

	public void setSeqNumber(int seqNumber) {
		this.seqNumber = seqNumber;
	}

	public int getHop() {
		return hop;
	}

	public void setHop(int hop) {
		this.hop = hop;
	}
}
