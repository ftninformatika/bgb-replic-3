package com.gint.app.bisis.replication.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SqlFile {
  
  public SqlFile(URL url) {
    statements.clear();
    try {
      BufferedReader in  = new BufferedReader(
          new InputStreamReader(url.openStream(), "UTF8"));
      String line = "";
      String accumulated = "";
      while ((line = in.readLine()) != null) {
        line = line.trim();
        if (line.length() == 0)
          continue;
        accumulated += " " + line;
        if (line.endsWith(";")) {
          statements.add(accumulated.substring(0, accumulated.length() - 1));
          accumulated = "";
        }
      }
      in.close();
    } catch (Exception ex) {
      log.fatal(ex);
    }
  }
  
  public List getStatements() {
    return statements;
  }
  
  private List statements = new ArrayList();
  private static Log log = LogFactory.getLog(SqlFile.class.getName());
}
