package com.gint.app.bisis.replication.fixrn2;

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
      log.info("Otvorena veza sa sekundarnom bazom.");
      Statement stmt = conn.createStatement();
      ResultSet rset = stmt.executeQuery("SELECT count(*) FROM documents");
      rset.next();
      int recordCount = rset.getInt(1);
      rset.close();
      stmt.close();
      if (frame != null)
        frame.setMaximum(recordCount);
      log.info("Ukupno " + recordCount + " zapisa u bazi.");
      
      PreparedStatement updateRez = conn.prepareStatement(
          "UPDATE documents SET document=? WHERE doc_id=?");
      PreparedStatement updatePref = conn.prepareStatement(
          "UPDATE Prefix_contents SET content=? WHERE doc_id=? AND pref_name=?");
      PreparedStatement updateRN = conn.prepareStatement(
          "UPDATE pref_RN SET content=? WHERE doc_id=?");
      PreparedStatement updateMR = conn.prepareStatement(
          "UPDATE pref_MR SET content=? WHERE doc_id=?");
      
      stmt = conn.createStatement();
      rset = stmt.executeQuery("SELECT doc_id, document FROM documents");
      for (int count = 1; rset.next(); count++) {
        int id = rset.getInt(1);
        String rez = rset.getString(2);
        if (frame != null)
          frame.setValue(count);
        if (rez == null || rez.length() == 0) {
          log.warn("Zapis ID="+id+" je prazan.");
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
        
        boolean RNchanged = false;
        String RN = rec.getSubfieldContent("001e");
        if (RN == null || RN.length() == 0)
          continue;
        String RN2 = trimZeros(RN);
        RNchanged = !RN2.equals(RN);
        if (RNchanged) {
          Subfield sf001e = rec.getSubfield("001e");
          sf001e.setContent(RN2);
        }
        
        boolean MRchanged = false;
        String MR = rec.getSubfieldContent("4741");
        String MR2 = null;
        if (MR != null && MR.length() > 0) {
          MR2 = trimZeros(MR);
          MRchanged = !MR2.equals(MR);
          if (MRchanged) {
            Subfield sf4741 = rec.getSubfield("4741");
            sf4741.setContent(MR2);
          }
        }
        
        if (RNchanged || MRchanged) {
          updateRez.clearParameters();
          updateRez.setString(1, RecordFactory.toUNIMARC(0, rec));
          updateRez.setInt(2, id);
          updateRez.executeUpdate();
          
          if (RNchanged) {
            updatePref.clearParameters();
            updatePref.setString(1, RN2);
            updatePref.setInt(2, id);
            updatePref.setString(3, "RN");
            updatePref.executeUpdate();
            
            updateRN.clearParameters();
            updateRN.setString(1, RN2);
            updateRN.setInt(2, id);
            updateRN.executeUpdate();
          }
          if (MRchanged) {
            updatePref.clearParameters();
            updatePref.setString(1, MR2);
            updatePref.setInt(2, id);
            updatePref.setString(3, "MR");
            updatePref.executeUpdate();
            
            updateMR.clearParameters();
            updateMR.setString(1, MR2);
            updateMR.setInt(2, id);
            updateMR.executeUpdate();
          }
          conn.commit();
        }
      }
      rset.close();
      stmt.close();
      updateRez.close();
      updatePref.close();
      updateRN.close();
      updateMR.close();
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
  
  private String trimZeros(String s) {
    try {
      int i = Integer.parseInt(s);
      return Integer.toString(i);
    } catch (Exception ex) {
      log.warn("Skidanje nula neuspesno za broj: " + s);
      log.warn(ex);
      return s;
    }
  }

  private static Log log = LogFactory.getLog(FixTask.class.getName());
}