module MultilevelForms
  module FormBuilderHelpers
    include ActionView::Helpers::PrototypeHelper
    include ActionView::Helpers::JavaScriptHelper
    include ActionView::Helpers::TagHelper
    include ActionView::Helpers::UrlHelper

    # Needed by link_to_remote
    def protect_against_forgery?
      false
    end

    # Works similarly to fields_for, but used for building forms for associated objects.
    # 
    # Automatically names fields to be compatible with the association_attributes= created by attribute_fu.
    #
    # An options hash can be specified to override the default behaviors.
    #
    # Options are:
    # <tt>:javascript</tt>  - Generate id placeholders for use with Prototype's Template class (this is how attribute_fu's add_associated_link works). 
    # <tt>:name</tt>        - Specify the singular name of the association (in singular form), if it differs from the class name of the object.
    #
    # Any other supplied parameters are passed along to fields_for.
    # 
    # Note: It is preferable to call render_associated_form, which will automatically wrap your form partial in a fields_for_associated call.
    #
    # source: attribute_fu
    def fields_for_associated(associated, *args, &block)
      conf            = args.last.is_a?(Hash) ? args.last : {}
      associated_name = extract_option_or_class_name(conf, :name, associated)
      name            = associated_base_name associated_name

      unless associated.new_record?
        name << "[#{associated.new_record? ? 'new' : associated.id}]"
        #name << "[existing][#{associated.id}]"
      else
        @new_objects ||= {}
        @new_objects[associated_name] ||= -1 # we want naming to start at 0
        identifier = !conf.nil? && conf[:javascript] ? '#{number}' : @new_objects[associated_name]+=1

        name << "[new][#{identifier}]"
      end

      @template.fields_for(name, *args.unshift(associated), &block)
    end

    # Renders the form of an associated object, wrapping it in a fields_for_associated call.
    #
    # The associated argument can be either an object, or a collection of objects to be rendered.
    #
    # An options hash can be specified to override the default behaviors.
    # 
    # Options are:
    # * <tt>:new</tt>        - specify a certain number of new elements to be added to the form. Useful for displaying a 
    #   few blank elements at the bottom.
    # * <tt>:name</tt>       - override the name of the association, both for the field names, and the name of the partial
    # * <tt>:partial</tt>    - specify the name of the partial in which the form is located.
    # * <tt>:fields_for</tt> - specify additional options for the fields_for_associated call
    # * <tt>:locals</tt>     - specify additional variables to be passed along to the partial
    # * <tt>:render</tt>     - specify additional options to be passed along to the render :partial call
    # * <tt>:local_name</tt> - specify the local name of associated model. defaults to partial's name
    #
    # source: attribute_fu
    def render_associated_form(associated, opts = {})
      associated = associated.is_a?(Array) ? associated : [associated] # preserve association proxy if this is one

      opts.symbolize_keys!
      (opts[:new] - associated.select(&:new_record?).length).times { associated.build } if opts[:new]

      if (not defined? @@forms_count) then
        @@forms_count = 0
      end
      
      unless associated.empty?
        name              = extract_option_or_class_name(opts, :name, associated.first)
        partial           = opts[:partial] || name
        local_assign_name = opts[:local_name] || partial.split('/').last.split('.').first.to_sym
  
        @@forms_count += associated.length

        associated.map do |element|
          fields_for_associated(element, (opts[:fields_for] || {}).merge(:name => name)) do |f|
            @template.render({:partial => "#{partial}", :locals => {local_assign_name => element, :f => f, :prefix => f.object_name}.merge(opts[:locals] || {})}.merge(opts[:render] || {}))
          end
        end
      end
    end

    # Creates a link that makes an ajax request to retreive a new associated
    # form.
    #
    # Parameters
    #  - name              the name to use as the link
    #  - associated_name   the name of the associated model we want to generate
    #                      a form for
    #  - url               the url from which we get the new associated form
    #  - opts              some options (see below)
    #  - html_options      some HTML options
    #
    # Options
    #  - :position         specify the position where the associated form should
    #                      be displayed. Defaults to :bottom
    #  - :html             some HTML options (overrided by html_options)
    def link_to_remote_associated_form(name, associated_name, url, opts = {}, html_options = nil)
      opts.symbolize_keys!
      
      position = opts.delete(:position) || :bottom
      html_options = html_options || opts.delete(:html)

      prefix = "#{associated_base_name associated_name.to_s}[new]['+multilevel_forms_count+']"

      link_to_remote name, { :url => url, :method => :get,
      :before => "if (typeof multilevel_forms_count == 'undefined') { multilevel_forms_count = 0; }; multilevel_forms_count++",
      :with => "'prefix=#{prefix}'", :update => associated_form_container_id, :position => position }, html_options
    end

    # Creates a link that removes the current associated form. This method must
    # be called from within an associated form.
    #
    # Parameters
    #  - name              the name to use as the link
    #  - html_options      some HTML options
    def link_to_delete_associated_form(name, html_options = nil)
      link_to_function name, "$('##{associated_form_id}').remove()", html_options
    end

    # Returns the id to be used for the container of the associated forms
    def associated_form_container_id
      '__multilevel_forms_container__' + @object_name.gsub(/\[|\]/, '_') + '_' + @object.id.to_s
    end

    # Returns the id to be used for the associated form
    def associated_form_id
      '__multilevel_forms_form__' + @object_name.gsub(/\[|\]/, '_') + '_' + @object.id.to_s
    end
    
    def initialize_form_counter
      if (not defined? @@forms_count) then 
        @@forms_count = 0
      end
      "<script type=\"text/javascript\">multilevel_forms_count = #{@@forms_count}</script>"
    end

    private
    # source: attribute_fu
    def extract_option_or_class_name(hash, option, object)
      (hash.delete(option) || object.class.name.split('::').last.underscore).to_s
    end
    
    # source: attribute_fu
    def associated_base_name(associated_name)
      "#{@object_name}[#{associated_name}_attributes]"
    end
  end
end