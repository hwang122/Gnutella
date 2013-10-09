package peer;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AutoUpdate {
	private ScheduledExecutorService executorService;
	private PeerFunc peerFunction;
	
	public AutoUpdate(PeerFunc peerFunction) {
		this.setPeerFunction(peerFunction);
		//create thread pool for the thread
		this.executorService = Executors.newScheduledThreadPool(1);
		//set fixed delay, this thread would execute each 10ms
		this.executorService.scheduleWithFixedDelay(new Runnable(){

			@Override
			public void run() {
				for(int i = 0; i < PeerInfo.local.fileList.size(); i++){
					//check each file in the file list
					File f = new File(PeerInfo.local.path + "\\" 
							+ PeerInfo.local.fileList.get(i));
					if(!f.exists()){
						System.out.println("File" + PeerInfo.local.fileList.get(i) + " is not exited.");
						getPeerFunction().unregister(PeerInfo.local.fileList.get(i));
						PeerInfo.local.fileList.remove(i);
					}
				}
			}
		}, 1000, 10, TimeUnit.MILLISECONDS);
	}

	public PeerFunc getPeerFunction() {
		return peerFunction;
	}

	public void setPeerFunction(PeerFunc peerFunction) {
		this.peerFunction = peerFunction;
	}

}
