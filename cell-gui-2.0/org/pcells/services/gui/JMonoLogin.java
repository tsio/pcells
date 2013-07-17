// $Id: JMonoLogin.java,v 1.10 2007/02/15 08:18:12 cvs Exp $
//
package org.pcells.services.gui ;

import  dmg.util.*;
//
import java.awt.*;
import java.awt.event.*;
import java.awt.font.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.util.*;
import java.io.* ;
import java.net.*;
import java.lang.reflect.*;
import java.util.prefs.* ;
import org.pcells.services.connection.DomainConnection ;
import org.pcells.services.connection.DomainConnectionListener ;
import org.pcells.services.connection.DomainEventListener ;
import org.pcells.services.connection.DomainConnectionAdapter ;
import org.pcells.services.connection.Ssh1DomainConnection ;
import org.pcells.services.connection.RawDomainConnection ;
import org.pcells.util.CellGuiClassLoader ;
import org.pcells.util.ModuleClassLoader ;

public class JMonoLogin extends CellGuiSkinHelper.CellPanel {

   private String            _name        = null ;
   private JPanel            _loginPanel  = null ;
   private UserPasswordPanel _userPasswordPanel = null ;
   private Switchboard       _switchboard = new Switchboard() ;
   private JMLSetupPanel     _setupPanel  = null ;
   private Preferences       _preferences = null ;
   private String            _protocol    = null ;
   private JTabbedPane       _tab         = new JTabbedPane() ;
   private JPanel            _setup       = null ;
   
   public JMonoLogin( String name , Preferences node ){
   
      _name        = name ;
      _preferences = node ;
      
      setBorder( new CellGuiSkinHelper.CellBorder( _name , 25 ) ) ;
      
      setLayout( new BorderLayout( 10 , 10 ) ) ;
      
      _setupPanel = new JMLSetupPanel( _name , _preferences ) ;
      _setupPanel.addActionListener(_switchboard) ;

      _setup = new CenterPanel( _setupPanel ) ;

      _loginPanel = new Login() ;
      
      displayLoginPanel() ;
   
   }
   public void paintComponent( Graphics gin ){
      //System.err.println("Paint component for : "+this);
	CellGuiSkinHelper.paintComponentBackground(gin,this);
	super.paintComponent(gin);
   }
   public class CenterPanel extends CellGuiSkinHelper.CellPanel {
      public CenterPanel( JPanel panel ){
         GridBagConstraints c = new GridBagConstraints()  ;
         c.gridwidth  = 1 ;
         c.gridheight = 1 ;
         c.insets     = new Insets(2,2,2,2) ;
         setLayout( new GridBagLayout() ) ;
         add( _setupPanel , c ) ; 
      }
   }
   private void remark( String message ){
      _userPasswordPanel._statusLabel.setText(message);
   }
   private void displayLoginPanel(){
      Preferences addr = _preferences.node("Addresses") ;
      _userPasswordPanel._login.setText(addr.get("loginname","admin"));
      _protocol = addr.get("protocol","ssh1") ;
      if( _protocol.equals("raw") ){
         _userPasswordPanel._login.setText("admin");
         _userPasswordPanel._login.setEnabled(false);
         _userPasswordPanel._passwd.setEnabled(false);
      }else if( _protocol.equals("ssh1") ){
         _userPasswordPanel._login.setEnabled(true);
         _userPasswordPanel._passwd.setEnabled(true);
      } 
      removeAll();
      add( _loginPanel , "Center" ) ;
      validate() ;
      repaint();
   }
   private void displaySetupPanel(){
      removeAll();
      add( _setup , "Center" ) ;
      validate() ;
      repaint();
   }
   private void displayTabPanel(){
      removeAll();
      add( _tab , "Center" ) ;
      validate() ;
      repaint();
   }
   private class Switchboard implements ActionListener, DomainEventListener {
      public void actionPerformed( ActionEvent event ){
         Object source = event.getSource() ;
//         System.out.println("Event : "+source.getClass().getName());
//         System.out.println("Event : "+event.getActionCommand());
         if( ( source == _userPasswordPanel._loginButton ) ||
             ( source == _userPasswordPanel._passwd      )    ) {
            new Thread(
                new Runnable(){
                   public void run(){
                      try{
                        remark("");
                        tryLogin() ;
                      }catch(Exception ee ){
                         System.err.println("tryLogin reported : "+ee ) ;
                         ee.printStackTrace();
                         if( ee instanceof ConnectException ){
                            remark(ee.getMessage());
                         }else{
                            remark("Login Failed");
                         }
                      }
                   }
                }
            ).start() ;
         }else if( source == _userPasswordPanel._setupButton ){
            displaySetupPanel();
         }else if( source == _setupPanel ){
            displayLoginPanel() ;
         }
      }
      public void connectionOpened( DomainConnection connection ){
         System.out.println("DomainConnection : connectionOpened");
         try{
            preparePanelModules() ;
            displayTabPanel();
         }catch(Exception ee ){
            System.err.println("preparePanelModule reported : "+ee );
            ee.printStackTrace();
         }
      }
      public void connectionClosed( DomainConnection connection ){
         System.out.println("DomainConnection : connectionClosed");
         displayLoginPanel();
      }
      public void connectionOutOfBand( DomainConnection connection ,
                                       Object subject                ){
         System.out.println("DomainConnection : connectionOutOfBand");
      }

   }
   private void preparePanelModules() throws Exception {
   
      Preferences modules = _preferences.node("Modules") ;
      String []  children = modules.childrenNames() ;
      _tab.removeAll();
      
      ClassLoader loader = this.getClass().getClassLoader() ;
      
      if( ! ( loader instanceof  ModuleClassLoader ) ){
         String errorMessage = "JMonoLogin needs the ModuleClassLoader, but only got : "+loader.getClass().getName() ;
         System.err.println( errorMessage );
         throw new
         IllegalArgumentException(errorMessage);
      }
      for( Iterator it = ((ModuleClassLoader)loader).modules() ; it.hasNext() ; ){
      
         CellGuiClassLoader.GuiJarModuleEntry entry = (CellGuiClassLoader.GuiJarModuleEntry)it.next() ;
         String className   = entry.getClassName() ;
         String moduleName  = entry.getModuleName() ;

         
         System.out.println("Module :  name : "+moduleName+" class "+className);
         CellGuiSkinHelper.setComponentProperties( _tab ) ;
         
         Class domainConnectionClass = loader.loadClass( "org.pcells.services.connection.DomainConnection" ) ;
         
         try{
         
            Class  [] classArgs  = { domainConnectionClass , // org.pcells.services.connection.DomainConnection.class ,
                                     java.util.prefs.Preferences.class } ;
            Object [] objectArgs = { _domainConnection  , modules.node(moduleName) } ;
         
            Class       cn = loader.loadClass( className ) ;
            Constructor cc = cn.getConstructor( classArgs ) ;
            JPanel      cp = (JPanel) cc.newInstance( objectArgs ) ;
            
            CellGuiSkinHelper.setComponentProperties( cp ) ;
            
            _tab.addTab( "   "+moduleName+"   " , cp ) ;
         
         }catch(Exception ee ){
         
         
            try{
                 Class  [] classArgs  = { domainConnectionClass } ;
                 Object [] objectArgs = { _domainConnection } ;

                 Class       cn = loader.loadClass( className ) ;
                 Constructor cc = cn.getConstructor( classArgs ) ;
                 JPanel      cp = (JPanel) cc.newInstance( objectArgs ) ;

                 _tab.addTab( "   "+moduleName+"   " , cp ) ;
            
            }catch( Exception eee ){
               System.err.println("Failed to create "+moduleName ) ;
               ee.printStackTrace();
               continue ;
            
            }        
         }
      }
   
   }
   private void preparePanelModulesX() throws Exception {
   
      Preferences modules = _preferences.node("Modules") ;
      String []  children = modules.childrenNames() ;
      _tab.removeAll();
      for( int i = 0 , n = children.length ; i < n ; i++ ){
      
         Preferences module = modules.node(children[i]) ;
         String className   = module.get("class",null);
         String name        = module.get("name",null) ;

         
         System.out.println("Module : "+module.name()+" name : "+name+" class "+className);
         if( ( name == null ) || ( className == null ) )continue ;
         
         try{
         
            Class  [] classArgs  = { org.pcells.services.connection.DomainConnection.class ,
                                     java.util.prefs.Preferences.class } ;
            Object [] objectArgs = { _domainConnection  , module } ;
         
            Class       cn = Class.forName( className ) ;
            Constructor cc = cn.getConstructor( classArgs ) ;
            JPanel      cp = (JPanel) cc.newInstance( objectArgs ) ;

            _tab.addTab( "   "+name+"   " , cp ) ;
         
         }catch(Exception ee ){
         
         
            try{
                 Class  [] classArgs  = { org.pcells.services.connection.DomainConnection.class } ;
                 Object [] objectArgs = { _domainConnection } ;

                 Class       cn = Class.forName( className ) ;
                 Constructor cc = cn.getConstructor( classArgs ) ;
                 JPanel      cp = (JPanel) cc.newInstance( objectArgs ) ;

                 _tab.addTab( "   "+name+"   " , cp ) ;
            
            }catch( Exception eee ){
               System.err.println("Failed to create "+name ) ;
               ee.printStackTrace();
            
            }        
         }
      }
   
   }
   public void close(){
      if( _domainConnection == null )return ;
      try{
         _domainConnection.sendObject("logoff",null,0);
      }catch(Exception e ){
         System.err.println("Problem in closeing : "+e);
         e.printStackTrace();
      }
      /*
      try{
         Method method = _domainConnection.getClass().getMethod("close",null);
         method.invoke(_domainConnection,null);
         System.out.println("'close' invoked");
      }catch(Exception e ){
         System.err.println("Problem in closeing : "+e);
         e.printStackTrace();
      }
      */
   }
   private void tryLogin() throws Exception {
     System.out.println("Trying login") ;
     if( _protocol == null )return ;
     
     if( _protocol.equals("raw") ){
         System.out.println("Trying login (raw)") ;
     
         Preferences addr = _preferences.node("Addresses") ;
         String nodename  = addr.get("hostname","localhost");
         String port      = addr.get("portnumber","22223");
         int portnumber   = Integer.parseInt(port);
         System.out.println("Connecting to "+nodename+":"+portnumber);
         RawDomainConnection connection = new RawDomainConnection(nodename,portnumber);
         System.out.println("Connected to "+nodename+":"+portnumber);
         _domainConnection = connection ;
         connection.addDomainEventListener(_switchboard);
         connection.go() ;
         
     }else if( _protocol.equals("ssh1") ){
         System.out.println("Trying login (raw)") ;
     
         Preferences addr = _preferences.node("Addresses") ;
         String nodename  = addr.get("hostname","localhost");
         String port      = addr.get("portnumber","22223");
         String loginname = _userPasswordPanel._login.getText() ;
         char [] pw       = _userPasswordPanel._passwd.getPassword() ;
         String password  = "" ;
         if( pw != null )password = new String(pw);
//         String loginname = addr.get("loginname","admin");
//         String password  = addr.get("password","dickerelch");
         int portnumber   = Integer.parseInt(port);
         System.out.println("Connecting to "+nodename+":"+portnumber);
         Ssh1DomainConnection connection = new Ssh1DomainConnection(nodename,portnumber);
         connection.setLoginName(loginname);
         connection.setPassword(password);
         String userHome = System.getProperties().getProperty("user.home");
         if( userHome != null ){
//            File identity = new File(userHome,".ssh/identity" ) ;
            File identity = new File(userHome,".ssh"+File.separator+"identity" ) ;
                  System.out.println("Setting identity file to : "+identity);
            if( identity.exists() ){
               try{
                  System.out.println("Setting identity file to : "+identity);
                  connection.setIdentityFile( identity );
               }catch(Exception ee){
                  System.err.println("Problems reading : "+identity);
               }
            }
         }
         System.out.println("Connected to "+nodename+":"+portnumber);
         _domainConnection = connection ;
         connection.addDomainEventListener(_switchboard);
         connection.go() ;
     
     }
   
   }
   private DomainConnection _domainConnection = null ;

   private class Login extends CellGuiSkinHelper.CellPanel {
      private Login(){

         setLayout( new GridBagLayout() ) ;

         JPanel center = new CellGuiSkinHelper.CellPanel( new BorderLayout( 10 , 10 ) ) ;

         _userPasswordPanel = new UserPasswordPanel() ;
         CellGuiSkinHelper.setComponentProperties( _userPasswordPanel ) ;
         
         center.add( _userPasswordPanel = new UserPasswordPanel() , "Center" ) ;
         
         URL imageUrl = null ;
         Icon    icon = null ;
         
         if( _name != null )
            imageUrl = getClass().getResource("/images/"+_name+"-logo.jpg") ;
            
         if( imageUrl == null )
            imageUrl = getClass().getResource("/images/cells-logo.jpg");
         
         if( imageUrl == null ){
             icon = (Icon)new CellIcon( 80 , 80 ) ;
         }else{
             ImageIcon iicon = new ImageIcon(imageUrl) ;
             Image im    = iicon.getImage() ;
             im = im.getScaledInstance( 80 , -1 , Image.SCALE_SMOOTH ) ;
             icon = (Icon)new ImageIcon(im);
         }
         JPanel iconpanel = new IconDisplayPanel( icon ) ;

         center.add( iconpanel , "West" ) ;
         GridBagConstraints c = new GridBagConstraints()  ;
         c.gridwidth  = 1 ;
         c.gridheight = 1 ;
         c.insets     = new Insets(2,2,2,2) ;
         add( center , c ) ;
         center.setBorder(
              BorderFactory.createBevelBorder(
                         BevelBorder.LOWERED )
          //               center.getBackground(),
            //             Color.green)
                         );
                         
      }
   
   }
   public void paintComponent2( Graphics gin ){
      Graphics2D g = (Graphics2D) gin ;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                         RenderingHints.VALUE_ANTIALIAS_ON);
      super.paintComponent(g);
   }

   private class UserPasswordPanel extends CellGuiSkinHelper.CellPanel {
   
      private JLabel _loginLabel     = new JLabel( "Login Name" , JLabel.RIGHT ) ;
      private JLabel _passwordLabel  = new JLabel( "Password" , JLabel.RIGHT ) ;
      private JTextField     _login  = new JTextField(20) ;
      private JPasswordField _passwd = new JPasswordField(20) ;
      private JLabel    _statusLabel = new JLabel(" ",JLabel.CENTER) ;
      private JLabel    _headerLabel = new JLabel("Cell Login" , JLabel.CENTER ) ;
      private JButton   _loginButton = new CellGuiSkinHelper.CellButton("Login");
      private JButton   _setupButton = new CellGuiSkinHelper.CellButton("Setup");
      public Insets getInsets(){ return new Insets(10,10,10,10);}
      
      private UserPasswordPanel(){
         setLayout( new GridBagLayout() ) ;
         GridBagConstraints c = new GridBagConstraints()  ;
         
         CellGuiSkinHelper.setComponentProperties( _headerLabel ) ;
         CellGuiSkinHelper.setComponentProperties( _loginLabel ) ;
         CellGuiSkinHelper.setComponentProperties( _passwordLabel ) ;
         
         c.gridwidth  = 1 ;
         c.gridheight = 1 ;
         c.insets     = new Insets(2,2,2,2) ;
         
         _headerLabel.setFont( new Font( "Courier" , Font.ITALIC | Font.BOLD, 24 ) ) ;
         
         c.gridx = 0 ; c.gridy = 0 ;
         c.gridwidth = 2 ;
         c.fill = GridBagConstraints.HORIZONTAL ;
         add( _headerLabel , c ) ;

         c.gridwidth = 1 ;

         c.gridx = 0 ; c.gridy = 1 ; c.weightx  =  0.0 ;
         c.fill       = GridBagConstraints.NONE ;
         add( _loginLabel , c ) ; 
         c.gridx = 1 ; c.gridy = 1 ; c.weightx  =  1.0 ;
         c.fill       = GridBagConstraints.HORIZONTAL ;
         add( _login , c ) ;
         c.gridx = 0 ; c.gridy = 2 ; c.weightx  =  0.0 ;
         c.fill       = GridBagConstraints.NONE ;
         add( _passwordLabel , c ) ;
         c.gridx = 1 ; c.gridy = 2 ; c.weightx  =  1.0 ;
         c.fill       = GridBagConstraints.HORIZONTAL ;
         add( _passwd , c ) ;
         
         c.gridx = 0 ; c.gridy = 3 ;
         c.gridwidth = 2 ;
         c.fill = GridBagConstraints.HORIZONTAL ;
         _statusLabel.setForeground(Color.red) ;
         add( _statusLabel , c ) ;


         c.gridwidth = 1 ;
         c.gridx = 0 ; c.gridy = 4 ; // c.weightx  =  0.0 ;
         c.fill       = GridBagConstraints.NONE ;
         add( _loginButton , c ) ; 
         c.gridx = 1 ; c.gridy = 4 ; // c.weightx  =  1.0 ;
         c.anchor = GridBagConstraints.EAST ;
         //c.fill       = GridBagConstraints.HORIZONTAL ;
         add( _setupButton , c ) ;
         
         _passwd.setEchoChar( '*' ) ;
         _loginButton.addActionListener(_switchboard);
         _setupButton.addActionListener(_switchboard);
         _passwd.addActionListener(_switchboard);
         _login.requestFocus() ;
      }
      public void paintComponent( Graphics gin ){
         Graphics2D g = (Graphics2D) gin ;
         g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                            RenderingHints.VALUE_ANTIALIAS_ON);
         super.paintComponent(g);
      }
   }
   private class IconDisplayPanel extends CellGuiSkinHelper.CellPanel {
       private Icon _icon = null ;
       public IconDisplayPanel( Icon icon ){ 
          _icon = icon ;
       }
       public Dimension getPreferredSize(){
         return new Dimension( _icon.getIconWidth() , _icon.getIconHeight() );
       }
       public void paintComponent( Graphics g ){
          Dimension d = getSize() ;
          int x = ( d.width  - _icon.getIconWidth() ) / 2 ;
          int y = ( d.height - _icon.getIconHeight()) / 2 ;
          _icon.paintIcon( this , g , x , y ) ;
       }
   }
   private class CellIcon implements Icon {
      private int _height = 0 ;
      private int _width  = 0 ;
      private CellIcon( int width , int height ){
         _height = height ;
         _width  = width ;
      }
      public void paintIcon( Component c , Graphics gin , int xi , int yi ){
         Graphics2D g = (Graphics2D) gin ;

         g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                            RenderingHints.VALUE_ANTIALIAS_ON);
         g.setColor( c.getBackground() ) ;
         g.fillRect(  xi , yi , _width - 1 , _height - 1 ) ;
         int x = xi + 4 ;
         int y = yi + 4 ;
         int width = _width - 8 ;
         int height = _height - 8 ;
         
         Color col = new Color( 0 , 0 , 255 ) ;
         
         while( width > 0 ){
            g.setColor( col ) ;
            width = width / 2 ; height = height / 2 ;
            g.fillOval( x , y , width , height ) ;
            x = x + width  ; y = y + height   ;
            col = col.brighter() ;
         }
       }
      public int getIconWidth(){ return _height ; }
      public int getIconHeight(){ return _width ; }
   }

}


