package com.csc301.songmicroservice;

import java.io.IOException;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Repository
public class SongDalImpl implements SongDal {

	private final MongoTemplate db;
	
	private OkHttpClient client = new OkHttpClient();

	@Autowired
	public SongDalImpl(MongoTemplate mongoTemplate) {
		this.db = mongoTemplate;
	}
	
	/**
	* Creates and inserts a song document into the db, returning the document that was inserted
	* @param  songToAdd  the Song object containing the song data to insert into the document
	* @return  DbQueryStatus with song document, server response message and status of operation
	*/
	@Override
	public DbQueryStatus addSong(Song songToAdd) {
		try {
			Document song = new Document();
			song.put(Song.KEY_SONG_NAME, songToAdd.getSongName());
			song.put(Song.KEY_SONG_ALBUM, songToAdd.getSongAlbum());
			song.put(Song.KEY_SONG_ARTIST_FULL_NAME, songToAdd.getSongArtistFullName());
			song.put("songAmountFavourites", songToAdd.getSongAmountFavourites());
			db.getCollection("songs").insertOne(song);
			ObjectId songID = song.getObjectId("_id");

			DbQueryStatus sucuess = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
			songToAdd.setId(songID);
			sucuess.setData(songToAdd);
	     	return sucuess;
		} catch(Exception e){
		     return new DbQueryStatus("Not OK", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
	}
	
	/**
	* Finds and returns song with id songId if it exists, otherwise responds with status not found
	* @param  songId  the song id of the wanted song
	* @return  DbQueryStatus with song document, server response message and status of operation
	*/
	@Override
	public DbQueryStatus findSongById(String songId) {
		try {	     	
    		Document queryDoc = new Document();
    		queryDoc.put("_id", new ObjectId(songId));
			Document resDoc = (Document) db.getCollection("songs").find(queryDoc).first();
			
			if(resDoc == null) {
            	return new DbQueryStatus("No user with such id exist", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);   
			}

			DbQueryStatus sucuess = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
			sucuess.setData(resDoc);

	     	return sucuess;

		} catch(Exception e){
		     return new DbQueryStatus("Not OK", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
	}
	
	/**
	* Finds and returns song title of song with id songId if it exists, otherwise responds with 
	* status not found
	* @param  songId  the song id of the song with the wanted song title
	* @return  DbQueryStatus with song title, server response message and status of operation
	*/
	@Override
	public DbQueryStatus getSongTitleById(String songId) {
		try {	     	
    		Document queryDoc = new Document();
    		queryDoc.put("_id", new ObjectId(songId));
			Document resDoc = (Document) db.getCollection("songs").find(queryDoc).first();
			
			if(resDoc == null) {
            	return new DbQueryStatus("No user with such id exist", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);   
			}

			DbQueryStatus sucuess = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
			sucuess.setData(resDoc.get(Song.KEY_SONG_NAME));

	     	return sucuess;

		} catch(Exception e){
		     return new DbQueryStatus("Not OK", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
	}
	
	/**
	* Finds and deletes song with id songId if it exists, otherwise responds with status not found
	* @param  songId  the song id of the song to be deleted
	* @return  DbQueryStatus with server response message and status of operation
	*/
	@Override
	public DbQueryStatus deleteSongById(String songId) {
		try {	     	
    		Document queryDoc = new Document();
    		queryDoc.put("_id", new ObjectId(songId));
			Document resDoc = (Document) db.getCollection("songs").findOneAndDelete(queryDoc);
			
			if(resDoc == null) {
            	return new DbQueryStatus("No song with such id exist", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);   
			}
			
			HttpUrl.Builder urlBuilder = HttpUrl.parse("http://localhost:3002" + "/deleteAllSongsFromDb/" + songId).newBuilder();
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
				if(status.toString().equals("OK")) {
					return new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
				}
				return new DbQueryStatus(status.toString(), DbQueryExecResult.QUERY_ERROR_GENERIC);
			} catch (IOException e) {
				e.printStackTrace();
				throw e;
			}
		} catch(Exception e){
			 e.printStackTrace();
		     return new DbQueryStatus("Not OK", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
	}

	/**
	* Increments or decrements the favorite count of song with id songId
	* @param  songId  id of the song whos favorite count to be updated
	* @param  shouldDecrement  Boolean specifying whether to increment or decrement
	* @return  DbQueryStatus containing server response message and status of operation
	*/
	@Override
	public DbQueryStatus updateSongFavouritesCount(String songId, boolean shouldDecrement) {
		try {	     	
    		Document queryDoc = new Document();
    		queryDoc.put("_id", new ObjectId(songId));
			Document resDoc = (Document) db.getCollection("songs").find(queryDoc).first();
			
			if(resDoc == null) {
            	return new DbQueryStatus("No user with such id exist", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);   
			}
			
			long original = (long) resDoc.get("songAmountFavourites");
			
			if ((long) resDoc.get("songAmountFavourites") == 0 && shouldDecrement) {
            	return new DbQueryStatus("Cant make less than zero", DbQueryExecResult.QUERY_ERROR_GENERIC);   
			}
			
			long newVal;
			if(shouldDecrement) {
				newVal = original-1;
			} else {
				newVal = original+1;
			}
			
    		Document newDoc = new Document();
    		newDoc.put("_id", new ObjectId(songId));

    		newDoc.put(Song.KEY_SONG_NAME, resDoc.get(Song.KEY_SONG_NAME));
    		newDoc.put(Song.KEY_SONG_ALBUM, resDoc.get(Song.KEY_SONG_ALBUM));
    		newDoc.put(Song.KEY_SONG_ARTIST_FULL_NAME, resDoc.get(Song.KEY_SONG_ARTIST_FULL_NAME));
    	
    		newDoc.put("songAmountFavourites", newVal);

			db.getCollection("songs").replaceOne(queryDoc, newDoc);

	     	return new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
		} catch(Exception e){
		     return new DbQueryStatus("Not OK", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
	}
}