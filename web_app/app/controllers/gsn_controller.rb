require 'rubygems'
require 'java'
require 'roxml' # jruby -S gem install roxml
require 'jdbc' # jruby -S gem install jdbc-wrapper

class GsnController < ApplicationController
	def test
		
		container_config = Java::gsn.Main::load_container_configuration
		p container_config.jdbc_username
		
		vs_list = Java::gsn.gui.util::VSensorIOUtil.new('virtual-sensors/', 'virtual-sensors/Disabled/')
		
		#p File.expand_path '.'
		enabled_vs_files = vs_list.read_virtual_sensors
		disabled_vs_files = vs_list.read_disabled_virtual_sensors
		
		
		enabled_vs_files.each {|f| puts f}
		disabled_vs_files.each {|f| puts f}
		
		p vs_list.methods
		
		
		enabled_vs_map = Java::gsn.gui.util::VSensorConfigUtil.getVSensorConfigs(enabled_vs_files)
		disabled_vs_map =  Java::gsn.gui.util::VSensorConfigUtil.getVSensorConfigs(disabled_vs_files)
			
#		enabled_vs_map.each_pair {|key,value| "#{key} == #{value.inspect}"}
#		enabled_vs_map.each_pair {|key,value| "#{key} == #{value.inspect}"}
		puts enabled_vs_map.size
		puts enabled_vs_map.each {|k| puts "#{k[0]} == #{k[1]}" }
		
		puts enabled_vs_map.each {|k| puts "#{k[0]} == #{p k[1].methods}" }
					
		render :nothing=>true		
	end
	
end
