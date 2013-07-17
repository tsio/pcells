package org.pcells.services.gui.util ;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;


public interface PieChartModel extends ListModel {

    public interface PieChartItem {
        public Color getColor() ;
        public long  getLongValue() ;
    }
    
}
