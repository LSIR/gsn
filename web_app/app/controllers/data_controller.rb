require 'java'

import 'java.io.InputStream'

class DataController < ApplicationController

  skip_before_filter :login_required

  verify :add_flash => {:error => "Please, select Fields as data output."}, :params => [:deployment],:only => [:download_data],:redirect_to => {:action => :data}

  def data
  end

  def parse_download_parameters(params)

    deployments = params[:deployment].nil?	? nil : params[:deployment].values
    virtual_sensors = params[:vs].nil?		? nil : params[:vs].values
    fields = params[:field].nil?		? nil : params[:field].values

    c_deployments = params[:c_deployment].nil?	? nil : params[:c_deployment].values
    c_vss = params[:c_vs].nil?			? nil : params[:c_vs].values
    c_fields = params[:c_field].nil?		? nil : params[:c_field].values
    c_joins = params[:c_join].nil?		? nil : params[:c_join].values
    c_maxs = params[:c_max].nil?		? nil : params[:c_max].values
    c_mins = params[:c_min].nil?		? nil : params[:c_min].values


    # VIRTUAL SENSORS AND FIELDS
    vss_and_fields = to_vss_and_fields(deployments, virtual_sensors, fields)

    # CONDITIONS
    conditions = []
    unless c_deployments.nil? || c_deployments.size == 0
      conditions = to_conditions(c_deployments, c_vss, c_fields, c_joins, c_maxs, c_mins)
    end

    # TIME
    param = params[:from]
    from = param.nil? || param.blank? ? nil : Time.parse(param).to_i
    param = params[:to]
    to = param.nil? || param.blank? ? nil : Time.parse(param).to_i

    # MAX NB OF DATA
    param = params[:nb]
    nb = case param
    when 'ALL'
      nil
    when 'SPECIFIED'
      params[:nb_value].to_i
    else
      nil
    end

    # TIME FORMAT
    param = params[:time_format]
    time_format = case param
    when "ISO"
      "iso"
    when "UNIX"
      "unix"
    else
      nil
    end

    # DOWNLOAD FORMAT
    param = params[:download_format]
    download_format = case param
    when "CSV"
      :csv
    when "REPORT"
      :report
    when "XML"
      :xml
    else
      nil
    end

    # AGGREGATION
    param = params[:agg_function]
    aggregation = case param
    when "AVG"
      :avg
    when "MAX"
      :max
    when "MIN"
      :min
    else
      nil
    end
    aggregation_period = aggregation.nil? ? nil : params[:agg_period].to_i
    aggregation_unit = aggregation.nil? ? nil : params[:agg_unit].to_i

    #######

    parameters_map = Java::java.util.Hashtable::new

    # vsname
    parameters_map.put('vsname', JavaBeans.instance.ruby_to_java_string_a(vss_and_fields))

    # std criteria
    conditions << "and:::timed:ge:#{from}" unless from.nil?	# time from
    conditions << "and:::timed:leq:#{to}"  unless to.nil?	# time to
    parameters_map.put('critfield', JavaBeans.instance.ruby_to_java_string_a(conditions)) unless conditions.empty?
    
    # limit criterion
    parameters_map.put('nb', JavaBeans.instance.ruby_to_java_string_a(["0:#{nb}"])) unless nb.nil?

    # aggregation criterion
    unless aggregation.nil?
      period = aggregation_period * aggregation_unit
      parameters_map.put('groupby', JavaBeans.instance.ruby_to_java_string_a(["#{period}:#{aggregation}"]))
    end

    # time format
    unless time_format.nil?
      parameters_map.put('timeformat', JavaBeans.instance.ruby_to_java_string_a(["#{time_format}"]))
    end

    # download format
    case download_format
    when :report
      parameters_map.put('reportclass', JavaBeans.instance.ruby_to_java_string_a(["report-default"]))
    when :csv
      parameters_map.put('outputtype', JavaBeans.instance.ruby_to_java_string_a(["csv"]))
    when :xml
      parameters_map.put('outputtype', JavaBeans.instance.ruby_to_java_string_a(["xml"]))
    end

    [download_format, parameters_map]
  end


  def download_data

    download_format, parameters_map  = parse_download_parameters(params)
    begin
      ## PROCESS
      case download_format
      when :report
        report = Java::gsn.http.datarequest.DownloadReport::new(parameters_map)
        report.process()
        dat = String.from_java_bytes report.output_result()
        send_data(dat, :filename => 'report.pdf', :type => 'application/pdf', :disposition => 'attachment')
      when :csv
        download_data = Java::gsn.http.datarequest.DownloadData::new(parameters_map)
        download_data.process
        #        dat = download_data.output_result
        #        send_data(dat, :filename => 'data.csv', :type => 'text/csv', :disposition => 'attachment')
        headers.update(
          'Content-Length'            => nil,
          'Content-Type'              => 'text/csv',
          'Content-Disposition'       => 'attachment; filename=data.csv'
        )
        render :text => proc { |response, output|
          is = download_data.get_input_stream(4096)
          java_byte_array = Java::byte[4096].new
          read_length = 0
          while ((read_length = is.read(java_byte_array)) != -1)
            next_block = [read_length]
            i = 0
            while (i < read_length)
              next_block[i] = java_byte_array[i].chr
              i = i + 1
            end
            output.print(next_block)
          end
        }, :layout => false
      when :xml
        download_data = Java::gsn.http.datarequest.DownloadData::new(parameters_map)
        download_data.process()
        headers.update(
          'Content-Length'            => nil,
          'Content-Type'              => 'text/xml',
          'Content-Disposition'       => 'attachment; filename=data.xml'
        )
        render :text => proc { |response, output|
          is = download_data.get_input_stream(4096)
          java_byte_array = Java::byte[4096].new
          read_length = 0
          while ((read_length = is.read(java_byte_array)) != -1)
            next_block = [read_length]
            i = 0
            while (i < read_length)
              next_block[i] = java_byte_array[i].chr
              i = i + 1
            end
            output.print(next_block)
          end
        }, :layout => false
      else
        raise Exception.new "The Download format #{download_format} is not implemented."
      end
    rescue Exception => e
      flash[:error] = "#{e.backtrace}"
      redirect_to :action => :data
    rescue NativeException => e
      flash[:error] = "#{e.backtrace}"
      redirect_to :action => :data
    end
  
  end

  def to_conditions(c_deployments, c_vss, c_fields, c_joins, c_maxs, c_mins)
    conditions = []
    c_deployments.each_with_index { |_dep, i|
      deployment_to_vss(_dep, c_vss[i]).each { |_vs|
        virtual_sensor_to_fields(_vs, c_fields[i]).each { |_field|
          join = c_joins[i] == '0' ? 'or' : 'and'
          conditions <<  ("#{join}::#{_vs.name.downcase}:#{_field.name.downcase}:le:#{c_maxs[i]}") unless (c_maxs[i] =='+inf')
          conditions <<  ("#{join}::#{_vs.name.downcase}:#{_field.name.downcase}:geq:#{c_mins[i]}") unless (c_mins[i] =='-inf')
        }
      }
    }
    conditions
  end

  # deployment:	    An array of deployment names
  # virtual_sensor: An array of virtual sensor names
  # field:	    An array of field names
  def to_vss_and_fields(deployment, virtual_sensor, field)
    vs_fields = {}
    deployment.each_with_index { |_dep, i|
      deployment_to_vss(_dep, virtual_sensor[i]).each { |_vs|
        update_vs_fields(vs_fields, _vs, virtual_sensor_to_fields(_vs, field[i]))
      }
    }
    vss_fields = []
    vs_fields.each { |_vs, _fields|
      vss_fields << "#{_vs.downcase}#{_fields.inject(''){ |fs, next_field| fs + ":" + next_field.downcase }}"
    }
    vss_fields
  end

  # Return an array of all the VirtualSensor that are both under the specified deployment_name and have the vs_name.
  def deployment_to_vss(deployment_name, vs_name)
    deployment = nil
    vss = nil
    begin
      if deployment_name == 'All'
        deployment = Deployment.find(:all)
      else
        deployment = Deployment.find(:all, :conditions => { :name => deployment_name })
        raise Exception.new "The Deployment #{deployment_name} does not exist." if deployment.empty?
      end
      if vs_name == 'All'
        vss = deployment.inject([]) { |list_of_vs, next_deployment| list_of_vs + next_deployment.virtual_sensors.find(:all) }
      else
        vss = deployment.inject([]) { |list_of_vs, next_deployment| list_of_vs + next_deployment.virtual_sensors.find(:all, :conditions => { :name => vs_name }) }
      end
    end
    vss
  end

  # Return an array that contains the list of fields names that are both in
  # the Virtual Sensor fields list and the field given as parameter.
  # If the field parameter == 'All' then this method returns all the
  # Virtual Sensor fields.
  def virtual_sensor_to_fields(vs, field_name)
    if field_name == 'All'
      vs.pc_instance.processor.output_formats.find(:all)
    else
      vs.pc_instance.processor.output_formats.find(:all, :conditions => { :name => field_name })
    end
  end


  # vs_fields structure: {:vsname1 => [:field1, field2, ..., fieldn], ... , :vsnamen => [:field1, field2, ..., fieldn] }
  def update_vs_fields(vs_fields, vs, fields)
    if fields.size > 0
      vs_fields[vs.name] ||= []
      fields.each { |_field|
        vs_fields[vs.name] << _field.name unless vs_fields[vs.name].include? _field.name
      }
    end
    vs_fields
  end

  # Returns a json object which contains deployments,virtual-sensors and all their curresponding fields.
  def dvos
    to_return = {}
    Deployment.find(:all).each do |deployment|
      to_return[deployment.name.to_sym] ||={}
      deployment.virtual_sensors.each do |vs|
        to_return[deployment.name.to_sym][vs.name.to_sym]||={}
        vs.pc_instance.processor.output_formats.each do |f|
          to_return[deployment.name.to_sym][vs.name.to_sym][ f.name.to_sym]=f.name.to_sym;
        end
      end
    end
    #    p to_return
    render :json => to_return, :layout => false
  end
end

def data_2

end
