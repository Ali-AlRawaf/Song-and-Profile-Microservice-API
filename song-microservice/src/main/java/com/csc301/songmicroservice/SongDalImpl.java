package com.csc301.songmicroservice;

import org.bson.Document;
import org.bson.types.ObjectId;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class SongDalImpl implements SongDal {

	private final MongoTemplate db;

	@Autowired
	public SongDalImpl(MongoTemplate mongoTemplate) {
		this.db = mongoTemplate;
	}

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

	@Override
	public DbQueryStatus getSongTitleById(String songId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DbQueryStatus deleteSongById(String songId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DbQueryStatus updateSongFavouritesCount(String songId, boolean shouldDecrement) {
		// TODO Auto-generated method stub
		return null;
	}
}