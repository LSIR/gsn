module WrapperHelper

  def add_wrapper_init_link(link_name,wrapper_id)
    link_to_function link_name do |page|
      page.insert_html :bottom, "wrapper_#{wrapper_id}_inits", :partial => "configuration/wrapper/wrapper_init", :object => WrapperInit.new
    end
  end

end
