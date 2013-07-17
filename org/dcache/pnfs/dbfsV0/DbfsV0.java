// $Id: DbfsV0.java,v 1.2 2004/06/21 22:30:59 cvs Exp $

package org.dcache.pnfs.dbfsV0 ;

import java.sql.* ;
import java.util.* ;
import java.io.*;

class DbfsV0 {

   private static final String __createNextPnfsIdTable =
     "CREATE TABLE nextpnfsid ( mode CHAR(8) , high INTEGER , low BIGINT )" ;
     
   private static final String __createInodesTable =
     "CREATE TABLE inodes ( "+
     "id_high     INTEGER , id_low     BIGINT , "+
     "type INTEGER , "+
     "parent_high INTEGER , parent_low BIGINT , "+
     "created TIMESTAMP , "+
     "PRIMARY KEY ( id_high , id_low ) )" ;
     
   private static final String __createDirInodesTable =
     "CREATE TABLE dirInodes ( "+
     "id_high    INTEGER , id_low   BIGINT , "+
     "tags_high  INTEGER , tags_low BIGINT , "+
     "modified TIMESTAMP , "+
     "accessed TIMESTAMP , "+
     "PRIMARY KEY ( id_high , id_low ) )" ;
     
   private static final String __createDirectoryTable = 
     "CREATE TABLE xxx ( "+
     "name VARCHAR(255) , "+
     "id_high  INTEGER , id_low BIGINT , "+
     "flags    INTEGER , "+
     "PRIMARY KEY ( name ) )" ;
     
     
   private static final String __createFileInodesTable =
     "CREATE TABLE fileInodes ( "+
     "id_high    INTEGER , id_low   BIGINT , "+
     "modified TIMESTAMP , "+
     "accessed TIMESTAMP , "+
     "data     BLOB , "+
     "PRIMARY KEY ( id_high , id_low ) )" ;
     
   private static final String __insertDirectory =
    "INSERT INTO "; 
   private static final String __insertInodes =
    "INSERT INTO inodes ( id_high , id_low , parent_high , parent_low , type , created) "+
    "VALUES ( ? , ? , ? , ? , ? , ? )" ;
   private static final String __getInodes =
    "SELECT * FROM inodes where id_high = ? and id_low = ?" ;
   private static final String __insertDirInodes =
    "INSERT INTO dirInodes ( id_high , id_low , modified , accessed ) "+
    "VALUES ( ? , ? , ? , ?  )" ;
   private static final String __insertFileInodes =
    "INSERT INTO fileInodes ( id_high , id_low , modified , accessed ) "+
    "VALUES ( ? , ? , ? , ?  )" ;
   private static final String __getDirInodes =
    "SELECT * FROM dirInodes where id_high = ? and id_low = ?" ;
   private static final String __getFileInodes =
    "SELECT * FROM fileInodes where id_high = ? and id_low = ?" ;
   private static final String __insertInitPnfsId =
     "INSERT INTO nextpnfsid ( mode , high , low ) VALUES ( 'init' ,  0 , 100 )" ;
   private static final String __insertNextPnfsId =
     "INSERT INTO nextpnfsid ( mode , high , low ) VALUES ( 'next' ,  0 , 100 )" ;


   private Connection _connection = null ;
   private LinkedList _pnfsIdPool = new LinkedList() ;
   
   private PreparedStatement _preparedInsertInode     = null ;
   private PreparedStatement _preparedInsertDirInode  = null ;
   private PreparedStatement _preparedInsertFileInode = null ;
   private PreparedStatement _preparedGetFileInode    = null ;
   private PreparedStatement _preparedGetDirInode     = null ;
   private PreparedStatement _preparedGetInode        = null ;
   
   public static final int DIRECTORY = 2 ;
   public static final int DATASET   = 1 ;
   
   public static class DirInode extends ExtendedInode {
      private DbPnfsId _tag = null ;
      private DirInode( Inode inode ){
         super(inode);
      }
      public DirInode( DbPnfsId pnfsId ){
         super(  pnfsId , DIRECTORY ) ;
      }
      public String toString(){
         return "("+super.toString()+"("+_tag+"))" ;
      }
      public String getTableName(){
         return getTableName(getPnfsId()) ;
      }
      public static String getTableName( DbPnfsId pnfsId ){
         return "d_"+pnfsId.getTableName() ;
      }
   }
   public static class FileInode extends ExtendedInode {
      private FileInode( Inode inode ){
         super(inode);
      }
      public FileInode( DbPnfsId pnfsId ){
         super(  pnfsId , DATASET ) ;
      }
      public String toString(){
         return "("+super.toString()+")" ;
      }
   }
   public static class ExtendedInode extends Inode {
   
      protected Timestamp _modified = new Timestamp(new java.util.Date().getTime()); 
      protected Timestamp _accessed = _modified ;
      
      private ExtendedInode( Inode inode ){
         super(inode);
      }
      private ExtendedInode( DbPnfsId pnfsId , int type ){
         super(  pnfsId , type ) ;
      }
      public String toString(){
         return "("+super.toString()+"("+_modified+","+_accessed+"))" ;
      }
       
   }  
   public static class Inode {
      protected DbPnfsId  _pnfsId = null ;
      private DbPnfsId  _parentId = null ;
      private Timestamp _created  = new Timestamp(new java.util.Date().getTime()); 
      private int       _type     = 0 ;
      public Inode( DbPnfsId pnfsId , int type ){ 
          _pnfsId = pnfsId ;
          _type   = type ;
      }
      private Inode( Inode inode ){
         _pnfsId   = inode._pnfsId ;
         _parentId = inode._parentId ;
         _created  = inode._created ;
         _type     = inode._type ;
      }
      public void setParentId( DbPnfsId parentId ){  _parentId = parentId ; }
      public String toString(){
        return "(id="+_pnfsId+";parent="+_parentId+";type="+_type+")";
      }
      public DbPnfsId getPnfsId(){ return _pnfsId ; }
   }
   public class DirectoryEntry {
      private DbPnfsId _id    = null ;
      private String   _name  = null ;
      private int      _flags = 0 ;
      private DirectoryEntry( String name , DbPnfsId pnfsId , int flags ){
         _id    = pnfsId ;
         _name  = name ;
         _flags = flags ;
      }
      public int getFlags(){ return _flags ; }
      public String getName(){ return _name ; }
      public DbPnfsId getPnfsId(){ return _id ; }
      public String toString(){
         return _name+" "+_id+" "+_flags ;
      }
   }
   static public class DbPnfsId {
      private long _low  = 0 ;
      private int  _high = 0 ;
      private DbPnfsId(){};
      public DbPnfsId( String pnfsString )throws IllegalArgumentException {
         int pos = pnfsString.indexOf(':');
         if( pos < 0 ){
            _low = Long.parseLong(pnfsString);
         }else{
            _high = Integer.parseInt( pnfsString.substring(0,pos) ) ;
            _low  = Long.parseLong( pnfsString.substring(pos+1) ) ;
         }
      }
      private DbPnfsId( int high , long low ){
         _low = low ;
         _high = high ;
      }
      public String toString(){
         return "["+_high+":"+_low+"]" ;
      }
      public DbPnfsId nextPnfsId(){
         return new DbPnfsId(_high,_low+8L);
      }
      public String getTableName(){
         return _high+"_"+_low  ;
      }
   }
   public DbfsV0( String driver , String url , String user , String passwd )
       throws Exception {
       
       
       Class.forName(driver);
       _connection = DriverManager.getConnection(url,user,passwd);
   
       init() ;
   }
   public void init() throws SQLException {
   
      DatabaseMetaData md = _connection.getMetaData() ;
      ResultSet tables = null ;
      
      createTableIfNotYetDone( md , "nextpnfsid" , __createNextPnfsIdTable ) ; 
      createTableIfNotYetDone( md , "inodes"     , __createInodesTable ) ; 
      createTableIfNotYetDone( md , "dirInodes"  , __createDirInodesTable ) ; 
      createTableIfNotYetDone( md , "fileInodes" , __createFileInodesTable ) ;

      _preparedInsertInode     = _connection.prepareStatement(__insertInodes);
      _preparedInsertDirInode  = _connection.prepareStatement(__insertDirInodes);
      _preparedInsertFileInode = _connection.prepareStatement(__insertFileInodes);
      _preparedGetInode        = _connection.prepareStatement(__getInodes) ;
      _preparedGetDirInode     = _connection.prepareStatement(__getDirInodes) ;
      _preparedGetFileInode    = _connection.prepareStatement(__getFileInodes) ;
   }
   private void createTableIfNotYetDone( DatabaseMetaData meta ,
                                         String name , 
                                         String createTable )
           throws SQLException {

      ResultSet tables    = meta.getTables(null,"%",name,(String[])null);
      if( ! tables.next() ){
         //
         // create 'nextpnfsid' table
         //
         System.out.println("Creating "+name+" table");
         Statement statement = _connection.createStatement() ;
         statement.execute(createTable) ;
         if( name.equals( "nextpnfsid" ) ){
            statement.executeUpdate(__insertInitPnfsId) ;
            statement.executeUpdate(__insertNextPnfsId) ;
         }
         statement.close();
      }
   
   }
   public Inode createInode( DbPnfsId id , DbPnfsId parent , int type )  throws SQLException {
      Inode obj = new Inode(id,type);
      obj.setParentId( parent == null ? new DbPnfsId() : parent ) ;
      System.out.println("New dbobject : "+obj ) ;
      insertInode(obj);
      return obj ;
   }
   private DirInode getDirectoryInode( DbPnfsId pnfsId )
           throws SQLException, NoSuchElementException {
      
      Inode    inode    = getFromInodeTable(pnfsId) ;
      
      if(  inode._type != DIRECTORY )
         throw new
         NoSuchElementException("Not a directory : "+pnfsId);
         
      DirInode dirInode = new DirInode(inode) ;
           
      return getFromDirInodeTable(dirInode) ;

   }
   private Inode getObjectInode( DbPnfsId pnfsId )
           throws SQLException, NoSuchElementException {
      
      Inode    inode    = getFromInodeTable(pnfsId) ;
      
      if( inode._type == DIRECTORY ){
         
         return getFromDirInodeTable(new DirInode(inode)) ;
         
      }else if(inode._type == DATASET ){
         
         return getFromFileInodeTable(new FileInode(inode)) ;
         
      }else
         throw new
         NoSuchElementException("Not a directory/file inode : "+pnfsId);

   }
   public void removeObject( DbPnfsId pnfsId , String name )
           throws SQLException, NoSuchElementException {

        DirectoryEntry entry = lookup( pnfsId , name ) ;
        if( entry == null )
           throw new
           NoSuchElementException("Entry not found : "+name ) ;
           
        DbPnfsId targetPnfsId = entry.getPnfsId() ;
        
        _removeObject(targetPnfsId) ;
        
        String delete = "DELETE FROM "+DirInode.getTableName(pnfsId)+
                        "  WHERE id_high = "+targetPnfsId._high+
                        "  AND id_low = "+targetPnfsId._low ;


        Statement statement = _connection.createStatement() ;
        statement.executeUpdate(delete) ;
        statement.close();
        
   }
   private void _removeObject( DbPnfsId pnfsId )
           throws SQLException, NoSuchElementException, IllegalStateException {
      
      Inode    inode    = getFromInodeTable(pnfsId) ;
      
      if( inode._type == DIRECTORY ){
         
         String    select    = "SELECT * FROM "+DirInode.getTableName(pnfsId);
         Statement statement = _connection.createStatement() ;
         ResultSet result    = statement.executeQuery(select) ;
         try{
            if( result.next() )
               throw new
               IllegalStateException("Directory not empty");
         }finally{
            statement.close();
         }
         removeFromDirInodeTable(pnfsId) ;
         removeFromInodeTable(pnfsId) ;
         removeDirTable(pnfsId);
         
      }else if(inode._type == DATASET ){
         
         removeFromFileInodeTable(pnfsId) ;
         removeFromInodeTable(pnfsId) ;
         
      }else
         throw new
         NoSuchElementException("Not a directory/file inode : "+pnfsId);

   }
   private void removeFromDirInodeTable( DbPnfsId pnfsId )
           throws SQLException {
           
      String delete = "DELETE FROM dirInodes"+
                      "  WHERE id_high = "+pnfsId._high+
                      "  AND id_low = "+pnfsId._low ;
           

      Statement statement = _connection.createStatement() ;
      statement.executeUpdate(delete) ;
      statement.close();

   }
   private void removeFromFileInodeTable( DbPnfsId pnfsId )
           throws SQLException {
           
      String delete = "DELETE FROM fileInodes "+
                      "  WHERE id_high = "+pnfsId._high+
                      "  AND id_low = "+pnfsId._low ;
           
      Statement statement = _connection.createStatement() ;
      statement.executeUpdate(delete) ;
      statement.close();
           
   }
   private void removeFromInodeTable( DbPnfsId pnfsId )
           throws SQLException {
           
      String delete = "DELETE FROM inodes "+
                      "  WHERE id_high = "+pnfsId._high+
                      "  AND id_low = "+pnfsId._low ;
           
      Statement statement = _connection.createStatement() ;
      statement.executeUpdate(delete) ;
      statement.close();
           
   }
   private void removeDirTable( DbPnfsId pnfsId )
           throws SQLException {
           
      String drop = "DROP TABLE  "+DirInode.getTableName(pnfsId) ;
           
      Statement statement = _connection.createStatement() ;
      statement.executeUpdate(drop) ;
      statement.close();
           
   }
   private void loadInode( Inode inode, ResultSet result ) 
           throws SQLException {

       inode._type     = result.getInt("type") ;
       inode._created  = result.getTimestamp("created") ;
       
       inode._parentId = 
          new DbPnfsId( result.getInt("parent_high") ,
                        result.getInt("parent_low")   ) ;
          
   }
   private void loadDirInode( DirInode inode, ResultSet result ) 
           throws SQLException {

       //
       // directory part
       //
       inode._tag = 
          new DbPnfsId( result.getInt("tags_high") ,
                        result.getInt("tags_low")   ) ;
          
   }
   private void loadFileInode( FileInode inode, ResultSet result ) 
           throws SQLException {

      Blob blob = result.getBlob("data") ;  
      System.out.println("Blob Class : "+blob.getClass().getName());
      System.out.println("Blob OBj   : "+new String(blob.getBytes(1L,(int)blob.length())));
   }
   private void loadExtendedInode( ExtendedInode inode, ResultSet result ) 
           throws SQLException {

       //
       // extended inode
       //
       inode._modified  = result.getTimestamp("modified") ;
       inode._accessed  = result.getTimestamp("accessed") ;
          
   }
   private DirInode getFromDirInodeTable( DirInode inode )
           throws SQLException, NoSuchElementException {
           
           
       _preparedGetDirInode.setInt(1,inode._pnfsId._high) ;
       _preparedGetDirInode.setLong(2,inode._pnfsId._low) ;
       ResultSet result = _preparedGetDirInode.executeQuery() ;
       if( ! result.next() )
          throw new
          NoSuchElementException("Not in dir inode table : "+inode._pnfsId);
           

       loadExtendedInode( inode , result ) ;
       loadDirInode( inode , result ) ;

       return inode ;

   }
   private FileInode getFromFileInodeTable( FileInode inode )
           throws SQLException, NoSuchElementException {
           
           
       _preparedGetFileInode.setInt(1,inode._pnfsId._high) ;
       _preparedGetFileInode.setLong(2,inode._pnfsId._low) ;
       ResultSet result = _preparedGetFileInode.executeQuery() ;
       if( ! result.next() )
          throw new
          NoSuchElementException("Not in file inode table : "+inode._pnfsId);
           

       loadExtendedInode( inode , result ) ;
       loadFileInode( inode , result ) ;

       return inode ;

   }
   private Inode getFromInodeTable( DbPnfsId pnfsId )
           throws SQLException, NoSuchElementException {
           
           
       _preparedGetInode.setInt(1,pnfsId._high) ;
       _preparedGetInode.setLong(2,pnfsId._low) ;
       ResultSet result = _preparedGetInode.executeQuery() ;
       if( ! result.next() )
          throw new
          NoSuchElementException("Not in inode table : "+pnfsId);
          
       
       Inode inode = new Inode(pnfsId,0) ;
       
       loadInode( inode , result ) ;
       
       return inode ;      
   }
   public DirectoryEntry lookup( DbPnfsId dirId , String name )
           throws SQLException {


      String tableName = DirInode.getTableName(dirId) ;
      DatabaseMetaData meta = _connection.getMetaData() ;
      ResultSet tables  = 
          meta.getTables(null,"%",tableName,(String[])null);

      if( ! tables.next() )
         throw new
         NoSuchElementException("Parent directory not found : "+dirId);
           
      String lookup = 
          "SELECT * FROM "+tableName+" where name = '"+name+"'" ;
           
      Statement statement = _connection.createStatement() ;
      ResultSet result    = statement.executeQuery(lookup) ;
      if( ! result.next() )return null ;
      
      DbPnfsId id = new DbPnfsId( result.getInt("id_high") ,
                                  result.getLong("id_low") ) ;
      
      int flags = result.getInt("flags");
      
      
      statement.close();
      
      return new DirectoryEntry(name,id,flags) ;
   }
   public DirInode createRootDirectory() throws SQLException {

      return getRootDirectory()  ;

   }
   public DirInode getRootDirectory() throws SQLException {
 
       
       DatabaseMetaData meta = _connection.getMetaData() ;
       _connection.setAutoCommit(false);
       
       try{
       
          DbPnfsId rootId   = retrievePnfsId("init") ;
          DirInode dirInode = new DirInode(rootId) ;
          ResultSet tables  = 
              meta.getTables(null,"%",dirInode.getTableName(),(String[])null);
              
          if( tables.next() ){
          
             dirInode = getDirectoryInode( retrievePnfsId("init") )  ;
             
          }else{

             dirInode = new DirInode(rootId);
             dirInode.setParentId( new DbPnfsId() ) ;
             insertDirInode( dirInode ) ;
             insertInode( dirInode ) ;
             createDirectoryTable( dirInode.getTableName() ) ;

          }
          
          _connection.commit() ;
          return dirInode ;
          
       }catch(SQLException e){
       
          _connection.rollback() ;
          throw e;
       
       }finally{

          _connection.setAutoCommit(true);
          
       }
   }
   private DirInode _newDirectory( DbPnfsId parent , String name ) throws SQLException {
   
      DbPnfsId dirId = getNextPnfsId() ;

      DirInode inode = new DirInode(dirId);
      inode.setParentId(parent) ;
      insertIntoDirectoryTable( parent , name , dirId ) ;
      insertDirInode( inode ) ;
      insertInode( inode ) ;
      createDirectoryTable( inode.getTableName() ) ;

      return inode ;
          
   }
   private FileInode _newFile( DbPnfsId parent , String name ) throws SQLException {
   
      DbPnfsId fileId = getNextPnfsId() ;

      FileInode inode = new FileInode(fileId);
      inode.setParentId(parent) ;
      insertIntoDirectoryTable( parent , name , fileId ) ;
      insertFileInode( inode ) ;
      insertInode( inode ) ;

      return inode ;
          
   }
   public Iterator listDirectory( DbPnfsId dirId ) 
          throws SQLException , NoSuchElementException{
          
      //
      // check if is directory 
      //
      DirInode inode = null ;
      try{
         inode = getDirectoryInode(dirId);
      }catch(Exception se ){
         throw new 
         NoSuchElementException("Not a directory " + dirId ) ;
      }
      String query = "SELECT * FROM "+inode.getTableName() ;
      Statement statement = _connection.createStatement() ;
      ResultSet result    = statement.executeQuery(query) ;
      ArrayList list      = new ArrayList() ;
      
      while( result.next() ){
      
         DbPnfsId id = new DbPnfsId( result.getInt("id_high") ,
                                     result.getLong("id_low") ) ;
         int   flags = result.getInt("flags");
         String name = result.getString("name");
         
         list.add( new DirectoryEntry( name ,  id , flags ) ) ;
      }
      
      statement.close();
      
      return list.iterator() ;
   }
   private void _storeFileData( DbPnfsId pnfsId , byte [] data )
       throws SQLException , NoSuchElementException {
   
      String update = 
         "UPDATE fileInodes "+
         "SET data = ? "+
         "  WHERE id_high = "+pnfsId._high+
         "  AND id_low = "+pnfsId._low ;
      PreparedStatement updateStat = _connection.prepareStatement(update);
      updateStat.setBytes(1,data);
      updateStat.executeUpdate() ;
      updateStat.close() ;
      
   }
   public void storeData( DbPnfsId fileid , byte [] data )
       throws SQLException , NoSuchElementException {
    
        Inode inode = getFromInodeTable(fileid) ;
        if( inode._type == DATASET ){
          FileInode finode = getFromFileInodeTable(new FileInode(inode)) ;
	  _storeFileData( fileid , data ) ;
        }else 
           throw new
           IllegalArgumentException("Not a storage object : "+inode._type ) ;
   }
   /*
   private byte [] _fetchFileData( DbPnfsId pnfsId )
       throws SQLException , NoSuchElementException {
   
      _preparedGetFileInode.setBytes(1,data);
      ResultSet result = _preparedGetFileInode.executeQuery() ;
      _preparedGetFileInode.close() ;
      
   }
   */
   public byte [] fetchData( DbPnfsId fileid )
       throws SQLException , NoSuchElementException {
    
        Inode inode = getFromInodeTable(fileid) ;
        if( inode._type == DATASET ){
          FileInode finode = getFromFileInodeTable(new FileInode(inode)) ;
	  return null ;
        }else 
           throw new
           IllegalArgumentException("Not a storage object : "+inode._type ) ;
   }
   public DirInode newDirectory( DbPnfsId parent , String name ) throws SQLException {
   
       _connection.setAutoCommit(false);
       
       try{
       
          return  _newDirectory( parent , name ) ;
          
       }catch(SQLException e){
       
          _connection.rollback() ;
          throw e;
       
       }finally{

          _connection.setAutoCommit(true);
          
       }
   }
   public FileInode newFile( DbPnfsId parent , String name ) throws SQLException {
   
       _connection.setAutoCommit(false);
       
       try{
       
          return  _newFile( parent , name ) ;
          
       }catch(SQLException e){
       
          _connection.rollback() ;
          throw e;
       
       }finally{

          _connection.setAutoCommit(true);
          
       }
   }
   public void createDirectoryTable( String name )throws SQLException {

      Statement statement = _connection.createStatement() ;
      String create = __createDirectoryTable.replaceAll("xxx",name) ;
      statement.execute(create) ;
      statement.close();
   
   }
   private void insertIntoDirectoryTable( DbPnfsId parent , String name , DbPnfsId id )
           throws SQLException {
           
      String insert = 
         "INSERT INTO "+DirInode.getTableName(parent)+
         " ( name , id_high , id_low , flags ) "+
         "values ( '"+name+"' , "+id._high+" , "+id._low+" , 0 )" ;
         
      Statement statement = _connection.createStatement() ;
      int updated = statement.executeUpdate(insert);
      statement.close();
        
   }
   private void insertDirInode( DirInode inode ) throws SQLException {
       _preparedInsertDirInode.setInt(1,inode._pnfsId._high) ;
       _preparedInsertDirInode.setLong(2,inode._pnfsId._low) ;
       _preparedInsertDirInode.setTimestamp(3,inode._modified) ;
       _preparedInsertDirInode.setTimestamp(4,inode._accessed) ;
       _preparedInsertDirInode.executeUpdate() ;
   }
   private void insertFileInode( FileInode inode ) throws SQLException {
       _preparedInsertFileInode.setInt(1,inode._pnfsId._high) ;
       _preparedInsertFileInode.setLong(2,inode._pnfsId._low) ;
       _preparedInsertFileInode.setTimestamp(3,inode._modified) ;
       _preparedInsertFileInode.setTimestamp(4,inode._accessed) ;
       _preparedInsertFileInode.executeUpdate() ;
   }
   private void insertInode( Inode inode ) throws SQLException {
       _preparedInsertInode.setInt(1,inode._pnfsId._high) ;
       _preparedInsertInode.setLong(2,inode._pnfsId._low) ;
       _preparedInsertInode.setInt(3,inode._parentId._high) ;
       _preparedInsertInode.setLong(4,inode._parentId._low) ;
       _preparedInsertInode.setLong(5,inode._type) ;
       _preparedInsertInode.setTimestamp(6,inode._created) ;
       _preparedInsertInode.executeUpdate() ;
   }
   private void storeNextPnfsId( DbPnfsId pnfsId ) throws SQLException {
      Statement statement = _connection.createStatement() ;
      String updateString = "UPDATE nextpnfsid"+
                            " set high = "+pnfsId._high+
                            ", low  = "+pnfsId._low+
                            " where mode = 'next'" ;
      int updated = statement.executeUpdate(updateString);
      if( updated < 1 )
         throw new
         IllegalArgumentException("FATAL : Couldn't update "+pnfsId ) ;
      statement.close() ;
      return ;
   }
   public synchronized DbPnfsId getNextPnfsId() throws SQLException {
      if( _pnfsIdPool.size() == 0 ){
         DbPnfsId id = retrievePnfsId("next") ;
         for( int i = 0 ; i < 32 ; i++ , id = id.nextPnfsId() ){
            _pnfsIdPool.addFirst( id ) ;
         }
         storeNextPnfsId(id);
      }
      return  (DbPnfsId)_pnfsIdPool.removeLast() ;
   }
   private DbPnfsId retrievePnfsId( String mode ) throws SQLException {
      Statement statement = _connection.createStatement() ;
      String  queryString = "SELECT * from nextpnfsid where mode = '"+mode+"'" ;
      ResultSet    result = statement.executeQuery(queryString);
      if( ! result.next() )
         throw new
         IllegalArgumentException("Entry not found for : "+mode ) ;
      DbPnfsId pnfsId = new DbPnfsId( result.getInt(2) , result.getLong(3) ) ;
      statement.close() ;
      return pnfsId ;
   }
   public void close(){
      try{
           _connection.close() ;
      }catch(SQLException e){
          e.printStackTrace() ;
      }
   }
   static private class ArgTokens {
      private ArrayList list = new ArrayList() ;
      private ArgTokens( String line ){
         StringTokenizer st = new StringTokenizer(line);
         while( st.hasMoreTokens() )list.add(st.nextToken());
      }
      public int argc(){ return list.size() ; }
      public String argv(int i){
         if( i >= argc() )
            throw new IllegalArgumentException("out of arguments");
         return (String)list.get(i);
      }
   }
   public static void main( String args[] ) throws Exception {
  
      DbfsV0 db = null; 
      if( args.length < 3 ){ 
   
           db = new DbfsV0( 
                            "com.mysql.jdbc.Driver" ,
                            "jdbc:mysql://localhost:3306/pnfs" ,
                            "patrick" ,
                            "elchy" ) ;
      }else{
           db = new DbfsV0( 
                            "com.mysql.jdbc.Driver" ,
                            "jdbc:mysql://"+args[0]+"/pnfs" ,
                            args[1] ,
                            args[2] ) ;

      }

       BufferedReader br = new BufferedReader( 
                           new InputStreamReader( System.in ) ) ;
       String line = null ;
       while( true ){
           System.out.print(" << ");
           if( ( line = br.readLine() ) == null )break ;
           ArgTokens v = new ArgTokens(line) ;
           if( v.argc() == 0 )continue ;
           String command = v.argv(0) ;
           if( command.equals("exit") )break ;
           try{
              if( command.equals("store") ){
                 if( v.argc() < 3 ){
                    System.out.println(" >> store <parentId> <string>");
                    continue ;
                 }
                 db.storeData( new DbPnfsId(v.argv(1)) ,v.argv(2).getBytes());
              }else if( command.equals("fetch") ){
                 if( v.argc() < 2 ){
                    System.out.println(" >> fetch <parentId>");
                    continue ;
                 }
                 db.fetchData( new DbPnfsId(v.argv(1)) );
              }else if( command.equals("remove") ){
                 if( v.argc() < 3 ){
                    System.out.println(" >> remove <parentId> <name>");
                    continue ;
                 }
                 db.removeObject( new DbPnfsId(v.argv(1)) ,v.argv(2));
              }else if( command.equals("create") ){
                 if( v.argc() < 2 ){
                    System.out.println(" >> create what ?");
                    continue ;
                 }
                 String sub = v.argv(1) ;
                 if( sub.equals("root") ){
                    System.out.println(" >> "+db.createRootDirectory()) ;
                 }else if( sub.equals("dir") ){
                    if( v.argc() < 4 ){
                       System.out.println(" >> create dir <parentId> <name>");
                       continue ;
                    }
                    DirInode inode = db.newDirectory( new DbPnfsId(v.argv(2)),v.argv(3));
                    System.out.println(" >> "+inode);
                 }else if( sub.equals("file") ){
                    if( v.argc() < 4 ){
                       System.out.println(" >> create file <parentId> <name>");
                       continue ;
                    }
                    FileInode inode = db.newFile( new DbPnfsId(v.argv(2)),v.argv(3));
                    System.out.println(" >> "+inode);
                 }else{
                    System.out.println(" >> Command not found");
                 }
              }else if( command.equals("ls") ){
                 if( v.argc() < 2 ){
                    System.out.println(" >> ls <dirId>");
                    continue ;
                 }
                 Iterator i = db.listDirectory( new DbPnfsId(v.argv(1) ) ) ;
                 while( i.hasNext() ){
                     System.out.println(" >> "+i.next().toString());
                 }
              }else if( command.equals("list") ){
                 if( v.argc() < 2 ){
                    System.out.println(" >> list <dirId>");
                    continue ;
                 }
                 Iterator i = db.listDirectory( new DbPnfsId(v.argv(1) ) ) ;
                 while( i.hasNext() ){
                     DirectoryEntry entry = (DirectoryEntry)i.next() ;
                     Inode inode = db.getObjectInode(entry.getPnfsId());
                     System.out.println(" >> "+entry.getName()+" "+inode);
                 }
              }else{
                 System.out.println(" >> Command not found");
              }
           }catch(SQLException sqlE ){
              System.out.println(" >> Exception : "+sqlE);
              sqlE.printStackTrace();
           }catch(Exception E ){
              System.out.println(" >> Exception : "+E);
              E.printStackTrace();
           }
       }
       System.out.println("");
       db.close() ;
    }
}
