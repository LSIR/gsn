require 'rubygems'
require 'java'
require 'roxml' # jruby -S gem install roxml
require 'jdbc'  # jruby -S gem install jdbc-wrapper

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

  def populate_detailed_list_of_vs
    # cookies[:user_id] = bla
    # cookies.delete :user_id

    # Get the last list of displayed vs from the cookie
    # Build the list from that
    # If the list does not have enough elements
    #	try to see if you can add more available
    # Save the list in the cookie
    render :partial => 'shared/detailed_list_of_vs', :locals => {:loaded_vs_list => VS_ENABLED, :storage_manager => STORAGE_MANAGER}
  end

  def populate_list_of_vs
    # Get the list from Java,
    # call a view that structure them through categories.
    # Remove the ones that are activated but not in the available list
    # do not save the activated list
  end

  def init_population_of_detailed_vs_list

    # kill the activated list from the cookie

    # called once the page is loading
    # adds a max of 10 firsts vs.
  end

  def add_vs_to_detailed_list
    # add it to the list of activated vs in the cookie
    # send the javascript to add it (and remove the last if already full)
  end

  def remove_vs_from_detailed_list
    # remove from the detailed list
    # send the javascript to remove it from the web interface
  end

  def update_selected_detailed_vss
    # get the static and dynamic data
    # send all the static and dynamic informations to update only one selected vs, alert if vs is not selected
    # show or hide the corresponding tabs tabs
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
	
end
