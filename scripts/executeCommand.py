from org.pcells.services.connection import Ssh2DomainConnection
from java.lang import Thread

connection = Ssh2DomainConnection( "localhost", 22224 )
connection.setLoginName( "admin" )
connection.set_algorithm("DSA")
connection.setPassword("")
connection.set_privateKeyFilePath("/Users/chris/.ssh/id_dsa.der")
connection.set_publicKeyFilePath("/Users/chris/.ssh/id_dsa.der.pub")
connection.set_keyPath("/home/root/.ssh")

connection.addDomainEventListener( _myEventListener( verbose ) )
thread = Thread( connection.go, "AdminSvrSession" )
thread.start()
