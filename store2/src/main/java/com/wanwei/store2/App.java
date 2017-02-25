package com.wanwei.store2;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.ErrorEvent;
import org.kurento.client.EventListener;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaFlowInStateChangeEvent;
import org.kurento.client.MediaPipeline;
import org.kurento.client.MediaProfileSpecType;
import org.kurento.client.MediaType;
import org.kurento.client.PausedEvent;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.RecorderEndpoint;
import org.kurento.client.RecordingEvent;
import org.kurento.client.StoppedEvent;
import org.kurento.client.VideoInfo;
import org.kurento.repository.RepositoryClient;
import org.kurento.repository.RepositoryClientProvider;
import org.kurento.repository.service.pojo.RepositoryItemRecorder;

/**
 * Hello world!
 *
 */
public class App 
{
	private KurentoClient kurento;
	private UserRegistry register;
	private RepositoryClient repositoryClient;
	private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-S");
	  //private static final String RECORDER_FILE_PATH = "file:///home/lb/sdb/HelloWorldRecorded.webm";
	//private static final String RECORDER_FILE_PATH = "file:///tmp/HelloWorldRecorded.webm" ;
	  private static final String RECORDING_EXT = ".webm";
//	  protected static final String DEFAULT_REPOSITORY_SERVER_URI = "http://localhost:7674";
	  protected static final String DEFAULT_REPOSITORY_SERVER_URI = "file:///tmp/";
		protected static final String REPOSITORY_SERVER_URI = System.getProperty("repository.uri",
		        DEFAULT_REPOSITORY_SERVER_URI);
		
	final static String DEFAULT_KMS_WS_URI = "ws://localhost:8888/kurento";
	//这个时间是表示开始接受数据后　多少秒钟开始存数据
	final String BEGIN_RECORD_TIME="6";
	//这个时间是正常情况下默认记录多长的视频
	final static String RECORD_TIME_MIN="1";
	  App(){
		  kurento= KurentoClient.create(System.getProperty("kms.url", DEFAULT_KMS_WS_URI));
		  /*repositoryClient=RepositoryClientProvider.create(System.getProperty("repository.uri",
			        DEFAULT_REPOSITORY_SERVER_URI));*/
		  repositoryClient=REPOSITORY_SERVER_URI.startsWith("file://") ? null
		            : RepositoryClientProvider.create(REPOSITORY_SERVER_URI);
		  register=new UserRegistry();
	  }
	public Boolean start(final String videourl,final String id) {
	    try {
	    	System.out.println("begin start");
	    	final User user=new User(id);
	    	register.register(user);
	    	final MediaPipeline pipeline = kurento.createMediaPipeline();
	    	user.setMediaPipeline(pipeline);
	    	final PlayerEndpoint playerEndpoint = new PlayerEndpoint.Builder(pipeline, videourl).build();
	    	user.setPlayerEndpoint(playerEndpoint);
	     
	      // Player listeners
	      playerEndpoint.addErrorListener(new EventListener<ErrorEvent>() {
	        public void onEvent(ErrorEvent event) {
	           System.out.println("ErrorListener");
	           System.out.println(event.getDescription());
	        	if(user.getRepositoryItemRecorder()!=null){
	        		String	repoid=user.getRepositoryItemRecorder().getId();
	        		Mongodb.addRemark(repoid, "play error");
	        		Mongodb.addEnd(repoid);
	        	}
	        	release(id);
	        }
	      });
	      playerEndpoint.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
	        public void onEvent(EndOfStreamEvent event) {
	        	System.out.println("EndOfStreamListenerr");
	        	if(user.getRepositoryItemRecorder()!=null){
	        		String	repoid=user.getRepositoryItemRecorder().getId();
	        		Mongodb.addRemark(repoid, "play end");
	        		Mongodb.addEnd(repoid);	
	        	}
	        	release(id);
	        }
	      });    
	      playerEndpoint.play();
	      sleepSECONDS(Long.valueOf(System.getProperty("record.begin", BEGIN_RECORD_TIME)));
	      if(register.getById(id)==null){
	    	  System.out.println("url is invail!");
	    	  return false;
	      }
	      System.out.print("play OK,begin stream");
	      RepositoryItemRecorder repoItem = null;
	      if (repositoryClient != null) {
	          try {
	            Map<String, String> metadata = Collections.emptyMap();
	            repoItem = repositoryClient.createRepositoryItem(metadata);
	          } catch (Exception e) {
	        	   System.out.println("Unable to create kurento repository items"+e.getMessage());
		            return false;
	          }
	        } else {
	          String now = df.format(new Date());
	          String filePath = REPOSITORY_SERVER_URI  + now + RECORDING_EXT;
	          repoItem = new RepositoryItemRecorder();
	          repoItem.setId(now);
	          repoItem.setUrl(filePath);
	        }
	      /*if (repositoryClient != null) {
	          try {
	            Map<String, String> metadata = Collections.emptyMap();
	            repoItem = repositoryClient.createRepositoryItem(metadata);
	          } catch (Exception e) {
	            System.out.println("Unable to create kurento repository items"+e.getMessage());
	            return false;
	          }
	        } else {
	        	System.out.print("repository is null");
	        	return false;
	        }*/
	      user.setRepositoryItemRecorder(repoItem);   
	      MediaProfileSpecType profile = getMediaProfileFromMessage();

	      RecorderEndpoint recorder = new RecorderEndpoint.Builder(pipeline, repoItem.getUrl())
	      .withMediaProfile(profile).build();
	      user.setRecorderEndpoint(recorder);
	      
	      recorder.addRecordingListener(new EventListener<RecordingEvent>() {
	      // @Override
	        public void onEvent(RecordingEvent event) {
	        	Mongodb.insert(user.getRepositoryItemRecorder().getId());
	        	System.out.println("RecordingListener");
	       }
	      });
	      recorder.addStoppedListener(new EventListener<StoppedEvent>() {
	     //   @Override
	        public void onEvent(StoppedEvent event) {
	        	String repoid=user.getRepositoryItemRecorder().getId();
	        	Mongodb.addRemark(repoid, "recode stopped");
	        	Mongodb.addEnd(repoid);
	        	System.out.println("addStopped");
	        }
	      });
	      recorder.addErrorListener(new EventListener<ErrorEvent>() {
		      //  @Override
		        public void onEvent(ErrorEvent event) {
		        	String repoid=user.getRepositoryItemRecorder().getId();
		        	Mongodb.addRemark(repoid, "recode error");
		        	release(id);
		        	System.out.println("addPaused");
		        }
		      });
	      recorder.addPausedListener(new EventListener<PausedEvent>() {
	      //  @Override
	        public void onEvent(PausedEvent event) {
	        	if(user.getRepositoryItemRecorder()!=null)
	        		Mongodb.addRemark(user.getRepositoryItemRecorder().getId(), "recode puase");
	        	System.out.println("addPaused");
	        }
	      });
	      connectAccordingToProfile(playerEndpoint, recorder, profile);
	      
	      recorder.record();
	      
	    } catch (Throwable t) {
	    	System.out.println("Start error");
	    	System.out.println(t);
	    	t.printStackTrace();
	    	return false;
	    }
	    return true;
	  }

	  private MediaProfileSpecType getMediaProfileFromMessage() {

	    MediaProfileSpecType profile;
	 /*   switch (jsonMessage.get("mode").getAsString()) {
	      case "audio-only":
	        profile = MediaProfileSpecType.WEBM_AUDIO_ONLY;
	        break;
	      case "video-only":
	        profile = MediaProfileSpecType.WEBM_VIDEO_ONLY;
	        break;
	      default:
	        profile = MediaProfileSpecType.WEBM;
	    }*/
	    profile = MediaProfileSpecType.WEBM;
	    return profile;
	  }

	  private void connectAccordingToProfile(PlayerEndpoint webRtcEndpoint, RecorderEndpoint recorder,
	      MediaProfileSpecType profile) {
	    switch (profile) {
	      case WEBM:
	        webRtcEndpoint.connect(recorder, MediaType.AUDIO);
	        webRtcEndpoint.connect(recorder, MediaType.VIDEO);
	        break;
	      case WEBM_AUDIO_ONLY:
	        webRtcEndpoint.connect(recorder, MediaType.AUDIO);
	        break;
	      case WEBM_VIDEO_ONLY:
	        webRtcEndpoint.connect(recorder, MediaType.VIDEO);
	        break;
	      default:
	        throw new UnsupportedOperationException("Unsupported profile for this tutorial: " + profile);
	    }
	  }
	public void stop(String id){
		User user=register.getById(id);
		if(user!=null){
			//user.getWebRtcEndpoint().re;
			user.stop();
		}
	}
	public void release(String id){
		User user=register.getById(id);
		if(user!=null){
			user.release();;
		}
		register.removeBySession(id);
	}
	public void destroy(){
		System.out.println( "destroy kurento" );
		register.clear();	
		kurento.destroy();
		kurento=null;
	}
	public Boolean getKurento(){
		if(null==kurento)
			return false;
		else
			return true;
	}
	public Boolean getUser(String id){
		User user=register.getById(id);
		if(null==user)
			return false;
		else
			return true;
	}
    private static void sleepSECONDS(long minutes) {
        try {
            TimeUnit.SECONDS.sleep(minutes);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

	public static void main( String[] args )
    {
        System.out.println( "store video begin!" );
        App app=new App();
        Thread myThread1 = new MyThread(app);
        myThread1.start();
       while(app.getKurento()){
    	   System.out.println( "start!" );
    	   Boolean flag=app.start("rtsp://10.19.10.155:8554/aa.mkv", "0");
    	   int sumSecond=0;
    	   final int time_minutes=Integer.valueOf(System.getProperty("record_time_minutes", RECORD_TIME_MIN));
    	   while(app.getUser("0"))
    	   {
    		   sumSecond++;
    		   sleepSECONDS(1);
    		   if(time_minutes*60==sumSecond){
    			   sumSecond=0;
    			   app.stop("0");
    			   app.release("0");
    		   }
    	   }
       }
       System.out.println( "end!" );
    }

}
class  MyThread extends Thread {
    
    private App app;
    MyThread(App app){
    	this.app=app;
    }
    @Override
    public void run() {
        try {
        	System.out.println( "waiting!" );
        	BufferedReader strin=new BufferedReader(new InputStreamReader(System.in)); 
			String str = strin.readLine();
			if("y".equals(str)){
				System.out.println( "destroy kurento" );
				this.app.destroy();
			}else
				System.out.println( "error" );
			System.out.println( str );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
    }
}
