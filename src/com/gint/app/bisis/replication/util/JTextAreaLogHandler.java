package com.gint.app.bisis.replication.util;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import javax.swing.JTextArea;

public class JTextAreaLogHandler extends Handler {
  
  public JTextAreaLogHandler() {
  }
  
  public JTextAreaLogHandler(JTextArea destination) {
    this.destination = destination;
  }
  
  public void flush() {
    if (destination != null && buffer.size() > 0) {
      for (int i = 0; i < buffer.size(); i++)
        append((LogRecord)buffer.get(i));
      buffer.clear();
    }
  }
  public void close() { }
  public void publish(LogRecord record) {
    if (destination == null) {
      buffer.add(record);
      return;
    }
    flush();
    append(record);
  }
  
  public void setDestination(JTextArea destination) {
    this.destination = destination;
  }
  
  private void append(LogRecord record) {
    String message = format.format(record);
    destination.append(message);
  }
  
  private JTextArea destination;
  private SimpleFormatter format = new SimpleFormatter();
  private List buffer = new ArrayList();
}
