class HomeController < ApplicationController

  skip_before_filter :login_required



  def home
    @deployments = Deployment.find(:all, :conditions => { :private => false })
    @data_count = {}
    @deployments.each { |deployment|
      deployment.virtual_sensors.each { |vs|
        @data_count[deployment.name] ||= 0
        @data_count[deployment.name] += (ActiveRecord::Base.connection.select_one("select count(timed) from #{vs.name.downcase}")['count(timed)'].to_i * vs.pc_instance.processor.output_formats.size)
      }
    }
    @last_data_count = Time.now
  end
  
end
