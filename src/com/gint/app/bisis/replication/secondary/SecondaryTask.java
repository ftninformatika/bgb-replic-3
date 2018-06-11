package com.gint.app.bisis.replication.secondary;

import java.io.File;
import java.io.RandomAccessFile;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gint.app.bisis.common.records.Record;
import com.gint.app.bisis.common.records.RecordFactory;
import com.gint.app.bisis.replication.export.ExportInfo;
import com.gint.app.bisis.replication.util.INIFile;
import com.gint.app.bisis.replication.util.ServerType;
import com.gint.app.bisis.replication.util.ZipUtils;
import com.gint.app.bisis.textsrv.TextServer;
import com.gint.util.file.FileUtils;

public class SecondaryTask {
  
  public SecondaryTask(SecondaryFrame frame) {
    File config = new File(FileUtils.getClassDir(getClass()) + "/secondary.ini");
    if (!config.isFile()) {
      log.fatal("Ne postoji konfiguracioni fajl: " + config.getAbsolutePath());
      return;
    }
    INIFile ini = new INIFile(FileUtils.getClassDir(getClass()) + "/secondary.ini");
    String driver = ini.getString("database", "driver");
    String url = ini.getString("database", "url");
    String username = ini.getString("database", "username");
    String password = ini.getString("database", "password");
    String fileName = ini.getString("secondary", "filename");
    boolean ignoreImportDates = ini.getBoolean("secondary", "ignore-import-dates");
    
    File zipFile = new File(fileName);
    if (!zipFile.isFile()) {
      log.fatal("Ne postoji fajl " + fileName);
      return;
    }
    if (!zipFile.canRead()) {
      log.fatal("Nije moguce citanje fajla " + fileName);
      return;
    }
    File dir = zipFile.getParentFile();
    if (!dir.canWrite()) {
      log.fatal("Nije moguce pisati u direktorijum " + dir.getAbsolutePath());
      return;
    }
    
    ZipUtils.unzip(zipFile);
    File recordsFile = new File(dir, "records.dat");
    if (!recordsFile.isFile()) {
      log.fatal("Nema fajla " + recordsFile.getAbsolutePath());
      return;
    }
    File infoFile = new File(dir, "export.info");
    if (!infoFile.isFile()) {
      log.fatal("Nema fajla " + infoFile.getAbsolutePath());
      return;
    }
    
    String line = "";
    try {
      ExportInfo exportInfo = new ExportInfo(FileUtils.readTextFile(infoFile));
      boolean incremental = 
        (exportInfo.getExportType() == ExportInfo.TYPE_INCREMENTAL);
      Class.forName(driver);
      log.info("Ucitan drajver za bazu.");
      Connection conn = DriverManager.getConnection(url, username, password);
      conn.setAutoCommit(false);
      log.info("Uspostavljena veza sa bazom.");
      TextServer txtSrv = new TextServer("sapdb", conn, url, username, password);
      log.info("Inicijalizovan tekst server.");
      try {
        ServerType serverType = ServerType.SECONDARY;
        boolean exists = serverType.exists(conn);
        if (!exists) {
          log.fatal("Sekundarni server nije inicijalizovan u bazi podataka!");
          conn.close();
          return;
        }
        Statement stmt = conn.createStatement();
        ResultSet rset = stmt.executeQuery(
            "SELECT count(*) FROM replication_history");
        rset.next();
        int replCount = rset.getInt(1);
        rset.close();
        stmt.close();
        if (replCount == 0 && incremental) {
          log.fatal("Eksport je inkrementalni, a na sekundarnom serveru nije bilo ranijih kopiranja!");
          conn.close();
          return;
        }
        if (frame != null)
          frame.setMaximum(exportInfo.getRecordCount());
        PreparedStatement pstmt = conn.prepareStatement(
            "INSERT INTO replication_history (replication_date, replication_type) VALUES (?, ?)");
        pstmt.setDate(1, new java.sql.Date(new java.util.Date().getTime()));
        pstmt.setString(2, incremental ? "I" : "F");
        pstmt.executeUpdate();
        pstmt.close();
        conn.commit();
      } catch (Exception ex) {
        if (!ignoreImportDates) {
          log.fatal(ex);
          log.fatal("Verovatno je vec obavljeno kopiranje sa danasnjim datumom.");
          return; 
        } else {
          log.warn("Bar jedan import sa danasnjim datumom je vec napravljen");
        }
      }
      int count = 0;
      RandomAccessFile in = new RandomAccessFile(recordsFile, "r");
      while ((line = in.readLine()) != null) {
        ++count;
        if (frame != null)
          frame.setValue(count);
        line = line.trim();
        if (line.length() == 0)
          continue;
        Record rec = null;
        try {
          rec = RecordFactory.fromUNIMARC(0, line);
        } catch (Exception ex) {
          log.warn("Parsiranje zapisa br. " + count + " je puklo");
          log.warn(ex);
          continue;
        }
        String RN = rec.getSubfieldContent("001e");
        if (RN == null) {
          log.warn("Zapis br. " + count + " nema RN.");
          continue;
        }
        if (incremental) {
          int hitCount = txtSrv.select("RN=" + RN);
          switch (hitCount) {
            case 0:
              txtSrv.insert(line);
              break;
            case 1:
              int docid = txtSrv.getDocID(1);
              txtSrv.update(docid, line);
              break;
            default:
              log.warn("Upit RN="+RN+" vratio je vise od jednog pogotka!");
              break;
          }
        } else {
          txtSrv.insert(line);
        }
      }
      in.close();
      conn.close();
      log.info("Kopiranje u sekundarnu bazu zavrseno.");
    } catch (Exception ex) {
      log.fatal(ex);
    } finally {
      if (frame != null)
        frame.enableGUI();
    }
  }
  
  private static Log log = LogFactory.getLog(SecondaryTask.class.getName());
}
