require 'rubygems'
require 'java'
require 'roxml'		# jruby -S gem install roxml
require 'jdbc'		# jruby -S gem install jdbc-wrapper

class GsnController < ApplicationController
	
  protect_from_forgery :except => [:notify, :populate_search_list_of_vs]
	
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

  # Return the GSN XML Output list of virtual sensors for backward compatibility
  def list_of_vs
    xml_output = CONTAINER_INFO_HANDLER.build_output(params[:name])
    render :xml => xml_output
  end

  def jason_search_list_of_vs

    page	=   params[:page]	?   params[:page].to_i	:   1
    rp		=   params[:rp]		?   params[:rp].to_i	:   30
    query	=   params[:query]	?   params[:query]	:   ''
    qtype	=   params[:qtype]	?   params[:qtype]	:   'get_name'
    sortorder	=   params[:sortorder]	?   params[:sortorder]	:   'desc'
    sortname	=   'get_name'

    offset = ((page-1) * rp).to_i

    @vss = filter_list_of_vs(query.split(' '), qtype.to_a, sortname, sortorder)
    @vss_limited = @vss.length > rp ? @vss[offset, rp] : @vss

    list_of_vs = Hash.new()
    list_of_vs[:page] = page
    list_of_vs[:total] = @vss.length
    list_of_vs[:rows] = @vss_limited.collect{ |vs| {:id => vs.get_name.to_s, :cell => [
	  vs.get_name,
	  "TODO Last Update",
	  vs.get_description,
	  vs.is_enabled,]}}
    render :text => list_of_vs.to_json, :layout => false
  end

  def populate_search_list_of_vs

    puts 'populate list called'

    page	=   params[:page]	?   params[:page].to_i	:   1
    rp		=   params[:rp]		?   params[:rp].to_i	:   30
    query	=   params[:query]	?   params[:query]	:   ''
    qtype	=   params[:qtype]	?   params[:qtype].to_a	:   ['get_name','get_description']
    sortorder	=   params[:sortorder]	?   params[:sortorder]	:   'desc'
    sortname	=   'get_name'
    offset = ((page-1) * rp).to_i

    @vss = filter_list_of_vs(query.split(' '), qtype, sortname, sortorder)

    if false #TODO Add a choice in the preferences
      # Drop down search
      @page_results = WillPaginate::Collection.create(page, rp, @vss.length) do |pager|
	pager.replace(@vss[offset, rp])
      end
      render :partial => 'shared/search_list_of_vs', :locals => {:page_results => @page_results} #, :locals => {:loaded_vs_list => VS_ENABLED, :storage_manager => STORAGE_MANAGER}
    else
      # Flexigrid search      
      @vss_limited = @vss.length > rp ? @vss[offset, rp] : @vss
      list_of_vs = Hash.new()
      list_of_vs[:page] = page
      list_of_vs[:total] = @vss.length
      list_of_vs[:rows] = @vss_limited.collect{ |vs| {:id => vs.get_name.to_s, :cell => [vs.get_name, "TODO Last Update", vs.get_description, vs.is_enabled,]}}
      render :text => list_of_vs.to_json, :layout => false
    end
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

  # Returns a sorted and filtered list of VS (enabled or not).
  # The VS are selected only if at least one of the VS field
  # specified in the 'list_of_fields' contains one of the string
  # specified in the 'list_of_criteria'. Comparison is not case
  # sensitive.
  #
  # The VS are sorted by the 'sort_by_field' field and following
  # the 'sort_order' order (either 'asc' or 'desc')
  #
  def filter_list_of_vs(list_of_criteria, list_of_fields, sort_by_field, sort_order)
    sorted_list_of_vs = get_list_of_vs.sort_by { |vs| vs.send(sort_by_field.to_s) }
    sorted_list_of_vs.reverse! if sort_order.eql? 'desc'
    if list_of_criteria.length == 0
      sorted_list_of_vs
    else
      filtered_list = sorted_list_of_vs.find_all { |vs|
	list_of_criteria.inject(false) { |decide,criterion|
	  (list_of_fields.inject(false) { |_decide, field| (vs.send(field.to_s).downcase.include? criterion.downcase) || _decide }) || decide }
      }
    end
  end

  # Returns an array of VS that merges the disabled and enabled ones.
  # Use the is_enabled method to differentiate between these two groups.
  def get_list_of_vs
    enabled = VS_ENABLED.values.to_array.collect { |vs| def vs.is_enabled
	true
      end
      vs
    }
    disabled = VS_DISABLED.values.to_array.select { |vs| vs }.collect { |vs| def vs.is_enabled
	false
      end
      vs
    }
    enabled + disabled
  end
	
end
