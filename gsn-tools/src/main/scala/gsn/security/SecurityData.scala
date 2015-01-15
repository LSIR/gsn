package gsn.security

import gsn.data.DataStore
import scala.util.Try
import slick.jdbc.JdbcBackend._

class SecurityData(ds:DataStore) {
  private object Queries{
    val addOrigin=s"""ALTER TABLE acuser 
                    ADD COLUMN ORIGIN VARCHAR(15) NULL;"""
    val addGsnOrigin=s"""UPDATE acuser SET ORIGIN ='GSN';"""
    val testOrigin="SELECT origin from acuser;"
    val userkeys="""CREATE TABLE acuserkey ( 
                    USERNAME VARCHAR(100) NOT NULL,
                    APIKEY VARCHAR(200) NOT NULL,
                    EXPIRES INT,
      				PRIMARY KEY (USERNAME,APIKEY),
      				FOREIGN KEY (USERNAME) REFERENCES ACUSER(USERNAME))"""
    def createUser(username:String,origin:String)=
      s"""INSERT INTO acuser VALUES ('$username' ,'','','','','no','$origin'"""
    def testTable(table:String)=s"SELECT * FROM $table"
   
  }  
  private def existsTable(tableQuery:String)(implicit session:Session)= 
    Try{session.conn.createStatement.execute(tableQuery)}
  
  def upgradeUsersTable:Unit={
    ds.withSession{implicit session=>
	  val existOrigin=existsTable(Queries.testOrigin)
	  if (existOrigin.isFailure){
        session.conn.createStatement.execute(Queries.addOrigin)	    
        session.conn.createStatement.execute(Queries.addGsnOrigin)        
	  }	  
	  val existsUserKey=existsTable(Queries.testTable("acuserkey"))
	  if (existsUserKey.isFailure){
        session.conn.createStatement.execute(Queries.userkeys)	    	    
	  }
    }
  }
  
  def authorizeVs(vsname:String,apikey:String):Boolean={
    val q=s"""SELECT * FROM acuserkey u,acuser_acdatasource uds 
              WHERE datasourcename='$vsname' AND
              u.username=uds.username"""
    val qGroup=s"""SELECT * FROM acuserkey u,acgroup_acdatasource gds,acuser_acgroup ug 
              WHERE datasourcename='$vsname' AND
              u.username=ug.username AND ug.groupname=gds.groupname"""
    ds.withSession{implicit session=>
      val rs=session.conn.createStatement.executeQuery(q)
      val exists=
        if (rs.next) true
        else {
          val rsGroup=session.conn.createStatement.executeQuery(qGroup)
          rsGroup.next
        }
      exists
    }
  }
  
}