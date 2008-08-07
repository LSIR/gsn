module JDBC
  # Wraps a java PreparedStatement. 
  class PreparedStatement
    # Takes a java PreparedStatement. You should not have to call this. It
    # will be instantiated by a DB connection.
    def initialize(stmt)
      @stmt = stmt
      @meta_data = stmt.getParameterMetaData
    end
    
    # Executes the statement with the provided list of comma separated
    # arguments. Will return a Result for queries and the number of rows
    # affected for updates.
    def execute(*args)
      if args.length != @meta_data.getParameterCount
        raise RuntimeError.new("Got #{args.length} params, " + 
                      "expected #{@meta_data.getParameterCount}.")
      end
      
      @stmt.clearParameters
      
      args.each_with_index do |arg, i|
        @stmt.setObject(i+1, arg, @meta_data.getParameterType(i+1))
      end
      
      if @stmt.execute
        return Result.new(@stmt.getResultSet, @stmt)
      end
      
      return @stmt.getUpdateCount
    end
    
    # Closes the statement.
    def close
      @stmt.close
    end
  end
end
