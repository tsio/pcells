// $Id: CellBorder.java,v 1.4 2005/04/17 22:03:55 cvs Exp $
//
package org.pcells.services.gui ;
//
import java.awt.*;
import java.awt.event.*;
import java.awt.font.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;


public class CellBorder extends CellGuiSkinHelper.CellBorder implements Border {

   public CellBorder( String title , int height ){
      super(title,height);
   }
}
