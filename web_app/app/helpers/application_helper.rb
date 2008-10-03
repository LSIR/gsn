# Methods added to this helper will be available to all templates in the application.
module ApplicationHelper

  # This method adds an input to the given HTML form, for one column name of one
  # Active Record class.
  # The HTML input is defined by the type of the column
  ######
  # eg.
  # <%= build_html_output(form, Processor, :name) %>
  # given the processor model and the field called :name in the Processor model generates the following HTML code
  # <input id="processor_name" name="processor[name]" size="30" type="text" />
  def build_html_output(form,active_record_class, column_name)
    c_name = column_name.to_s
    column = active_record_class.columns_hash[c_name]
    unless column then
      puts "column: #{c_name} not found in the active record: #{active_record_class}"
      return
    end
    field_type = column.type
    case field_type
    when :binary
    when :decimal
    when :float
    when :integer
    when :string
      form.text_field(c_name)
    when :date
    when :datetime
    when :time
    when :timestamp
      form.text_field(c_name) #TODO Use a specific UI to handle dates
    when :text
      form.text_area(c_name, :cols => 20, :rows => 40)
    when :boolean
      form.select :optional, [['No' , :false], ['Yes' , :true]]
    else
      puts "column type: #{field_type} for field: #{c_name} is not implemented yet."
    end
  end

  def has_specified_text(controller)
    FileTest.exist?(File.join(RAILS_ROOT, 'app', 'views', controller.controller_name, '_' + controller.controller_name + '.rhtml'))
  end

end
