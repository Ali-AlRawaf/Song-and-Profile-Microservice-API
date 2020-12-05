package com.csc301.profilemicroservice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.springframework.stereotype.Repository;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.Values;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Record;

@Repository
public class ProfileDriverImpl implements ProfileDriver {

	Driver driver = ProfileMicroserviceApplication.driver;
	OkHttpClient client = new OkHttpClient();

	/**
	* Initlaize Profile Db
	* @return  void
	*/
	public static void InitProfileDb() {
		String queryStr;

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT exists(nProfile.userName)";
				trans.run(queryStr);

				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT exists(nProfile.password)";
				trans.run(queryStr);

				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT nProfile.userName IS UNIQUE";
				trans.run(queryStr);

				trans.success();
			}
			session.close();
		}
	}
	
	/**
	* Add user to db and return status
	* @param  userName  a string of the desired username
	* @param  fullName  a string of the desired fullname
	* @param  password  a string of the desired password
	* @return  DbQueryStatus with OK status if operation is success, and not OK otherwise
	*/
	@Override
	public DbQueryStatus createUserProfile(String userName, String fullName, String password) {
		try {
		     try (Session session = ProfileMicroserviceApplication.driver.session()){
		            try (Transaction tx = session.beginTransaction()) {   	
		            	int userNameResult = tx.run("MATCH (n:profile {userName: $x}) RETURN n" , Values.parameters("x", userName )).list().size();
		            	if (userNameResult == 0){
			                tx.run("MERGE (a:profile {userName: $x, fullName: $y, password: $z})", Values.parameters("x", userName, "y", fullName, "z", password));
			                tx.run("MERGE (a:playlist {plName: $x})", Values.parameters("x", userName +"-favorites"));
		                    tx.run("MATCH (a:profile {userName: $x}),(p:playlist {plName: $y})\n" +  "MERGE (a)-[:created]->(p)", Values.parameters("x", userName, "y", userName +"-favorites"));	
		            		tx.success();
		                    session.close();   
		                } else {
		                	return new DbQueryStatus("UserName already exist", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
		                } 	
		            }
		        }
	     	return new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
		} catch(Exception e){
		     return new DbQueryStatus("Not OK", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}	
	}
	
	/**
	* Create a :follows relation between the given users and return status
	* @param  userName  a string of the desired username (the user following)
	* @param  frndUserName  a string of the desired frndUserName (the user being followed)
	* @return  DbQueryStatus with OK status if operation is success, and not OK otherwise
	*/
	@Override
	public DbQueryStatus followFriend(String userName, String frndUserName) {	
		try {
		     try (Session session = ProfileMicroserviceApplication.driver.session()){
		            try (Transaction tx = session.beginTransaction()) {   	
		            	int userNameResult = tx.run("MATCH (n:profile {userName: $x}) RETURN n" , Values.parameters("x", userName )).list().size();
		            	int frndUserNameResult = tx.run("MATCH (n:profile {userName: $x}) RETURN n" , Values.parameters("x", frndUserName )).list().size();

		            	if (userNameResult == 0 || frndUserNameResult == 0){
		                	return new DbQueryStatus("One of the users does not exist", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);   
		                } else {
		                    tx.run("MATCH (a:profile {userName: $x}),(b:profile {userName: $y})\n" +  "MERGE (a)-[:follows]->(b)", Values.parameters("x", userName, "y", frndUserName));	
		            		tx.success();
		                    session.close();	       
		                 } 	
		            }
		        }
		     return new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
		} catch(Exception e) {
		     return new DbQueryStatus("Not OK", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}	
	}

	/**
	* Delete a :follows relation between the given users and return status
	* @param  userName  a string of the desired username (the user following)
	* @param  frndUserName  a string of the desired frndUserName (the user being followed)
	* @return  DbQueryStatus with OK status if operation is success, and not OK otherwise
	*/
	@Override
	public DbQueryStatus unfollowFriend(String userName, String frndUserName) {	
		try {
		     try (Session session = ProfileMicroserviceApplication.driver.session()){
		            try (Transaction tx = session.beginTransaction()) {   	
		            	int userNameResult = tx.run("MATCH (n:profile {userName: $x}) RETURN n" , Values.parameters("x", userName )).list().size();
		            	int frndUserNameResult = tx.run("MATCH (n:profile {userName: $x}) RETURN n" , Values.parameters("x", frndUserName )).list().size();
		            	int hasRelationshipResult = tx.run("MATCH (a:profile {userName: $x})-[r:follows]->(b:profile {userName: $y}) RETURN r", Values.parameters("x", userName, "y", frndUserName)).list().size();

		            	if (userNameResult == 0 || frndUserNameResult == 0 || hasRelationshipResult == 0){
		                	return new DbQueryStatus("One of the users does not exist, or userName isnt following frndUserName", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);   
		                } else {
		                    tx.run("MATCH (a:profile {userName: $x})-[r:follows]->(b:profile {userName: $y}) DELETE r", Values.parameters("x", userName, "y", frndUserName));	
		            		tx.success();
		                    session.close();	       
		                 } 	
		            }
		        }
		     return new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
		} catch(Exception e) {
		     return new DbQueryStatus("Not OK", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
	}
	
	private String getSongTitle(String songId) throws Exception {
		HttpUrl.Builder urlBuilder = HttpUrl.parse("http://localhost:3001" + "/getSongTitleById/" + songId).newBuilder();
		String url = urlBuilder.build().toString();

		Request request = new Request.Builder()
				.url(url)
				.method("GET", null)
				.build();

		Call call = client.newCall(request);
		Response responseFromSongMs = null;

		try {
			responseFromSongMs = call.execute();
			String body = responseFromSongMs.body().string();
			responseFromSongMs.close();
			JSONObject deserialized = new JSONObject(body);
	        String status = deserialized.getString("status");
			if(status.toString().equals("OK"))
				return deserialized.getString("data");
			throw new Exception();
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		}
	}

	/**
	* Returns a list of all the songs that every friend of username likes inside dbquerydata
	* @param  userName  a string of the desired username (the user following)
	* @return  DbQueryStatus with Not OK status if there is no profile with userName, otherwise OK status as long as operation succeeds
	*/
	@Override
	public DbQueryStatus getAllSongFriendsLike(String userName) {
		try {
			try (Session session = ProfileMicroserviceApplication.driver.session()){
				try (Transaction tx = session.beginTransaction()) {   	
					int userNameResult = tx.run("MATCH (n:profile {userName: $x}) RETURN n" , Values.parameters("x", userName )).list().size();

					if (userNameResult == 0){
						return new DbQueryStatus("User does not exist", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);   
					}
						
					StatementResult songsResult = tx.run("MATCH (:profile {userName: $x})-[:follows]->(p:profile)-[:created]->(:playlist)-[:includes]->(s:song) RETURN p.userName,s.songId", Values.parameters("x", userName));
					
					HashMap<String, ArrayList<String>> data = new HashMap<String, ArrayList<String>>();
					if(songsResult.hasNext()) {
						List<Record> songsList = songsResult.list(); 
						for(Record rec : songsList) {
							String user = rec.get("p.userName").asString();
							String song = rec.get("s.songId").asString();
							
							String songTitle = "";
							try {
								songTitle = getSongTitle(song);
							} catch (Exception e) {
								return new DbQueryStatus("Song does not exist", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
							}
							
							ArrayList<String> arr = new ArrayList<String>();
							
							if(data.containsKey(user)) arr = data.get(user);
							arr.add(songTitle);
							data.put(user, arr);
						}
					}
					DbQueryStatus success = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
					success.setData(data);
					tx.success();
					session.close();
					return success;
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		    return new DbQueryStatus("Not OK", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
	}
}
