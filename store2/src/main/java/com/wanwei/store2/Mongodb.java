package com.wanwei.store2;

import java.util.Date;
import org.bson.Document;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
public class Mongodb {

	private final static String MONGO_HOST_IP="localhost";
	private final static String MONGO_HOST_PORT="27017";
	private final static String MONGO_DB_NAME="testdb";
	private final static String MONGO_DB_COLLECTION="user";
	protected static final String DEFAULT_ROBOT_ID = "0";
	protected static final String DEFAULT_CAMERA_IP = "0.0.0.0";
	protected static final String ROBOT_ID = System.getProperty("robot.id",
			DEFAULT_ROBOT_ID);
	protected static final String CAMERA_IP = System.getProperty("camera.ip",
			DEFAULT_CAMERA_IP);
	public static void insert(String id){
		try{

		MongoClient mongo = new MongoClient(System.getProperty("mongo.ip",
				MONGO_HOST_IP), Integer.valueOf(System.getProperty("mongo.port",
				MONGO_HOST_PORT)));

		MongoDatabase db = mongo.getDatabase(System.getProperty("mongo.dbname",MONGO_DB_NAME));
		MongoCollection<Document> table = db.getCollection(System.getProperty("mongo.dbCollection",MONGO_DB_COLLECTION));
		
		/**** Insert ****/
		// create a document to store key and value
		Document document = new Document();
		document.put("robotId", ROBOT_ID);
		document.put("cameraIp", CAMERA_IP);
		document.put("id", id);
		document.put("startTime", new Date());
		document.put("endTime", "0");
		document.put("remark", "begin insert");
		table.insertOne(document);

		mongo.close();
	    } catch (MongoException e) {
	    	e.printStackTrace();
	    }

	  }
	public static void addEnd(String id){
		try{
		MongoClient mongo = new MongoClient(System.getProperty("mongo.ip",
				MONGO_HOST_IP), Integer.valueOf(System.getProperty("mongo.port",
				MONGO_HOST_PORT)));
		MongoDatabase db = mongo.getDatabase(System.getProperty("mongo.dbname",MONGO_DB_NAME));
		MongoCollection<Document> table = db.getCollection(System.getProperty("mongo.dbCollection",MONGO_DB_COLLECTION));
		table.updateOne(Filters.eq("id",id), new Document("$set",new Document("endTime",new Date())));
		mongo.close();
	    } catch (MongoException e) {
	    	e.printStackTrace();
	    }
	  }
	public static void addRemark(String id,String remark){
		try{
		MongoClient mongo = new MongoClient(System.getProperty("mongo.ip",
				MONGO_HOST_IP), Integer.valueOf(System.getProperty("mongo.port",
				MONGO_HOST_PORT)));
		MongoDatabase db = mongo.getDatabase(System.getProperty("mongo.dbname",MONGO_DB_NAME));
		MongoCollection<Document> table = db.getCollection(System.getProperty("mongo.dbCollection",MONGO_DB_COLLECTION));
		table.updateOne(Filters.eq("id",id), new Document("$set",new Document("remark",remark)));
		mongo.close();
	    } catch (MongoException e) {
	    	e.printStackTrace();
	    }
	}
}
