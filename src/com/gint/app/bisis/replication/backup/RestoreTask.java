package com.gint.app.bisis.replication.backup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gint.app.bisis.replication.util.INIFile;
import com.gint.util.file.FileUtils;

public class RestoreTask {

  public RestoreTask(RestoreFrame frame) {
    restoreDatabase(frame, "database");
    restoreDatabase(frame, "secondary");
  }
  
  public void restoreDatabase(RestoreFrame frame, String database) {
    File config = new File(FileUtils.getClassDir(getClass()) + "/backup.ini");
    if (!config.isFile()) {
      log.fatal("Ne postoji konfiguracioni fajl: " + config.getAbsolutePath());
      return;
    }
    INIFile ini = new INIFile(FileUtils.getClassDir(getClass()) + "/backup.ini");
    String driver = ini.getString(database, "driver");
    String url = ini.getString(database, "url");
    String username = ini.getString(database, "username");
    String password = ini.getString(database, "password");
    String destination = ini.getString("backup", "destination");
    
    File dir = new File(destination);
    if (!dir.exists()) {
      dir.mkdirs();
      log.info("Direktorijum " + destination + " ne postoji, kreiran je.");
    }
    if (!dir.canWrite()) {
      Exception ex = new Exception("Nije dozvoljeno pisanje u direktorijum " + 
          destination);
      log.fatal(ex);
    }
    
    try {
      Class.forName(driver);
      log.info("Ucitan drajver za bazu.");
      Connection conn = DriverManager.getConnection(url, username, password);
      //conn.setAutoCommit(false);
      log.info("Uspostavljena veza sa bazom.");

      String filename = findNewest(dir, "export_history"); 
      if (filename != null && exists(conn, "EXPORT_HISTORY"))
        restoreTable(conn, "EXPORT_HISTORY", dir, filename);

      filename = findNewest(dir, "replication_history");
      if (filename != null && exists(conn, "REPLICATION_HISTORY"))
        restoreTable(conn, "REPLICATION_HISTORY", dir, filename); 

      filename = findNewest(dir, "import_history");
      if (filename != null && exists(conn, "IMPORT_HISTORY"))
        restoreTable(conn, "IMPORT_HISTORY", dir, filename);

      filename = findNewest(dir, "record_map");
      if (filename != null && exists(conn, "RECORD_MAP"))
        restoreTable(conn, "RECORD_MAP", dir, filename); 
      
      conn.close();
    } catch (Exception ex) {
      log.fatal(ex);
    } finally {
      if (frame != null)
        frame.enableGUI();
    }
    log.info("Gotovo.");
  }

  public void restoreTable(Connection conn, String tableName, File dir, 
      String fileName) {
    List tableCols = TableConfig.getColumns(tableName);
    List tableTypes = TableConfig.getTypes(tableName);
    int colCount = tableCols.size();
    String sql = "INSERT INTO " + tableName + "(";
    for (Iterator it = tableCols.iterator(); it.hasNext(); ) {
      String colName = (String)it.next();
      sql += colName;
      if (it.hasNext())
        sql += ",";
    }
    sql += ") VALUES (";
    for (int i = 0; i < colCount; i++) {
      sql += "?";
      if (i < colCount - 1)
        sql += ",";
    }
    sql += ")";
    
    File src = new File(dir, fileName);
    PreparedStatement stmt = null;
    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(src), "UTF8"));

      Statement del = conn.createStatement();
      int rowsAffected = del.executeUpdate("DELETE FROM " + tableName);
      del.close();
      log.info("Iz tabele " + tableName + " obrisano " + rowsAffected + " redova.");

      log.info("Citam podatke iz fajla: " + src.getAbsolutePath());
      stmt = conn.prepareStatement(sql);
      String line = null;
      while ((line = in.readLine()) != null) {
        line = line.trim();
        if (line.length() == 0)
          continue;
        String[] parts = line.split(",");
        int i = 0;
        stmt.clearParameters();
        for (Iterator it = tableCols.iterator(); it.hasNext();) {
          String colName = (String)it.next();
          int colType = ((Integer)tableTypes.get(i)).intValue();
          switch (colType) {
            case Types.CHAR:
              stmt.setString(i+1, parts[i]);
              break;
            case Types.INTEGER:
              stmt.setInt(i+1, Integer.parseInt(parts[i]));
              break;
            case Types.DATE:
              stmt.setDate(i+1, new java.sql.Date(intern.parse(parts[i]).getTime()));
              break;
            default:
              log.fatal("Unexpected SQL type for " + tableName + "." + 
                  colName + ": " + colType);
              break;
          }
          i++;
        }
        stmt.executeUpdate();
      }
      in.close();
    } catch (Exception ex) {
      ex.printStackTrace();
      log.fatal(ex);
    } finally {
      try {
        if (stmt != null)
          stmt.close();
      } catch (Exception e) { log.fatal(e); }
      log.info("Tabela " + tableName + " je napunjena.");
    }
  }
  
  public boolean exists(Connection conn, String tableName) {
    boolean retVal = false;
    Statement stmt = null;
    ResultSet rset = null;
    try {
      stmt = conn.createStatement();
      rset = stmt.executeQuery("SELECT * FROM " + tableName);
      retVal = true;
      log.info("Tabela " + tableName + " je pronadjena.");
    } catch (Exception ex) {
    } finally {
      try {
        if (rset != null)
          rset.close();
        if (stmt != null)
          stmt.close();
      } catch (Exception e) { log.fatal(e); }
    }
    return retVal;
  }
  
  public String findNewest(File dir, String stem) {
    final String stemm = stem;
    String[] files = dir.list(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.startsWith(stemm);
      }
    });
    if (files.length == 0)
      return null;
    Arrays.sort(files);
    return files[files.length-1];
  }

  private SimpleDateFormat intern = new SimpleDateFormat("yyyyMMdd");
  private static Log log = LogFactory.getLog(RestoreTask.class.getName());
}
