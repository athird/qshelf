package uk.ac.open.kmi.carre.qs.sparql;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import com.hp.hpl.jena.sparql.engine.http.QueryExceptionHTTP;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import org.apache.jena.atlas.web.auth.HttpAuthenticator;
import org.apache.jena.atlas.web.auth.PreemptiveBasicAuthenticator;
import org.apache.jena.atlas.web.auth.ScopedAuthenticator;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Properties;

import com.hp.hpl.jena.rdf.model.RDFNode;

import uk.ac.open.kmi.carre.qs.vocabulary.CARREVocabulary;
import virtuoso.jdbc4.VirtuosoConnection;
import virtuoso.jdbc4.VirtuosoConnectionPoolDataSource;
import virtuoso.jena.driver.*;

import javax.sql.PooledConnection;


/**
 * Created by gg3964 on 04/07/2014.
 */
public class CarrePlatformConnector {

	private String DB_USERNAME = "";
	private String DB_PASSWORD = "";
	private String DB_SERVER = "";
	private  int DB_SERVER_PORT = 1111;

	protected String uname;
	protected String upassw;
	protected String endpoint = "";
	protected Boolean loggedIn;

	public CarrePlatformConnector(String propertiesPath){
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(propertiesPath));
			String dbUser = properties.getProperty("carre.dbuser");
			String dbPassword = properties.getProperty("carre.dbpass");
			String dbServer = properties.getProperty("carre.dbserver");
			String dbServerPort = properties.getProperty("carre.dbserverport");
			String dbEndpoint = properties.getProperty("carre.dbendpoint");
			if (dbUser != null) {
				DB_USERNAME = dbUser;
			}
			if (dbPassword != null) {
				DB_PASSWORD = dbPassword;
			}
			if (dbServer != null) {
				DB_SERVER = dbServer;
			}
			if (dbServerPort != null) {
				DB_SERVER_PORT = Integer.parseInt(dbServerPort);
			}
			if (dbEndpoint != null) {
				endpoint = dbEndpoint;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}//function

	private Integer executeSQL(String sql){
		try {
			VirtuosoConnectionPoolDataSource pds = new VirtuosoConnectionPoolDataSource();
			pds.setUser(DB_USERNAME);
			pds.setPassword(DB_PASSWORD);
			pds.setServerName(DB_SERVER);
			pds.setPortNumber(DB_SERVER_PORT);
			PooledConnection pconn = pds.getPooledConnection();
			Connection connection = pconn.getConnection();

			PreparedStatement preparedStatement = connection
					.prepareStatement(sql);
			Integer rs = preparedStatement.executeUpdate();
			if (rs<0)
				return rs;
			//            System.out.println(rs);
		} catch (Exception e){
			System.out.println(e.getMessage());
			return e.hashCode();
		}
		return 0;

	}//function

	public ResultSet executeSPARQL(String sparql) {
		VirtGraph set;
		try {
			set = new VirtGraph(endpoint, DB_USERNAME, DB_PASSWORD);
			String query = "";
			for (String prefix : CARREVocabulary.PREFICES) {
				query += "PREFIX " + prefix + "\n";
			}
			query += "\n";
			query += sparql;
			System.err.println(query);
			Query sparqlQuery = QueryFactory.create(query);
			VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparqlQuery, set);
			return vqe.execSelect();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public Boolean insertTriples(String user, String triples) {
		VirtGraph set;
		try {
			if (user.contains(CARREVocabulary.USER_URL)) {
				user = user.replace(CARREVocabulary.USER_URL, "");
			}
			set = new VirtGraph (endpoint, DB_USERNAME, DB_PASSWORD);
			String query = "";
			for (String prefix : CARREVocabulary.PREFICES) {
				query += "PREFIX " + prefix + "\n";
			}
			query += "\n";
			query += "INSERT IN <" + CARREVocabulary.USER_URL + user + "> {\n" + triples + "}";
			System.err.println(query);
			VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(query, set);
			vur.exec();
			

		} catch (Exception e){
			System.out.println(e.getMessage());
			return false;
		}

		return true;
	}

	public Boolean InsertTriple(String triple){
		VirtGraph set;
		try {
			set = new VirtGraph (endpoint, uname, upassw);
			String query = "INSERT IN <" + CARREVocabulary.USER_URL + uname + "> {" + triple + "}";
			VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(query, set);
			vur.exec();

		} catch (Exception e){
			System.out.println(e.getMessage());
			return false;
		}

		return true;
	}//function

	public Boolean updateRefreshToken(String connectionURI, String newRefreshToken) {
		VirtGraph set;
		try {
			set = new VirtGraph (endpoint, DB_USERNAME, DB_PASSWORD);
			String query = "";
			for (String prefix : CARREVocabulary.PREFICES) {
				query += "PREFIX " + prefix + "\n";
			}
			query += "\n";
			query += "WITH <" + CARREVocabulary.USER_URL + uname 
					+ ">\n DELETE { <" + connectionURI + "> <" + CARREVocabulary.REFRESH_TOKEN_PREDICATE + "> ?token }\n"
					+ "INSERT { <" + connectionURI + "> <" + CARREVocabulary.REFRESH_TOKEN_PREDICATE + "> \"" + newRefreshToken + "\"^^xsd:string }\n"
					+ "WHERE \n"
					+ "{ <" + connectionURI + "> <" + CARREVocabulary.REFRESH_TOKEN_PREDICATE + "> ?token } ";
			System.err.println(query);
			VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(query, set);
			vur.exec();

		} catch (Exception e){
			System.out.println(e.getMessage());
			return false;
		}

		return true;
	}
	
}//class
