require 'rubygems'
require 'java'
require 'roxml'		# jruby -S gem install roxml
require 'jdbc'		# jruby -S gem install jdbc-wrapper

class GsnController < ApplicationController
	
  protect_from_forgery :except => :notify 
	
  def test
    p CONTAINER_CONFIG.jdbc_username
    puts VS_ENABLED.size
    puts VS_ENABLED.each {|k| puts "#{k[0]} == #{k[1]}" }
    puts VS_ENABLED.each {|k| puts "#{k[0]} == #{p k[1].methods}" }
    puts STORAGE_MANAGER.methods
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

  def populate_search_list_of_vs
    search_criteria = params[:search_criteria] ? params[:search_criteria].to_s : ''
    current_page = params[:page] ? params[:page].to_i : 1
    @per_page = 2 #TODO ADD THIS CONSTANT IN THE PREFERENCES
    @vss = filter_list_of_vs(search_criteria)
    @page_results = WillPaginate::Collection.create(current_page, @per_page, @vss.length) do |pager|
	start = (current_page-1)*@per_page
	pager.replace(@vss[start, @per_page])
    end
    render :partial => 'shared/search_list_of_vs', :locals => {:page_results => @page_results} #, :locals => {:loaded_vs_list => VS_ENABLED, :storage_manager => STORAGE_MANAGER}
  end

  def update_refreshbar
    session[:refresh_time] = params[:refresh_time]
    render :partial => 'shared/refreshbar'
  end

  # sample request: http://localhost:3000/gsn/structure/GPSVS
  def structure
    vs_config = VS_NAME_TO_CONFIG[params[:name].upcase.chomp]
    if vs_config.nil?
      render :text => "requested virtual sensor #{params[:name]} is not found/accessible.",:layout=>false,:status => 500
    else
      to_return = vs_config.output_structure.inject('') {|sum,o| sum << "#{o.name}[#{o.type}],"} unless vs_config.nil?
      render :text => (to_return||''), :layout => false,:status => 200
    end
    
  end
  
  #gsn/register/GPSVS/1/2/notify/localhost/22001/12345/select * from wrapper
  def register
    vs_config = VS_NAME_TO_CONFIG[params[:name].upcase.chomp]
    if vs_config.nil?
      render :text => "requested virtual sensor #{params[:name]} is not found/accessible.",:layout=>false,:status => 500
    else
      Java::gsn.GSNRequestHandler::register_query params[:remote_host],params[:remote_port].to_i,params[:name],params[:query],params[:code].to_i    
    end
    render :text => "query:#{params[:query]} registered.", :layout => false,:status => 200
  end
  
  def notify
  	puts 'called'
  	puts params[:code]
  	params.each {|key,value| puts "#{key} ==== #{value}" }
  	render :text => "Accepted.", :layout => false,:status => 201
  end

  :private

  def filter_list_of_vs(search_criteria)
    VS_ENABLED.values.to_array.find_all { |vs| vs.get_name.include? search_criteria  }
  end
	
end
