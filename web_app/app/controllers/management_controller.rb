require 'rubygems'
require 'java'

class ManagementController < ApplicationController

	def index
		render :nothing=>true
	end
	
	def status 
		
	end
	
	def structure
	
		#param[
	    vs_list = Java::gsn.gui.util::VSensorIOUtil.new('virtual-sensors/', 'virtual-sensors/Disabled/')
		enabled_vs_files = vs_list.read_virtual_sensors
		
		render :nothing=>true
	end
end
