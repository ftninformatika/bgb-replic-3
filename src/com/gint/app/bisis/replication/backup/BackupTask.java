package com.gint.app.bisis.replication.backup;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gint.app.bisis.replication.util.INIFile;
import com.gint.util.file.FileUtils;

public class BackupTask {
  
  public BackupTask(BackupFrame frame) {
    backupDatabase(frame, "database");
    backupDatabase(frame, "secondary");
  }
  
  public void backupDatabase(BackupFrame frame, String database) {
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
    Date today = new Date();
    
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
      conn.setAutoCommit(false);
      log.info("Uspostavljena veza sa bazom.");

      if (exists(conn, "EXPORT_HISTORY"))
        backupTable(conn, "EXPORT_HISTORY", dir, 
            "export_history-"+intern.format(today)+".csv");
      if (exists(conn, "REPLICATION_HISTORY"))
        backupTable(conn, "REPLICATION_HISTORY", dir, 
            "replication_history-"+intern.format(today)+".csv");
      if (exists(conn, "IMPORT_HISTORY"))
        backupTable(conn, "IMPORT_HISTORY", dir, 
            "import_history-"+intern.format(today)+".csv");
      if (exists(conn, "RECORD_MAP"))
        backupTable(conn, "RECORD_MAP", dir, 
            "record_map-"+intern.format(today)+".csv");
      
      conn.close();
    } catch (Exception ex) {
      log.fatal(ex);
    } finally {
      if (frame != null)
        frame.enableGUI();
    }
    log.info("Gotovo.");
  }
  
  public void backupTable(Connection conn, String tableName, File dir, 
      String fileName) {
    List tableCols = TableConfig.getColumns(tableName);
    List tableTypes = TableConfig.getTypes(tableName);
    int colCount = tableCols.size();
    String query = "SELECT ";
    for (Iterator it = tableCols.iterator(); it.hasNext(); ) {
      String colName = (String)it.next();
      query += colName;
      if (it.hasNext())
        query += ",";
    }
    query += " FROM " + tableName;
    
    File dest = new File(dir, fileName);
    
    Statement stmt = null;
    ResultSet rset = null;
    try{
      PrintWriter out = new PrintWriter(new BufferedWriter(
          new OutputStreamWriter(new FileOutputStream(dest), "UTF8")));
      stmt = conn.createStatement();
      rset = stmt.executeQuery(query);
      while (rset.next()) {
        int i = 1;
        for (Iterator it = tableCols.iterator(); it.hasNext();) {
          String colName = (String)it.next();
          int colType = ((Integer)tableTypes.get(i-1)).intValue();
          switch (colType) {
            case Types.CHAR:
              String svalue = rset.getString(i);
              out.print(svalue);
              break;
            case Types.INTEGER:
              int ivalue = rset.getInt(i);
              out.print(ivalue);
              break;
            case Types.DATE:
              Date dvalue = rset.getDate(i);
              out.print(intern.format(dvalue));
              break;
            default:
              log.fatal("Unexpected SQL type for " + tableName + "." + 
                  colName + ": " + colType);
              break;
          }
          if (i++ < colCount)
            out.print(",");
        }
        out.println();
      }
      out.close();
    } catch (Exception ex) {
      ex.printStackTrace();
      log.fatal(ex);
    } finally {
      try {
        if (rset != null)
          rset.close();
        if (stmt != null)
          stmt.close();
      } catch (Exception e) { log.fatal(e); }
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

  private SimpleDateFormat intern = new SimpleDateFormat("yyyyMMdd");
  private static Log log = LogFactory.getLog(BackupTask.class.getName());
}
