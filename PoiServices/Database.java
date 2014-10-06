package PoiServices;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Observable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.sun.rowset.CachedRowSetImpl;

import Configuration.DatabaseInformation;

public class Database extends Observable {
	private final java.sql.Connection connection;
	private final String DB_URL;
	// register user
	private final String sSelectUsers = "SELECT USERNAME FROM USER WHERE ENABLED=1 ORDER BY USERNAME";
	private final String sSelectUser = "SELECT USERNAME FROM USER WHERE ENABLED=1 AND USERNAME='%s'";
	private final String sSelectUserPassword = "SELECT USERNAME FROM USER WHERE ENABLED=1 AND USERNAME='%s' AND PASSWORD='%s'";
	private final String sInsertUser = "INSERT INTO USER(USERNAME, PASSWORD) VALUES('%s', '%s')";
	private final String sDeleteUser = "UPDATE USER SET enabled=0 WHERE USERNAME='%s'";
	private final String sDeleteUsers = "DELETE FROM USER";
	// setMonitor data
	private final String sSelectPOI_by_T = "select * from pointofinterest where inserted between now()-%d AND now()";
	// private final String sSelectPOI = "SELECT * FROM POINTOFINTEREST WHERE DESCRIPTION='%s' AND LOCATION='%s') VALUES ('%s', '%s')";
	private final String sSelectPOI_ID = "SELECT id FROM POINTOFINTEREST WHERE DESCRIPTION='%s' AND LOCATION=GeomFromText('POINT(%d %d)')";
	private final String sSelectPOIArea = "select *, AsText(location) as loc, st_distance(location, GeomFromText('POINT(%d %d)')) as distance from POINTOFINTEREST where st_distance(location, GeomFromText('POINT(%d %d)')) < %d";	
	private final String sSelectPOIAreaStats = "select p.description, p.location, p.SecondaryType, p.inserted, count(distinct setlog.username) as TotalSet, count(distinct getlog.username) as TotalGet, AsText(p.location) as loc, st_distance(location, GeomFromText('POINT(%d %d)')) as distance from POINTOFINTEREST p, setlog, getlog where setlog.poi_id=p.id and getlog.poi_id=p.id and st_distance(p.location, GeomFromText('POINT(%d %d)')) < %d group by p.description, p.location, p.SecondaryType, p.inserted";
	private final String sSelectPOIAreaByUser = "select p.description, p.location, p.SecondaryType, p.inserted, AsText(p.location) as loc from SetLog s, POINTOFINTEREST p where s.poi_id=p.id and username='%s' order by p.description";
	private final String sCountPOIArea = "select * from POINTOFINTEREST where DESCRIPTION='%s' and st_distance(location, GeomFromText('POINT(%d %d)')) < %d";
	private final String sFindNearestPOI = "select * from POINTOFINTEREST where DESCRIPTION='%s' and st_distance(location, GeomFromText('POINT(%d %d)')) < %d order by st_distance(location, GeomFromText('POINT(%d %d)'))";
	private final String sInsertPOI = "INSERT INTO POINTOFINTEREST(DESCRIPTION, LOCATION, TYPE, SECONDARYTYPE) VALUES ('%s', GeomFromText('POINT(%d %d)'), '%d', '%s')";
	
	//tab1:
	private final String sTab1 = "select (select count(*) from setlog where inserted between now()-INTERVAL %d MICROSECOND AND now()) as TotalSet, (select count(*) from getlog where inserted between now()-INTERVAL %d MICROSECOND  AND now()) as TotalGet, (select count(*) from setlog where inserted between now()-INTERVAL %d MICROSECOND AND now())+(select count(*) from getlog where inserted between now()-INTERVAL %d MICROSECOND  AND now()) as TotalSetGet";
	private final String sSetLog = "INSERT INTO SETLOG(USERNAME, POI_ID) VALUES ('%s', %d)";	
	// getMapData
	private final String sGetLog = "INSERT INTO GETLOG(USERNAME, POI_ID) VALUES ('%s', %d)";	

	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private final Lock read = lock.readLock();
	private final Lock write = lock.writeLock();
	
	public Database(DatabaseInformation d) {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			DB_URL = d.getDatabaseURL();
			System.out.println("DB_URL: " + DB_URL);
			connection = DriverManager.getConnection(DB_URL, d.username,
					d.password);

			System.out.println("Database connection successful to: "
					+ d.getDatabaseURL());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(
					"Cannot connect to database. JDBC drivers missing.");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			throw new IllegalArgumentException("Cannot connect to database: "
					+ d.getDatabaseURL() + ". Invalid database information: "
					+ e.getMessage());
		}
		
		System.out.println("********************** Database object has been created! ***************************");
	}

	public void close() {
		try {
			connection.close();
			System.out.println("Database connection closed from: " + DB_URL);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private CachedRowSetImpl ExecuteQuery(String query) {
		Statement statement = null;
		try {
			read.lock();
			CachedRowSetImpl cr = new CachedRowSetImpl();
			
			statement = connection.createStatement();
			System.out.println("Execute: " + query);
			ResultSet rs = statement.executeQuery(query);
			 
			cr.populate(rs);
			
			rs.close();
			statement.close();
			
			return cr;
		} catch (SQLException se) {
			System.out.println("Warning: " + se.getMessage());
		} finally {
			try {
				if (statement != null)
					statement.close();
			} catch (SQLException se2) {
				System.out.println("statement cleared after SQL Exception");
			}		
			read.unlock();
		}
		return null;
	}
	
	private int CountExecuteQuery(String query) {
		Statement statement = null;
		try {
			read.lock();
			int count = 0;
			statement = connection.createStatement();
			System.out.println("Execute: " + query);
			ResultSet rs = statement.executeQuery(query);
			while (rs.next())
				count++;
			
			rs.close();
			statement.close();
			
			return count;
		} catch (SQLException se) {
			System.out.println("Warning: " + se.getMessage());
		} finally {
			try {
				if (statement != null)
					statement.close();
			} catch (SQLException se2) {
				System.out.println("statement cleared after SQL Exception");
			}		
			read.unlock();
		}
		return -1;
	}

	private int ExecuteUpdate(String query, boolean exclusiveLock) {
		Statement statement = null;
		try {
			if (exclusiveLock) {
				write.lock();
			} else {
				read.lock();
			}
			statement = connection.createStatement();
			System.out.println("Execute: " + query);
			
			int i = statement.executeUpdate(query);
						
			statement.close();
			
			return i;
		} catch (SQLException se) {
			se.printStackTrace();
		}
		finally {
			try {
				if (statement != null)
					statement.close();
			} catch (SQLException se2) {
				
			}	
			if (exclusiveLock) {
				write.unlock();
			} else {
				read.unlock();
			}
		}
		return 0;
	}
	
	public int registerUser(String username, String password) {
		String q1 = String.format(sSelectUser, username);
		String q2 = String.format(sInsertUser, username, password);
		
		int c = CountExecuteQuery(q1);
		if (c==0) {
			System.out.println("User does not exist ... Inserted.");
			int i = ExecuteUpdate(q2, false);
			System.out.println("Notify!");
			setChanged();
			notifyObservers();
			return i;
		} else {
			System.out.println("User exists ... skipped.");
			return 0;
		}
	}
	
	public boolean authenticateUser(String username, String password) {
		String q = String.format(sSelectUserPassword, username, password);
		int c = CountExecuteQuery(q);
		if (c==0) {
			return false;
		} else {
			return true;
		}
	}
	
	public int setMonitorData(String username, int x, int y, String type, String name, int R) {
		try {					
			String q0 = String.format(sCountPOIArea, name, x, y, R);
			String q1 = String.format(sInsertPOI, name, x, y, 1, type);		
			String q2 = String.format(sSelectPOI_ID, name, x, y);
			
			// count nearby pois
			int i = CountExecuteQuery(q0);
			if (i==0) {										// does not exist
				i = ExecuteUpdate(q1, false);					// insert
				if (i==0) {
					throw new SQLException("Poi Insertion failed.");
				}
				CachedRowSetImpl c = ExecuteQuery(q2);		// select PID
				if (c.next()) {
					int poi_id = c.getInt("id");
					c.close();
					
					String q3 = String.format(sSetLog, username, poi_id);
					i = ExecuteUpdate(q3, false);
					setChanged();
					notifyObservers();
					
					return poi_id;
				} else {
					System.out.println("Warning: Primary key not obtained");
				}
			} else {								// poi already exists
				System.out.println("POI exists. Ignored.");			// find nearest to add the select row
				String q4 = String.format(sFindNearestPOI, name, x, y, R, x ,y);
				CachedRowSetImpl c = ExecuteQuery(q4);
				c.next();
				String q5 = String.format(sSetLog, username, c.getInt("id"));	
				i = ExecuteUpdate(q5, false);
				setChanged();
				notifyObservers();
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block		// ## add set....  
			e.printStackTrace();					
		}
		return -1;
	}
	
	public String getMapData(String username, int x, int y, int R) {
		try {		
			StringBuffer sb = new StringBuffer();
			String q = String.format(sSelectPOIArea, x, y, x,y ,R);
			
			CachedRowSetImpl c = ExecuteQuery(q);		// select PID
			while (c.next()) {
				int poi_id = c.getInt("id");
				
				String q3 = String.format(sGetLog, username, poi_id);
				ExecuteUpdate(q3, false);
				sb.append(c.getString("description") + "#" + c.getString("loc") + "#" + c.getString("SecondaryType"));
				sb.append("$");
			} 
			c.close();
			if (sb.toString().equals("")) {
				return "NONE";
			} else {
				return sb.toString();
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();					
		}
		return "None found";
	}
	
	public void DeleteUsers() {
		String q = String.format(sDeleteUsers);
		ExecuteUpdate(q, true);
	}
	
	public void DeleteUser(String username) {
		String q = String.format(sDeleteUser, username);
		ExecuteUpdate(q, true);
	}
	
	
	public String CalculateTab1Statistics(int T) {
		try {
			String q = String.format(sTab1, T,T,T,T);
			StringBuffer sb = new StringBuffer();
			sb.append("GET:");
			
			CachedRowSetImpl c = ExecuteQuery(q);
			c.next();
			sb.append(c.getString("TotalGet")).append("\tSET:").append(c.getString("TotalSet")).append("\tSET+GET:").append(c.getString("TotalSetGet"));
			c.close();
			
			return sb.toString();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public String CalculateTab2Statistics() {
		return "N/a";
	}
	
	public String sSelectPOI_by_T(int t) {
		try {		
			StringBuffer sb = new StringBuffer();
			String q = String.format(sSelectPOI_by_T, t);
			
			CachedRowSetImpl c = ExecuteQuery(q);		// select PID
			while (c.next()) {
				ExecuteUpdate(q, false);
				sb.append(c.getString("description") + "     " + c.getString("loc") + "     " + c.getString("SecondaryType"));
				sb.append("\n");
			} 
			c.close();
			return sb.toString();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();					
		}
		return "None found";
	}
	
	public String GetLocationStatistics(int x, int y, int R) {
		try {		
			StringBuffer sb = new StringBuffer();
			String q = String.format(sSelectPOIAreaStats, x, y, x,y ,R);
			
			CachedRowSetImpl c = ExecuteQuery(q);		// select PID
			while (c.next()) {
				sb.append(c.getString("description") + "\t" + c.getString("loc") + "\t" + c.getString("SecondaryType") + "\tSet:" + c.getString("TotalSet") + "\tGet:" + c.getString("TotalGet"));
				sb.append("\n");
			} 
			c.close();
			return sb.toString();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();					
		}
		return "None found";
	}
	

	public String GetLocationStatisticsByUser(String username) {
		try {		
			StringBuffer sb = new StringBuffer();
			String q = String.format(sSelectPOIAreaByUser, username);
			
			CachedRowSetImpl c = ExecuteQuery(q);		// select PID
			while (c.next()) {
				sb.append(c.getString("description") + "\t" + c.getString("loc") + "\t" + c.getString("SecondaryType"));
				sb.append("\n");
			} 
			c.close();
			return sb.toString();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();					
		}
		return "None found";
	}
	
	public ArrayList<String> GetUserNames(){
		ArrayList<String> list = new ArrayList<String>();
		try {		
			String q = String.format(sSelectUsers);
			CachedRowSetImpl c = ExecuteQuery(q);
			while (c.next()) {
				list.add(c.getString("username"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return list;
	}
}
