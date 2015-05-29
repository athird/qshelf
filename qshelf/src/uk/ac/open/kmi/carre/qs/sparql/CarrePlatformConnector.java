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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Logger;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

import uk.ac.open.kmi.carre.qs.service.iHealth.OAuth2AccessToken;
import uk.ac.open.kmi.carre.qs.service.misfit.MisfitService;
import uk.ac.open.kmi.carre.qs.vocabulary.CARREVocabulary;
import virtuoso.jdbc4.VirtuosoConnection;
import virtuoso.jdbc4.VirtuosoConnectionPoolDataSource;
import virtuoso.jena.driver.*;

import javax.sql.PooledConnection;


/**
 * Created by gg3964 on 04/07/2014.
 */
public class CarrePlatformConnector {
	private static Logger logger = Logger.getLogger(CarrePlatformConnector.class.getName());

	private String DB_USERNAME = "";
	private String DB_PASSWORD = "";
	private String DB_SERVER = "";
	private  int DB_SERVER_PORT = 1111;

	protected String uname;
	protected String upassw;
	protected String endpoint = "";
	protected Boolean loggedIn;

	public static void main(String[] args) {
		String sparql = "";
		CarrePlatformConnector connector = new CarrePlatformConnector("/Users/allanthird/Work/workspaces/git/qshelf2/qshelf/WebContent/WEB-INF/config.properties");
		ResultSet results = connector.executeSPARQL(sparql);
		logger.finer("getRowNumber: " + results.getRowNumber());
	}

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
			logger.finer(e.getMessage());
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
			logger.finer(query);
			Query sparqlQuery = QueryFactory.create(query);
			VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparqlQuery, set);
			ResultSet results = vqe.execSelect();
			return results;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public Boolean insertTriples(String user, String triples) {
		VirtGraph set;
		try {
			if (user.contains(CARREVocabulary.BASE_URL) || user.contains(CARREVocabulary.SECURE_BASE_URL)) {
				user = user.substring(user.lastIndexOf("/") + 1);
			}
			set = new VirtGraph (endpoint, DB_USERNAME, DB_PASSWORD);
			String query = "";
			for (String prefix : CARREVocabulary.PREFICES) {
				query += "PREFIX " + prefix + "\n";
			}
			query += "\n";
			query += "INSERT IN <" + CARREVocabulary.USER_URL + user + "> {\n" + triples + "}";
			logger.finer(query);
			VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(query, set);
			vur.exec();


		} catch (Exception e){
			logger.finer(e.getMessage());
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
			logger.finer(e.getMessage());
			return false;
		}

		return true;
	}//function

	public Boolean updateTripleObject(String username, String subject, String predicate, String object) {
		VirtGraph set;
		try {
			set = new VirtGraph (endpoint, DB_USERNAME, DB_PASSWORD);
			String prefices = "";
			for (String prefix : CARREVocabulary.PREFICES) {
				prefices += "PREFIX " + prefix + "\n";
			}
			prefices += "\n";
			String query = "" + prefices;
			/*query += "WITH <" + CARREVocabulary.USER_URL + uname 
					+ ">\n DELETE { <" + connectionURI + "> <" + CARREVocabulary.REFRESH_TOKEN_PREDICATE + "> ?token }\n"
					+ "INSERT { <" + connectionURI + "> <" + CARREVocabulary.REFRESH_TOKEN_PREDICATE + "> \"" + newAccessToken + "\"^^xsd:string }\n"
					+ "WHERE \n"
					+ "{ <" + connectionURI + "> <" + CARREVocabulary.REFRESH_TOKEN_PREDICATE + "> ?token } ";*/
			query += "WITH <" + CARREVocabulary.USER_URL + username 
					+ ">\n DELETE { ?subject ?predicate ?object } WHERE { ?subject ?predicate ?object . <" 
					+ subject + "> " + predicate + " ?object } \n";
			logger.finer(query);
			VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(query, set);
			vur.exec();
			query = "" + prefices;
			query	+= "INSERT IN <" + CARREVocabulary.USER_URL + username + "> {\n"
					+ "<" + subject + "> " + predicate + " " + object + " }\n";
			logger.finer(query);
			VirtuosoUpdateRequest vur2 = VirtuosoUpdateFactory.create(query, set);
			vur2.exec();

		} catch (Exception e){
			logger.finer(e.getMessage());
			return false;
		}

		return true;
	}

}//class
















