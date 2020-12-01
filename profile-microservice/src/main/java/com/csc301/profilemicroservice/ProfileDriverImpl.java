package com.csc301.profilemicroservice;

import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.springframework.stereotype.Repository;

import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.Values;

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
	            	int userNameResult = tx.run("MATCH (n:nProfile {userName: $x}) RETURN n" , Values.parameters("x", userName )).list().size();
	            	if (userNameResult == 0){
		                tx.run("MERGE (a:nProfile {userName: $x, fullName: $y, password: $y})", Values.parameters("x", userName, "y", fullName, "z", password));
		                tx.run("MERGE (a:playlist {plName: $x})", Values.parameters("x", userName +"-favorites"));
	                    tx.run("MATCH (a:nProfile {userName: $x}),(p:playlist {plName: $y})\n" +  "MERGE (a)-[:created]->(p)", Values.parameters("x", userName, "y", userName +"-favorites"));	
	            		tx.success();
	                    session.close();   
	                } else {
	                	return new DbQueryStatus("UserName already exist", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
	                } 	
	            }
	        }
     	return new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
	}

	@Override
	public DbQueryStatus followFriend(String userName, String frndUserName) {
		
		return null;
	}

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

	@Override
	public DbQueryStatus getAllSongFriendsLike(String userName) {
			
		return null;
	}
}
