package com.wanwei.store2;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.kurento.client.EventListener;
import org.kurento.client.IceCandidate;
import org.kurento.client.ListenerSubscription;
import org.kurento.client.MediaPipeline;
import org.kurento.client.RecorderEndpoint;
import org.kurento.client.StoppedEvent;
import org.kurento.repository.service.pojo.RepositoryItemRecorder;
import org.kurento.client.PlayerEndpoint;
public class User {
	private String id;
	  private PlayerEndpoint playerEndpoint;
	  private RecorderEndpoint recorderEndpoint;
	  private MediaPipeline mediaPipeline;
	  private RepositoryItemRecorder repo;
	  private Date stopTimestamp;

	  public User(String session) {
	    this.id = session;
	    this.recorderEndpoint=null;
	    this.playerEndpoint=null;
	    this.mediaPipeline=null;
	  }

	  public String getId() {
	    return id;
	  }

	  public void setRepositoryItemRecorder(RepositoryItemRecorder repo) {
	    this.repo = repo;
	  }
	  public RepositoryItemRecorder getRepositoryItemRecorder() {
		    return repo;
		  }

		  public void setId(String id) {
		    this.id = id;
		  }
	  public PlayerEndpoint getWebRtcEndpoint() {
	    return playerEndpoint;
	  }

	  public void setPlayerEndpoint(PlayerEndpoint playerEndpoint) {
	    this.playerEndpoint = playerEndpoint;
	  }

	  public void setRecorderEndpoint(RecorderEndpoint recorderEndpoint) {
	    this.recorderEndpoint = recorderEndpoint;
	  }

	  public MediaPipeline getMediaPipeline() {
	    return mediaPipeline;
	  }

	  public void setMediaPipeline(MediaPipeline mediaPipeline) {
	    this.mediaPipeline = mediaPipeline;
	  }


	  public Date getStopTimestamp() {
	    return stopTimestamp;
	  }

	  public void stop() {
	    if (recorderEndpoint != null) {
	      final CountDownLatch stoppedCountDown = new CountDownLatch(1);
	      ListenerSubscription subscriptionId = recorderEndpoint
	          .addStoppedListener(new EventListener<StoppedEvent>() {
	            //@Override
	            public void onEvent(StoppedEvent event) {
	              stoppedCountDown.countDown();
	            }
	          });
	      recorderEndpoint.stop();
	      try {
	        if (!stoppedCountDown.await(5, TimeUnit.SECONDS)) {
	          //log.error("Error waiting for recorder to stop");
	        	System.out.println("Error waiting for recorder to stop");
	        }
	      } catch (InterruptedException e) {
	        //log.error("Exception while waiting for state change", e);
	    	System.out.println("Exception while waiting for state change");
	      }
	      recorderEndpoint.removeStoppedListener(subscriptionId);
	    }
	    if(null!=this.playerEndpoint){
	    	this.playerEndpoint.stop();
	    }
	  }

	  public void release() {
		if(null!=this.recorderEndpoint){
			    this.recorderEndpoint.release();
			    this.recorderEndpoint=null;
		}
		if(null!=this.playerEndpoint){
			this.playerEndpoint.release();
	    	this.playerEndpoint = null;
		}
		if(null!=this.mediaPipeline){
			this.mediaPipeline.release();
	    	this.mediaPipeline = null;
		}
	    if (this.stopTimestamp == null) {
	      this.stopTimestamp = new Date();
	    }
	  }
}
