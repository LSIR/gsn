$:.unshift File.join(File.dirname(__FILE__),'.')

require 'java'
require 'time'
require 'fileutils'

require 'adapters'

module JDBC
  # A database connection. The starting point for all interaction with the
  # database. The JDBC drivers are not provided, so you are responsible for
  # making sure the jar file is in the classpath so that JRuby will be able
  # class load it.
  class DB
    # Creates a new database connection, you are responsible for
    # making sure the connection gets closed. The database engine
    # param should be a symbol. Supported: :h2, :h2_mem, :hsqldb, 
    # :hsqldb_mem, :derby, :mysql, :postgresql
    def initialize(engine, host, port, user, password, schema)
      adapter = get_adapter(engine, host, port, user, password, schema)
      
      begin
        java.lang.Class.forName(adapter.class_name).newInstance()
    
        @conn = JavaSql::DriverManager.getConnection(
                                  adapter.connection_string)
      rescue java.lang.ClassNotFoundException => e
        raise RuntimeError.new(e.message)
      rescue JavaSql::SQLException => e
        raise RuntimeError.new(e.message)
      end
    end
    
    # Takes a block, and provides an open database connection to the
    # block. Will safely close the connection automatically.
    def self.start(engine, host, port, user, password, database)
      db = nil
      
      begin
          db = DB.new(engine, host, port, user, password, database)
      
          yield(db)
      rescue JavaSql::SQLException => e
        raise RuntimeError.new(e.message)
      ensure
        db.close unless db.nil?
      end
    end
  
    # Takes a valid SQL string. Will return a Result object for a query,
    # or the number of rows affected for an update.
    def query(sql)
      stmt = nil
      result = nil
      
      begin
        stmt = @conn.createStatement
        
        res = stmt.execute(sql)
        
        if res == false
          return stmt.getUpdateCount
        end
        
        return Result.new(stmt.getResultSet, stmt)
      rescue JavaSql::SQLException => e
        stmt.close unless stmt.nil?
        raise RuntimeError.new(e.message)
      end
    end
    
    # Takes a valid SQL string. Returns a PreparedStatement object if
    # no block is given (you are required to close it). If a block is 
    # provided it will pass the statement to the block, and it will 
    # automatically close upon block exit.
    def prepare(sql)
      stmt = PreparedStatement.new(@conn.prepareStatement(sql))
      
      if block_given?
        yield(stmt)
        
        stmt.close
        
        return
      end
      
      return stmt
    end
    
    # Closes the database connection.
    def close
      @conn.close unless @conn.nil?
    end
  
    private
    
    def get_adapter(engine, host, port, user, password, schema)
      case engine
      when :derby
        return Adapters::Derby.new(host, port, user, password, schema)
      when :h2
        return Adapters::H2.new(host, port, user, password, schema)
      when :h2_mem
        return Adapters::H2.new(host, port, user, password, schema, true)
      when :hsqldb
        return Adapters::Hsqldb.new(host, port, user, password, schema)
      when :hsqldb_mem
        return Adapters::Hsqldb.new(host, port, user, password, schema, true)
      when :mysql
        return Adapters::Mysql.new(host, port, user, password, schema)
      when :postgresql
        return Adapters::Postgresql.new(host, port, user, password, schema)
      end
      
      raise RuntimeError.new("#{engine} is not supported.")
    end
  end
end
