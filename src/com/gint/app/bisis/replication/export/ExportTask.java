package com.gint.app.bisis.replication.export;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gint.app.bisis.common.records.Field;
import com.gint.app.bisis.common.records.Record;
import com.gint.app.bisis.common.records.RecordFactory;
import com.gint.app.bisis.replication.util.INIFile;
import com.gint.app.bisis.replication.util.ZipUtils;
import com.gint.util.file.FileUtils;

public class ExportTask {
  
  public ExportTask(ExportFrame frame) {
    File config = new File(FileUtils.getClassDir(getClass()) + "/export.ini");
    if (!config.isFile()) {
      log.fatal("Ne postoji konfiguracioni fajl: " + config.getAbsolutePath());
      return;
    }
    INIFile ini = new INIFile(FileUtils.getClassDir(getClass()) + "/export.ini");
    String driver = ini.getString("database", "driver");
    String url = ini.getString("database", "url");
    String username = ini.getString("database", "username");
    String password = ini.getString("database", "password");
    String destination = ini.getString("export", "destination");
    String fileName = ini.getString("export", "filename");
    boolean incremental = ini.getBoolean("export", "incremental");
    Date today = new Date();
    Date startDate = null;
    
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
    File file = new File(dir, fileName);
    if (file.exists()) {
      file.delete();
      log.info("Fajl " + fileName + 
          " vec postoji, prethodna verzija je obrisana.");
    }
    File recordsFile = new File(dir, "records.dat");
    if (recordsFile.exists())
      recordsFile.delete();
    File infoFile = new File(dir, "export.info");
    if (infoFile.exists())
      infoFile.delete();

    int totalRecords = 0;
    try {
      RandomAccessFile out = new RandomAccessFile(recordsFile, "rw");
      Class.forName(driver);
      log.info("Ucitan drajver za bazu.");
      Connection conn = DriverManager.getConnection(url, username, password);
      conn.setAutoCommit(false);
      log.info("Uspostavljena veza sa bazom.");
      if (incremental) {
        Statement stmt = conn.createStatement();
        ResultSet rset = stmt.executeQuery(
            "SELECT max(export_date) FROM export_history");
        if (rset.next()) {
          startDate = rset.getDate(1);
          if (startDate != null)
            log.info("Inkrementalni eksport od datuma: " + sdf.format(startDate));
          else {
            incremental = false;
            log.info("Nema ranijeg eksporta, radi se puni eksport.");
          }
        } else {
          incremental = false;
          log.info("Nema ranijeg eksporta, radi se puni eksport.");
        }
        rset.close();
        stmt.close();
      }
      PreparedStatement pstmt = conn.prepareStatement(
          "INSERT INTO export_history (export_date, export_type) VALUES (?,?)");
      pstmt.setDate(1, new java.sql.Date(today.getTime()));
      pstmt.setString(2, incremental ? "I" : "F");
      try {
        pstmt.executeUpdate();
        conn.commit();
      } catch (Exception ex) {
        log.fatal(ex);
        log.fatal("Danasnji eksport je vec napravljen?");
        return;
      } finally {
        pstmt.close();
      }
      
      Statement stmt = conn.createStatement();
      ResultSet rset = stmt.executeQuery("SELECT count(*) FROM documents");
      rset.next();
      totalRecords = rset.getInt(1);
      rset.close();
      stmt.close();
      log.info("Ukupno " + totalRecords + " zapisa za obradu.");
      if (frame != null)
        frame.setMaximum(totalRecords);
      stmt = conn.createStatement();
      rset = stmt.executeQuery("SELECT doc_id, document FROM documents");
      int count = 0;
      int recordsProcessed = 0;
      while (rset.next()) {
        count++;
        if (frame != null)
          frame.setValue(count);
        int docid = rset.getInt(1);
        String rez = rset.getString(2);
        if (rez == null) {
          log.warn("Zapis ID: " + docid + " je prazan.");
          continue;
        }
        rez = rez.replace('\n',' ').replace('\r',' ').replace('\t',' ');
        Record rec = null;
        try {
          rec = RecordFactory.fromUNIMARC(0, rez);
        } catch (Exception ex) {
          log.warn("Parsiranje zapisa ID " + docid + " je puklo.");
          continue;
        }
        if (incremental) {
          String dates = rec.getSubfieldContent("000b");
          if (dates == null || dates.length() < 16) {
            log.warn("Zapis ID " + docid + ": neispravno polje 000b.");
            continue;
          }
          String sLastModified = dates.substring(8);
          if ("00000000".equals(sLastModified))
            continue;
          Date lastModified = null;
          try {
            lastModified = intern.parse(sLastModified);
          } catch (Exception ex) {
            log.warn("Zapis ID: " + docid + ": neispravan datum u polju 000b.");
            continue;
          }
          if (lastModified.before(startDate))
            continue;
        }
        recordsProcessed++;
        Iterator it = rec.getFields().iterator();
        while (it.hasNext()) {
          Field f = (Field)it.next();
          if (f.getName().startsWith("99"))
            it.remove();
        }
        rec.pack();
        out.writeBytes(RecordFactory.toUNIMARC(0, rec));
        out.writeBytes("\n");
      }
      rset.close();
      stmt.close();
      conn.close();
      out.close();
      log.info("Eksport zavrsen.");
      
      ExportInfo exportInfo = new ExportInfo();
      exportInfo.setExportDate(today);
      exportInfo.setExportType(incremental ? ExportInfo.TYPE_INCREMENTAL : ExportInfo.TYPE_FULL);
      if (incremental)
        exportInfo.setStartDate(startDate);
      exportInfo.setRecordCount(recordsProcessed);
      BufferedWriter info = new BufferedWriter(new OutputStreamWriter(
          new FileOutputStream(infoFile), "UTF8"));
      info.write(exportInfo.toString());
      info.close();
      
      ZipUtils.zip(file, new File[] { recordsFile, infoFile} );
      recordsFile.delete();
      infoFile.delete();
      log.info("Fajl je kompresovan.");
    } catch (Exception ex) {
      log.fatal(ex);
    } finally {
      if (frame != null)
        frame.enableGUI();
    }
  }
  
  private SimpleDateFormat intern = new SimpleDateFormat("yyyyMMdd");
  private SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy.");
  private static Log log = LogFactory.getLog(ExportTask.class.getName());
}
