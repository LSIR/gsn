module JDBC
  module Adapters
    class Mysql
      attr_reader :host, :port, :user, :password, :schema
      
      def initialize(host, port, user, password, schema)
        @host = host
        @port = port
        @user = user
        @password = password
        @schema = schema
      end
      
      def connection_string
        "jdbc:mysql://#{@host}:#{@port}/#{@schema}" +
                                  "?user=#{@user}&password=#{@password}"
      end
      
      def class_name
        "com.mysql.jdbc.Driver"
      end
    end
  end
end