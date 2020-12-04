package com.csc301.profilemicroservice;

import java.io.IOException;

import org.json.*;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.springframework.stereotype.Repository;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.Values;

@Repository
public class PlaylistDriverImpl implements PlaylistDriver {

	Driver driver = ProfileMicroserviceApplication.driver;
	OkHttpClient client = new OkHttpClient();

	public static void InitPlaylistDb() {
		String queryStr;

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				queryStr = "CREATE CONSTRAINT ON (nPlaylist:playlist) ASSERT exists(nPlaylist.plName)";
				trans.run(queryStr);
				trans.success();
			}
			session.close();
		}
	}
	
	private Boolean songIsValid(String songId) throws IOException {
		HttpUrl.Builder urlBuilder = HttpUrl.parse("http://localhost:3001" + "/getSongById/" + songId).newBuilder();
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
			return status.toString().equals("OK");
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	private Boolean updateFavoriteCount(String songId, String shouldDecrement) throws IOException {
		HttpUrl.Builder urlBuilder = HttpUrl.parse("http://localhost:3001" + "/updateSongFavouritesCount/" + songId).newBuilder();
		urlBuilder.addQueryParameter("shouldDecrement", shouldDecrement);
		String url = urlBuilder.build().toString();
		
		RequestBody body = RequestBody.create(null, new byte[0]);

		Request request = new Request.Builder()
				.url(url)
				.method("PUT", body)
				.build();

		Call call = client.newCall(request);
		Response responseFromSongMs = null;

		try {
			responseFromSongMs = call.execute();
			String b = responseFromSongMs.body().string();
			responseFromSongMs.close();
			JSONObject deserialized = new JSONObject(b);
	        String status = deserialized.getString("status");
			return status.toString().equals("OK");
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		}
	}

	@Override
	public DbQueryStatus likeSong(String userName, String songId) {
		try {
			Boolean songWasInPlaylist = false;
		    try (Session session = ProfileMicroserviceApplication.driver.session()){
				try (Transaction tx = session.beginTransaction()) {   	
					int userNameResult = tx.run("MATCH (n:profile {userName: $x}) RETURN n" , Values.parameters("x", userName )).list().size();
					
					if (userNameResult == 0){
						return new DbQueryStatus("User does not exist", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);   
					}

					if(!songIsValid(songId)) {
						return new DbQueryStatus("Song does not exist", DbQueryExecResult.QUERY_ERROR_NOT_FOUND); 
					}
					
					int songResult = tx.run("MATCH (s:song {songId: $x}) RETURN s", Values.parameters("x", songId)).list().size();
					
					if(songResult == 0) {
						tx.run("MERGE (s:song {songId: $x})", Values.parameters("x", songId));
					} else {
						int songInPlaylistResult = tx.run("MATCH (:profile {userName: $x})-[:created]->(:playlist)-[r:includes]->(:song {songId: $y}) RETURN r", Values.parameters("x", userName, "y", songId)).list().size();
						System.out.print(songInPlaylistResult + "\n");
						if(songInPlaylistResult != 0) songWasInPlaylist = true; 
					}
					
					tx.run("MATCH (u:profile {userName: $x})-[:created]->(p:playlist)\n"
							+ "MATCH (s:song {songId: $z})\n" 
							+ "MERGE (p)-[:includes]->(s)", Values.parameters("x", userName, "z", songId));	
					tx.success();
					session.close();
				}
		    }
		    
		    if(!songWasInPlaylist) {
		    	if(!updateFavoriteCount(songId, "false")) {
		    		return new DbQueryStatus("Couldn't update favorite count for this song", DbQueryExecResult.QUERY_ERROR_GENERIC);
		    	}
		    }
		    
		    return new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);

		} catch(Exception e) {
			e.printStackTrace();
		    return new DbQueryStatus("Not OK", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
	}

	@Override
	public DbQueryStatus unlikeSong(String userName, String songId) {
		try {
		    try (Session session = ProfileMicroserviceApplication.driver.session()){
				try (Transaction tx = session.beginTransaction()) {   	
					int userNameResult = tx.run("MATCH (n:profile {userName: $x}) RETURN n" , Values.parameters("x", userName )).list().size();
					
					if (userNameResult == 0){
						return new DbQueryStatus("User does not exist", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);   
					}

					if(!songIsValid(songId)) {
						return new DbQueryStatus("Song does not exist", DbQueryExecResult.QUERY_ERROR_NOT_FOUND); 
					}
					
					int songInPlaylistResult = tx.run("MATCH (:profile {userName: $x})-[:created]->(:playlist)-[r:includes]->(:song {songId: $y}) RETURN r", Values.parameters("x", userName, "y", songId)).list().size();
					
					if(songInPlaylistResult == 0) {
						return new DbQueryStatus("Song is not in this users playlist", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
					}
					
					tx.run("MATCH (:profile {userName: $x})-[:created]->(:playlist)-[r:includes]->(:song {songId: $y})\n"
							+ "DELETE r", Values.parameters("x", userName, "y", songId));	
					tx.success();
					session.close();
				}
		    }
		    if(updateFavoriteCount(songId, "true")) {
		    	return new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
		    } else {
		    	return new DbQueryStatus("Couldn't update favorite count for this song", DbQueryExecResult.QUERY_ERROR_GENERIC);
		    }
		} catch(Exception e) {
			e.printStackTrace();
		    return new DbQueryStatus("Not OK", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
	}

	@Override
	public DbQueryStatus deleteSongFromDb(String songId) {
		
		return null;
	}
}
