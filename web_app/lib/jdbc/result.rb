module JDBC
  # Result wraps a java ResultSet, and provides methods to both fetch rows
  # one at a time, as well as ruby iterators. All rows are automatically
  # converted to a ruby Array or Hash when they are fetched. Each column is
  # casted to the equivalent ruby object if possible, otherwise it will be a
  # String.
  class Result
    # Takes a java ResultSet and Statement. You should not have to call this.
    # A Result will usually be instantiated by a DB or PreparedStatement.
    def initialize(resultSet, statement)
      @rs = resultSet
      @stmt = statement
      @columns = get_rs_meta_data
    end
    
    # Fetches the next row and returns it as an Array. Returns nil if no more
    # rows are available and closes the Result.
    def fetch
      if @rs.next
        result = []
      
        @columns.each do |column| 
          result << fetch_and_cast(@rs, column)
        end
        
        return result
      end
      
      close
      
      return nil
    end
    
    # Fetches the next row and returns it as a Hash. The column names are
    # the keys. Returns nil is no more rows are available and closes the Result.
    #
    # Note: All column names are automatically lowercased for consistency
    # since databases differ in behavior on this aspect. (ex: Derby is
    # uppercase, Postgres is lowercase, and Mysql depends on how the table
    # was created.)
    def fetch_hash
      if @rs.next
        result = {}
      
        @columns.each do |column| 
          result[column[:name]] = fetch_and_cast(@rs, column)
        end
        
        return result
      end
      
      close
      
      return nil
    end
    
    # Returns each row as an Array to the provided block. Will automatically
    # close the Result after the block exits.
    def each
      while(result = fetch)
        yield(result)
      end
    end
  
    # Returns each row as an Hash to the provided block. The column names 
    # are the keys. Will automatically close the Result after the block exits.
    def each_hash
      while(result = fetch_hash)
        yield(result)
      end
    end
  
    # Closes the Result
    def close
      @rs.close unless @rs.nil?
      @stmt.close unless @stmt.nil?
    end
  
    private
    
    def get_rs_meta_data
      meta_data = @rs.getMetaData
    
      columns = []
    
      meta_data.getColumnCount.times do |i|
        columns << {
          :name => meta_data.getColumnName(i+1).downcase,
          :type => meta_data.getColumnType(i+1)
        }
      end
      
      return columns
    end
    
    def fetch_and_cast(rs, column)
      if column[:type] == JavaSql::Types::NULL
        return nil
      end
    
      if column[:type] == JavaSql::Types::INTEGER  ||
         column[:type] == JavaSql::Types::SMALLINT ||
         column[:type] == JavaSql::Types::TINYINT  ||
         column[:type] == JavaSql::Types::BIGINT 
        return rs.getInt(column[:name])
      end
    
      if column[:type] == JavaSql::Types::DECIMAL ||
         column[:type] == JavaSql::Types::FLOAT
        return rs.getDouble(column[:name])
      end
    
      if column[:type] == JavaSql::Types::DATE ||
         column[:type] == JavaSql::Types::TIME ||
         column[:type] == JavaSql::Types::TIMESTAMP
      
        val = rs.getString(column[:name])
      
        return nil if val.nil?
      
        return Time.parse(val)
      end
    
      return rs.getString(column[:name])
    end
  end
end
