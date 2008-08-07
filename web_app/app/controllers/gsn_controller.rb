require 'rubygems'
require 'java'
require 'roxml' # jruby -S gem install roxml
require 'jdbc' # jruby -S gem install jdbc-wrapper

class GsnController < ApplicationController

	def test
		
		p CONTAINER_CONFIG.jdbc_username
		
		#p File.expand_path '.'
		
		
		puts VS_ENABLED.size
		puts VS_ENABLED.each {|k| puts "#{k[0]} == #{k[1]}" }
		
		puts VS_ENABLED.each {|k| puts "#{k[0]} == #{p k[1].methods}" }
					
							
		render :nothing=>true		
	end
	


  def home
  end

  def data
  end

  def map
  end

  def fullmap
  end  

  def populate_list_of_vs

  end

  def structure
	render :nothing=>true
  end
	
end
