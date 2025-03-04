package com.csc301.profilemicroservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.csc301.profilemicroservice.Utils;
import com.csc301.profilemicroservice.exceptions.FourHundredException;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/")
public class ProfileController {
	public static final String KEY_USER_NAME = "userName";
	public static final String KEY_USER_FULLNAME = "fullName";
	public static final String KEY_USER_PASSWORD = "password";

	@Autowired
	private final ProfileDriverImpl profileDriver;

	@Autowired
	private final PlaylistDriverImpl playlistDriver;

	OkHttpClient client = new OkHttpClient();

	public ProfileController(ProfileDriverImpl profileDriver, PlaylistDriverImpl playlistDriver) {
		this.profileDriver = profileDriver;
		this.playlistDriver = playlistDriver;
	}
	
	
	/**
	* Add user to db and return status
	* @param  params  a dictionary containing KEY_USER_NAME, KEY_USER_FULLNAME, KEY_USER_PASSWORD
	* @param   request http API request
	* @return  DbQueryStatus with OK status if operation is success, and not OK otherwise
	*/
	@RequestMapping(value = "/profile", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> addProfile(@RequestParam Map<String, String> params,
			HttpServletRequest request) {
		
		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("POST %s", Utils.getUrl(request)));
		
		if (params.get(KEY_USER_NAME) == null || params.get(KEY_USER_FULLNAME) == null || params.get(KEY_USER_PASSWORD) == null) {
			response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null);
			response.put("message", "Bad");
		} else {
			DbQueryStatus dbQueryStatus = profileDriver.createUserProfile(params.get(KEY_USER_NAME), params.get(KEY_USER_FULLNAME), params.get(KEY_USER_PASSWORD));
			response.put("message", dbQueryStatus.getMessage());
			response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		}
		return response;
	}
	
	/**
	* Create a :follows relation between the given users and return status
	* @param  userName  a string of the desired username (the user following)
	* @param  frndUserName  a string of the desired frndUserName (the user being followed)
	* @param   request http API request
	* @return  DbQueryStatus with OK status if operation is success, and not OK otherwise
	*/
	@RequestMapping(value = "/followFriend/{userName}/{friendUserName}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> followFriend(@PathVariable("userName") String userName,
			@PathVariable("friendUserName") String friendUserName, HttpServletRequest request) {
		
		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		
		DbQueryStatus dbQueryStatus = profileDriver.followFriend(userName, friendUserName);
		response.put("message", dbQueryStatus.getMessage());
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		return response;
	}
	
	/**
	* Returns a list of all the songs that every friend of username likes inside dbquerydata
	* @param  userName  a string of the desired username (the user following)
	* @param   request http API request
	* @return  DbQueryStatus with OK status if operation is success and all songs in the playlist of every user userName follows, and not OK otherwise
	*/
	@RequestMapping(value = "/getAllFriendFavouriteSongTitles/{userName}", method = RequestMethod.GET)
	public @ResponseBody Map<String, Object> getAllFriendFavouriteSongTitles(@PathVariable("userName") String userName,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		
		DbQueryStatus dbQueryStatus = profileDriver.getAllSongFriendsLike(userName);
		response.put("message", dbQueryStatus.getMessage());
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		return response;
	}

	/**
	* Delete a :follows relation between the given users and return status
	* @param  userName  a string of the desired username (the user following)
	* @param  frndUserName  a string of the desired frndUserName (the user being followed)
	* @param   request http API request
	* @return  DbQueryStatus with OK status if operation is success, and not OK otherwise
	*/
	@RequestMapping(value = "/unfollowFriend/{userName}/{friendUserName}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> unfollowFriend(@PathVariable("userName") String userName,
			@PathVariable("friendUserName") String friendUserName, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		
		DbQueryStatus dbQueryStatus = profileDriver.unfollowFriend(userName, friendUserName);
		response.put("message", dbQueryStatus.getMessage());
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		return response;
	}

	/**
	* Adds a :includes relation between the given users playlist and the given songid and return status
	* @param  userName  a string of the desired username (the user adding to playlist)
	* @param  songId  a string of the desired songid (the one being followed)
	* @param   request http API request
	* @return  DbQueryStatus with OK status if operation is success, and not OK otherwise
	*/
	@RequestMapping(value = "/likeSong/{userName}/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> likeSong(@PathVariable("userName") String userName,
			@PathVariable("songId") String songId, HttpServletRequest request) {
		
		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		
		DbQueryStatus dbQueryStatus = playlistDriver.likeSong(userName, songId);
		response.put("message", dbQueryStatus.getMessage());
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		return response;
	}
	
	/**
	* Removes an :includes relation between the given users playlist and the given songid and return status
	* @param  userName  a string of the desired username (the user adding to playlist)
	* @param  songId  a string of the desired songid (the one being followed)
	* @param   request http API request
	* @return  DbQueryStatus with OK status if operation is success, and not OK otherwise
	*/
	@RequestMapping(value = "/unlikeSong/{userName}/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> unlikeSong(@PathVariable("userName") String userName,
			@PathVariable("songId") String songId, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));

		DbQueryStatus dbQueryStatus = playlistDriver.unlikeSong(userName, songId);
		response.put("message", dbQueryStatus.getMessage());
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		return response;
	}
	/**
	* Removes songid from db, and all relation connecting to it and return status
	* @param  songId  a string of the desired songid (the one being followed)
	* @param   request http API request
	* @return  DbQueryStatus with OK status if operation is success, and not OK otherwise
	*/
	@RequestMapping(value = "/deleteAllSongsFromDb/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> deleteAllSongsFromDb(@PathVariable("songId") String songId,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		
		DbQueryStatus dbQueryStatus = playlistDriver.deleteSongFromDb(songId);
		response.put("message", dbQueryStatus.getMessage());
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		return response;
	}
}