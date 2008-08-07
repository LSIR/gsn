module JDBC
  module Adapters
    class Hsqldb
      attr_reader :host, :port, :user, :password, :schema
      
      def initialize(host, port, user, password, schema, in_memory = false)
        @host = host
        @port = port
        @user = user
        @password = password
        @schema = schema
        @in_memory = in_memory
      end
      
      def connection_string
        str = "jdbc:hsqldb"
        str << ":mem" if @in_memory
        str << ":#{@schema}"
        str
      end
      
      def class_name
        "org.hsqldb.jdbcDriver"
      end
    end
  end
end