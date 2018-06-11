package com.gint.app.bisis.replication.fixrn;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gint.app.bisis.common.records.Field;
import com.gint.app.bisis.common.records.Record;
import com.gint.app.bisis.common.records.RecordFactory;
import com.gint.app.bisis.common.records.Subfield;
import com.gint.app.bisis.replication.util.INIFile;
import com.gint.util.file.FileUtils;

public class FixTask {
  public FixTask(FixFrame frame) {
    File config = new File(FileUtils.getClassDir(getClass()) + "/fixrn.ini");
    if (!config.isFile()) {
      log.fatal("Ne postoji konfiguracioni fajl: " + config.getAbsolutePath());
      return;
    }
    INIFile ini = new INIFile(FileUtils.getClassDir(getClass()) + "/fixrn.ini");
    String driver = ini.getString("database", "driver");
    String url = ini.getString("database", "url");
    String username = ini.getString("database", "username");
    String password = ini.getString("database", "password");

    try {
      Class.forName(driver);
      Connection conn = DriverManager.getConnection(url, username, password);
      conn.setAutoCommit(false);
      log.info("Otvorena veza sa bazom.");
      Statement stmt = conn.createStatement();
      ResultSet rset = stmt.executeQuery("SELECT count(*) FROM documents");
      rset.next();
      int recordCount = rset.getInt(1);
      rset.close();
      stmt.close();
      if (frame != null)
        frame.setMaximum(recordCount);
      log.info("Ukupno " + recordCount + " zapisa u bazi.");
      
      PreparedStatement update = conn.prepareStatement(
          "UPDATE documents SET document=? WHERE doc_id=?");
      PreparedStatement insert1 = conn.prepareStatement(
          "INSERT INTO Prefix_contents (prefID, pref_name, doc_id, content) " +
          "VALUES (PrefContSeq.NEXTVAL, 'RN', ?, ?)");
      PreparedStatement insert2 = conn.prepareStatement(
          "INSERT INTO pref_RN (doc_id, content, pref_id, word_pos, sent_pos) " +
          "VALUES (?, ?, PrefContSeq.CURRVAL, 1, 1)");
      
      stmt = conn.createStatement();
      rset = stmt.executeQuery("SELECT doc_id, document FROM documents");
      for (int count = 1; rset.next(); count++) {
        int id = rset.getInt(1);
        String rez = rset.getString(2);
        if (frame != null)
          frame.setValue(count);
        if (rez == null) {
          log.warn("Zapis ID=" + id + " je prazan.");
          continue;
        }
        Record rec = null;
        try {
          rec = RecordFactory.fromUNIMARC(0, rez);
        } catch (Exception ex) {
          log.warn("Parsiranje zapisa ID=" + id + " je puklo.");
          log.warn(ex);
          continue;
        }
        String RN = rec.getSubfieldContent("001e");
        if (RN == null || RN.length() == 0) {
          fixRecord(conn, rec, id);
          updateRecord(conn, update, insert1, insert2, rec, id);
        }
      }
      rset.close();
      stmt.close();
      update.close();
      insert1.close();
      insert2.close();
      conn.close();
    } catch (Exception ex) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      ex.printStackTrace(pw);
      log.fatal(sw.toString());
    } finally {
      if (frame != null)
        frame.enableGUI();
    }
  }
  
  public void updateRecord(Connection conn, PreparedStatement update, 
      PreparedStatement insert1, PreparedStatement insert2, Record rec, int id) {
    String RN = rec.getSubfieldContent("001e");
    String rez = null;
    try {
      rez = RecordFactory.toUNIMARC(0, rec);
    } catch (Exception ex) {
      log.warn("Serijalizacija zapisa ID=" + id + " je pukla.");
      log.warn(ex);
      return;
    }
    try {
      update.clearParameters();
      update.setString(1, rez);
      update.setInt(2, id);
      update.executeUpdate();
      insert1.clearParameters();
      insert1.setInt(1, id);
      insert1.setString(2, RN);
      insert1.executeUpdate();
      insert2.clearParameters();
      insert2.setInt(1, id);
      insert2.setString(2, RN);
      insert2.executeUpdate();
      conn.commit();
    } catch (Exception ex) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      ex.printStackTrace(pw);
      log.fatal(sw.toString());
    }
  }

  public void fixRecord(Connection conn, Record rec, int id) {
    int rn = getRN(conn);
    if (rn == 0)
      return;
    log.info("Zapis ID=" + id + " nema RN, sada dobija RN="+rn);
    Field f001 = rec.getField("001");
    if (f001 == null) {
      f001 = new Field("001");
      rec.add(f001);
    }
    Subfield sf001e = f001.getSubfield('e');
    if (sf001e == null) {
      sf001e = new Subfield('e');
      f001.add(sf001e);
    }
    sf001e.setContent(Integer.toString(rn));
  }
  
  public int getRN(Connection conn) {
    int RN = 0;
    try {
      Statement stmt = conn.createStatement();
      ResultSet rset = stmt.executeQuery(
          "SELECT counter_value FROM misc_counters " +
          "WHERE counter_name='RN' FOR UPDATE");
      if (rset.next()) {
        RN = rset.getInt(1) + 1;
        stmt.executeUpdate(
            "UPDATE misc_counters SET counter_value=counter_value+1 " +
            "WHERE counter_name='RN'");
      } else {
        RN = 0;
      }
      rset.close();
      stmt.close();
      conn.commit();
    } catch (Exception ex) {
      log.fatal("Greska kod dobijanja novog RN broja.");
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      ex.printStackTrace(pw);
      log.fatal(sw.toString());
    }
    return RN;
  }

  private static Log log = LogFactory.getLog(FixTask.class.getName());
}
