class LocalTimeWrapperRB <  Java::gsn.wrappers::RubyWrapperAdapter

	#@@logger        = Java::org.apache.log4j::Logger.logger(self.java_class);
	
	@@thread_counter = 0
	
	EMPTY_STREAM_ELEMENT = [].to_java(Java::java.io::Serializable)
	EMPTY_FIELD_TYPES = [].to_java(:string)
	EMPTY_FIELDS = [].to_java(Java::gsn.beans::DataField)
	
	def bootstrap
		@wrapper_name = "System Time"
		@@thread_counter+=1
		name= "LocalTimeWrapper-Thread #{@@thread_counter}"
		@delay = active_address_bean.predicate_value_as_int("clock-period",1)
    	using_remote_timestamp , @running = true , true
    	true
	end
	
	def run
	  while @running
	  	sleep @delay 
	  	se = Java::gsn.beans::StreamElement.new(EMPTY_FIELDS , EMPTY_FIELD_TYPES , EMPTY_STREAM_ELEMENT , Time.now.tv_sec*1000 );
	  	post_stream_element(se)
	  end
	end
	
	def shutdown
	  @@thread_counter-=1
	  @running = false
	end
	
 	def  getOutputFormat; EMPTY_FIELDS ; end
	
end
  