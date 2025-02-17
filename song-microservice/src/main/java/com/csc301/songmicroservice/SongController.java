package com.csc301.songmicroservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/")
public class SongController {

	@Autowired
	private final SongDal songDal;

	public SongController(SongDal songDal) {
		this.songDal = songDal;
	}

	/**
	* Return song document of song with id songId
	* @param  songId  id of the wanted song
	* @param  request  http API request
	* @return  DbQueryStatus containing the song, server response message and status of operation
	*/
	@RequestMapping(value = "/getSongById/{songId}", method = RequestMethod.GET)
	public @ResponseBody Map<String, Object> getSongById(@PathVariable("songId") String songId,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("GET %s", Utils.getUrl(request)));

		DbQueryStatus dbQueryStatus = songDal.findSongById(songId);

		response.put("message", dbQueryStatus.getMessage());
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());

		return response;
	}

	/**
	* Return song title of song with id songId
	* @param  songId  id of the song with wanted song title
	* @param  request  http API request
	* @return  DbQueryStatus containing the song title, server response message and status of operation
	*/
	@RequestMapping(value = "/getSongTitleById/{songId}", method = RequestMethod.GET)
	public @ResponseBody Map<String, Object> getSongTitleById(@PathVariable("songId") String songId,
			HttpServletRequest request) {
	
		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("GET %s", Utils.getUrl(request)));
		
		DbQueryStatus dbQueryStatus = songDal.getSongTitleById(songId);
		
		response.put("message", dbQueryStatus.getMessage());
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		
		return response;
	}

	/**
	* Deletes song with id songId
	* @param  songId  id of the song to be deleted
	* @param  request  http API request
	* @return  DbQueryStatus containing server response message and status of operation
	*/
	@RequestMapping(value = "/deleteSongById/{songId}", method = RequestMethod.DELETE)
	public @ResponseBody Map<String, Object> deleteSongById(@PathVariable("songId") String songId,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("DELETE %s", Utils.getUrl(request)));

		DbQueryStatus dbQueryStatus = songDal.deleteSongById(songId);
		
		response.put("message", dbQueryStatus.getMessage());
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		
		return response;
	}

	/**
	* Adds song with parameters specified in params
	* @param  params  a dictionary containing KEY_SONG_NAME, KEY_SONG_ARTIST_FULL_NAME, KEY_SONG_ALBUM
	* @param  request  http API request
	* @return  DbQueryStatus containing song document, server response message and status of operation
	*/
	@RequestMapping(value = "/addSong", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> addSong(@RequestParam Map<String, String> params,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("POST %s", Utils.getUrl(request)));
		String songName = params.get(Song.KEY_SONG_NAME);
		String songArtistFullName = params.get(Song.KEY_SONG_ARTIST_FULL_NAME);
		String songAlbum = params.get(Song.KEY_SONG_ALBUM);

		if ( songName == null || songArtistFullName == null || songAlbum == null) {
			response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null);
			response.put("message", "Bad");
		} else {
			Song song = new Song(songName, songArtistFullName, songAlbum);
			DbQueryStatus dbQueryStatus = songDal.addSong(song);
			response.put("message", dbQueryStatus.getMessage());
			response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		}
		return response;
	}
	
	/**
	* Increments or decrements the favorite count of song with id songId
	* @param  songId  id of the song whos favorite count to be updated
	* @param  shouldDecrement  Boolean specifying whether to increment or decrement
	* @param  request  http API request
	* @return  DbQueryStatus containing server response message and status of operation
	*/
	@RequestMapping(value = "/updateSongFavouritesCount/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> updateFavouritesCount(@PathVariable("songId") String songId,
			@RequestParam("shouldDecrement") String shouldDecrement, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		if(!shouldDecrement.equals("true") && !shouldDecrement.equals("false")) {
			response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null);
			response.put("message", "Bad");
			return response;
		}
		
		try {
			boolean bool = Boolean.parseBoolean(shouldDecrement);
			
			DbQueryStatus dbQueryStatus = songDal.updateSongFavouritesCount(songId, bool);
			response.put("message", dbQueryStatus.getMessage());
			response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		} catch(Exception e) {
			response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null);
			response.put("message", "Bad");
		}
		return response;
	}
}