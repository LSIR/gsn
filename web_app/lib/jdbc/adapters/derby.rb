module JDBC
  module Adapters
    class Derby
      attr_reader :host, :port, :user, :password, :schema
      
      def initialize(host, port, user, password, schema)
        @host = host
        @port = port
        @user = user
        @password = password
        @schema = schema
      end
      
      def connection_string
        str = "jdbc:derby:#{@schema}"
      
        if ! File.exist?(@schema)
          str << ";create=true"
        end
      
        return str
      end
      
      def class_name
        "org.apache.derby.jdbc.EmbeddedDriver"
      end
    end
  end
end