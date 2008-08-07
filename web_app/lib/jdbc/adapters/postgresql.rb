module JDBC
  module Adapters
    class Postgresql
      attr_reader :host, :port, :user, :password, :schema
      
      def initialize(host, port, user, password, schema)
        @host = host
        @port = port
        @user = user
        @password = password
        @schema = schema
      end
      
      def connection_string
        "jdbc:postgresql://#{@host}:#{@port}/#{@schema}" +
                                  "?user=#{@user}&password=#{@password}"
      end
      
      def class_name
        "org.postgresql.Driver"
      end
    end
  end
end