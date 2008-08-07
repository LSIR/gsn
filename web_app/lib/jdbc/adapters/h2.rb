module JDBC
  module Adapters
    class H2
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
        str = "jdbc:h2:"
        str << "mem:" if @in_memory
        str << "#{@schema}"
        str
      end
      
      def class_name
        "org.h2.Driver"
      end
    end
  end
end