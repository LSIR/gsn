require 'test_helper'

class WrapperTest < ActiveSupport::TestCase

  def test_correct_instanciation
    wrapper = Wrapper.new(:name => 'tinyos', :class_name => 'gsn.wrapper.TestWrapper', :description => 'Test wrapper')
    assert wrapper.valid?
    wrapper = Wrapper.new(:name => 'tinyos_wrapper', :class_name => 'gsn.wrapper.TestWrapper', :description => 'Test wrapper')
    assert wrapper.valid?
    wrapper = Wrapper.new(:name => 'tinyos_wrapper', :class_name => 'TestWrapper', :description => 'Test wrapper')
    assert wrapper.valid?
    wrapper = Wrapper.new(:name => 'tinyos_wrapper', :class_name => 'testWrapper', :description => 'Test wrapper')
    assert wrapper.valid?
    wrapper = Wrapper.new(:name => 'tinyos_wrapper', :class_name => 'testWrapper456', :description => 'Test wrapper')
    assert wrapper.valid?
    wrapper = Wrapper.new(:name => 'tinyos_wrapper', :class_name => 'testWr235apper456', :description => 'Test wrapper')
    assert wrapper.valid?
    wrapper = Wrapper.new(:name => 'tinyos_wrapper', :class_name => 'testWr235a_pper456', :description => 'Test wrapper')
    assert wrapper.valid?
    wrapper = Wrapper.new(:name => 'tinyos_wrapper', :class_name => 'dfg45.testWr235ap_per456', :description => 'Test wrapper')
    assert wrapper.valid?
  end

  def test_incorrect_instanciation
    wrapper = Wrapper.new(:name => 'tinyos wrapper', :class_name => 'gsn.wrapper.TestWrapper', :description => 'Test wrapper')
    assert !wrapper.valid?
    wrapper = Wrapper.new(:name => " tinyoswrapper", :class_name => 'gsn.wrapper.TestWrapper', :description => 'Test wrapper')
    assert ! wrapper.valid?
    wrapper = Wrapper.new(:name => "tiny\toswrapper", :class_name => 'gsn.wrapper.TestWrapper', :description => 'Test wrapper')
    assert ! wrapper.valid?
    wrapper = Wrapper.new(:name => "tiny\roswrapper", :class_name => 'gsn.wrapper.TestWrapper', :description => 'Test wrapper')
    assert ! wrapper.valid?
    wrapper = Wrapper.new(:name => "tiny|oswrapper", :class_name => 'gsn.wrapper.TestWrapper', :description => 'Test wrapper')
    assert ! wrapper.valid?
    wrapper = Wrapper.new(:name => "tiny@oswrapper", :class_name => 'gsn.wrapper.TestWrapper', :description => 'Test wrapper')
    assert ! wrapper.valid?
    wrapper = Wrapper.new(:name => "tiny/oswrapper", :class_name => 'gsn.wrapper.TestWrapper', :description => 'Test wrapper')
    assert ! wrapper.valid?
    wrapper = Wrapper.new(:name => "tiny()oswrapper", :class_name => 'gsn.wrapper.TestWrapper', :description => 'Test wrapper')
    assert ! wrapper.valid?
    #
    wrapper = Wrapper.new(:name => 'tinyos_wrapper', :class_name => 'gsn.wrapper .TestWrapper', :description => 'Test wrapper')
    assert ! wrapper.valid?
    wrapper = Wrapper.new(:name => 'tinyos_wrapper', :class_name => 'gsn.wrapper TestWrapper', :description => 'Test wrapper')
    assert ! wrapper.valid?
    wrapper = Wrapper.new(:name => 'tinyos_wrapper', :class_name => 'Test Wrapper', :description => 'Test wrapper')
    assert ! wrapper.valid?
    wrapper = Wrapper.new(:name => 'tinyos_wrapper', :class_name => 'gsn.wrap-per.TestWrapper', :description => 'Test wrapper')
    assert ! wrapper.valid?
    wrapper = Wrapper.new(:name => 'tinyos_wrapper', :class_name => 'gsn.wrap%per.TestWrapper', :description => 'Test wrapper')
    assert ! wrapper.valid?
    wrapper = Wrapper.new(:name => 'tinyos_wrapper', :class_name => 'gsn.wrapper.TestWrapper..test', :description => 'Test wrapper')
    assert ! wrapper.valid?
    wrapper = Wrapper.new(:name => 'tinyos_wrapper', :class_name => 'gsn.wrapper.TestWrapper.test.', :description => 'Test wrapper')
    assert ! wrapper.valid?
    wrapper = Wrapper.new(:name => 'tinyos_wrapper', :class_name => '.gsn.wrapper.TestWrapper', :description => 'Test wrapper')
    assert ! wrapper.valid?
  end
end