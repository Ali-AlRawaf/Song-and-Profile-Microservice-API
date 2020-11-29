package com.csc301.profilemicroservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.json.JSONArray;
import org.json.JSONObject;
import org.neo4j.driver.*;


import org.springframework.stereotype.Repository;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.Values;
import org.neo4j.driver.v1.*;

@Repository
public class ProfileDriverImpl implements ProfileDriver {

	Driver driver = ProfileMicroserviceApplication.driver;

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
	
	@Override
	public DbQueryStatus createUserProfile(String userName, String fullName, String password) {
	     try (Session session = ProfileMicroserviceApplication.driver.session()){
	            try (Transaction tx = session.beginTransaction()) {
	                tx.run("MERGE (a:nProfile {userName: $x, fullName: $y, password: $y})", Values.parameters("x", userName, "y", fullName, "z", password));
	                tx.run("MERGE (a:playlist {plName: $x})", Values.parameters("x", userName +"-favorites"));
                    tx.run("MATCH (a:nProfile {userName: $x}),(p:playlist {plName: $y})\n" +  "MERGE (a)-[:created]->(p)", Values.parameters("x", userName, "y", userName +"-favorites"));
					tx.success();

                    session.close();   
	            }
	        }
	   
		return null;
	}

	@Override
	public DbQueryStatus followFriend(String userName, String frndUserName) {
		
		return null;
	}

	@Override
	public DbQueryStatus unfollowFriend(String userName, String frndUserName) {
		
		return null;
	}

	@Override
	public DbQueryStatus getAllSongFriendsLike(String userName) {
			
		return null;
	}
}
