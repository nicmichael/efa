/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.util;

import java.io.*;

// @i18n complete

public class EfaFileFilter extends javax.swing.filechooser.FileFilter {

  String description = "";
  String ext1 = "";
  String ext2 = "";
  String ext3 = "";
  int anz=0;

  public EfaFileFilter(String descr, String ext) {
    description = descr;
    ext1 = ext.toUpperCase();
    anz=1;
  }

  public EfaFileFilter(String descr, String ext1, String ext2) {
	    description = descr;
	    this.ext1 = ext1.toUpperCase();
	    this.ext2 = ext2.toUpperCase();
	    anz=2;
	  }

  public EfaFileFilter(String descr, String ext1, String ext2, String ext3) {
	    description = descr;
	    this.ext1 = ext1.toUpperCase();
	    this.ext2 = ext2.toUpperCase();
	    this.ext3 = ext3.toUpperCase();
	    anz=3;
	  }

  public boolean accept (File f) {
    return f.isDirectory() || f.getName().toUpperCase().endsWith(ext1) || (anz == 2 && f.getName().toUpperCase().endsWith(ext2)) || (anz == 3 && f.getName().toUpperCase().endsWith(ext3));
  }

  public String getDescription() {
    return description;
  }
}
