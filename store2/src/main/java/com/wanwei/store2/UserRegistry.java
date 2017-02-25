package com.wanwei.store2;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class UserRegistry {
	private ConcurrentHashMap<String, User> usersBySessionId = new ConcurrentHashMap<String, User>();

	  public void register(User user) {
	    usersBySessionId.put(user.getId(), user);
	  }

	  public User getById(String id) {
	    return usersBySessionId.get(id);
	  }

	  public User getBySession(String id) {
	    return usersBySessionId.get(id);
	  }

	  public boolean exists(String id) {
	    return usersBySessionId.keySet().contains(id);
	  }

	  public User removeBySession(String id) {
	    final User user = getBySession(id);
	    if (user != null) {
	      usersBySessionId.remove(user.getId());
	    }
	    return user;
	  }
	  public void clear(){
		  usersBySessionId.entrySet();
		  for(Entry<String, User> e:usersBySessionId.entrySet()){
			  e.getValue().release();
			  //usersBySessionId.remove(e.getKey());
		  }
		  usersBySessionId.clear();
	  }
}
