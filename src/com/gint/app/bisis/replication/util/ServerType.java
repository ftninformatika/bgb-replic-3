package com.gint.app.bisis.replication.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ServerType {
  
  public static ServerType EXPORT;
  public static ServerType SECONDARY;
  public static ServerType IMPORT;
  
  public boolean exists(Connection conn) {
    String sql = (String)check.getStatements().get(0);
    Statement stmt = null;
    ResultSet rset = null;
    boolean retVal = true;
    try {
      stmt = conn.createStatement();
      rset = stmt.executeQuery(sql);
      rset.next();
    } catch (Exception ex) {
      retVal = false;
    } finally {
      try {
        if (rset != null)
          rset.close();
        if (stmt != null)
          stmt.close();
      } catch (Exception ex1) {
      }
    }
    return retVal;
  }
  
  public void setup(Connection conn) {
    execute(conn, create.getStatements());
  }
  
  public void remove(Connection conn) {
    execute(conn, drop.getStatements());
  }
  
  public String getDescription() {
    return description;
  }
  
  public String toString() {
    return description;
  }
  
  private void execute(Connection conn, List statements) {
    try {
      Statement stmt = conn.createStatement();
      Iterator it = statements.iterator();
      while (it.hasNext()) {
        String sql = (String)it.next();
        stmt.executeUpdate(sql);
      }
      stmt.close();
      conn.commit();
    } catch (Exception ex) {
      log.fatal(ex);
    }
  }
  
  static {
    EXPORT = new ServerType(
        "/create-export-server.sql", 
        "/drop-export-server.sql", 
        "/check-export-server.sql",
        "Eksport server");
    IMPORT = new ServerType(
        "/create-import-server.sql", 
        "/drop-import-server.sql", 
        "/check-import-server.sql",
        "Import server");
    SECONDARY = new ServerType(
        "/create-secondary-server.sql", 
        "/drop-secondary-server.sql", 
        "/check-secondary-server.sql",
        "Sekundarni server");
  }
  
  private ServerType(String sqlCreate, String sqlDrop, String sqlCheck, 
      String description) {
    create = new SqlFile(getClass().getResource(sqlCreate));
    drop = new SqlFile(getClass().getResource(sqlDrop));
    check = new SqlFile(getClass().getResource(sqlCheck));
    this.description = description;
  }
  
  private SqlFile create;
  private SqlFile drop;
  private SqlFile check;
  private String description;
  
  private static Log log = LogFactory.getLog(ServerType.class.getName());
}
