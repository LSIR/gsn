module ConfigurationHelper

  def add_wrapper_init_link(link_name,wrapper_id)
    link_to_function link_name do |page|
      page.insert_html :bottom, "wrapper_#{wrapper_id}_inits", :partial => "/configuration/wrapper/wrapper_init", :object => WrapperInit.new
    end
  end

  def add_output_format_link(link_name,processor_id)
    link_to_function link_name do |page|
      page.insert_html :bottom, "processor_#{processor_id}_output_formats", :partial => "/configuration/processor/output_format", :object => OutputFormat.new
    end
  end

  def add_pc_init_link(link_name, processor_id)
    link_to_function link_name do |page|
      page.insert_html :bottom, "processor_#{processor_id}_pc_inits", :partial => "/configuration/processor/pc_init", :object => PcInit.new
    end
  end

  def add_web_input_link(link_name, processor_id)
    link_to_function link_name do |page|
      page.insert_html :bottom, "processor_#{processor_id}_web_inputs", :partial => "/configuration/processor/web_input", :object => WebInput.new
    end
  end

  def add_property_link(link_name)
    link_to_function link_name do |page|
      page.insert_html :bottom, :properties, :partial => "/configuration/property_group/property", :object => Property.new
    end
  end

  def add_property_value_link(link_name, deployment_id)
    link_to_function link_name do |page|
      page.insert_html :bottom, "deployment_#{deployment_id}_property_values", :partial => "/configuration/deployment/property_value", :object => PropertyValue.new
    end
  end

end
