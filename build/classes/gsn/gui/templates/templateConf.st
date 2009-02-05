<sensor-server>
   <name>$name$</name>
   <author>$author$</author>
   <email>$email$</email>
   <description>$description$</description>
   <port>$gsn_port$</port>
<!-- OTHER DATABASE OPTIONS -->
<!-- <storage user="sa" password="" driver="org.hsqldb.jdbcDriver" url="jdbc:hsqldb:MEM:." />  -->
<!-- <storage user="sa" password="" driver="org.hsqldb.jdbcDriver" url="jdbc:hsqldb:file:MyFile" /> -->
<!-- <storage user="root" password="" driver="com.mysql.jdbc.Driver" url="jdbc:mysql://localhost/gsn" /> --> 
<!-- <storage user="gsn" password="gsn" driver="net.sourceforge.jtds.jdbc.Driver" url="jdbc:jtds:sqlserver://192.168.51.14:12345/gsn;cachemetadata=true;prepareSQL=3" />-->
<!-- <storage user="root" password="" driver="com.mysql.jdbc.Driver" url="jdbc:mysql://localhost/gsn" />-->  
   <storage user="$db_user$" password="$db_password$" driver="$db_driver$" url="$db_url$" overwrite-tables="$db_overwrite$" permanent-storage="$db_permanent$"/>
 </sensor-server>

