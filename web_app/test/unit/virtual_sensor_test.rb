require 'test_helper'

class VirtualSensorTest < ActiveSupport::TestCase

  def test_correct_instanciation
    virtual_sensor = VirtualSensor.new(:name => 'my_vs_01', :priority => 10, :protected => false, :pool_size => 3, :storage_size => 10, :unique_timestamp => true, :load_shedding => 0.4)
    assert virtual_sensor.valid?
    virtual_sensor = VirtualSensor.new(:name => 'my_vs_01', :priority => 10, :protected => false, :pool_size => 3, :storage_size => 10, :unique_timestamp => true, :load_shedding => 0)
    assert virtual_sensor.valid?
    virtual_sensor = VirtualSensor.new(:name => 'my_vs_01', :priority => 10, :protected => false, :pool_size => 3, :storage_size => 10, :unique_timestamp => true, :load_shedding => 1)
    assert virtual_sensor.valid?
    virtual_sensor = VirtualSensor.new(:name => 'my_vs_01', :priority => 10, :protected => false, :pool_size => 3, :storage_size => 10, :unique_timestamp => true, :load_shedding => '1')
    assert virtual_sensor.valid?
    #
    virtual_sensor = VirtualSensor.new(:name => 'my_vs_01', :priority => 10, :protected => false, :pool_size => 3, :storage_size => '10h', :unique_timestamp => true, :load_shedding => 1)
    assert virtual_sensor.valid?
    virtual_sensor = VirtualSensor.new(:name => 'my_vs_01', :priority => 10, :protected => false, :pool_size => 3, :storage_size => '10', :unique_timestamp => true, :load_shedding => 1)
    assert virtual_sensor.valid?
    virtual_sensor = VirtualSensor.new(:name => 'my_vs_01', :priority => 10, :protected => false, :pool_size => 3, :storage_size => '10d', :unique_timestamp => true, :load_shedding => 1)
    assert virtual_sensor.valid?
  end

  def test_incorrect_instanciation
    virtual_sensor = VirtualSensor.new(:name => 'my_vs_01', :priority => 10, :protected => false, :pool_size => 3, :storage_size => 10, :unique_timestamp => true, :load_shedding => 3.4)
    assert ! virtual_sensor.valid?
    virtual_sensor = VirtualSensor.new(:name => 'my_vs_01', :priority => 10, :protected => false, :pool_size => 3, :storage_size => 10, :unique_timestamp => true, :load_shedding => -0.4)
    assert ! virtual_sensor.valid?
    virtual_sensor = VirtualSensor.new(:name => 'my_vs_01', :priority => 10, :protected => false, :pool_size => 3, :storage_size => 10, :unique_timestamp => true, :load_shedding => 5)
    assert ! virtual_sensor.valid?
    #
    virtual_sensor = VirtualSensor.new(:name => 'my_vs_01', :priority => 10, :protected => false, :pool_size => 3, :storage_size => '10 h', :unique_timestamp => true, :load_shedding => 5)
    assert ! virtual_sensor.valid?
    virtual_sensor = VirtualSensor.new(:name => 'my_vs_01', :priority => 10, :protected => false, :pool_size => 3, :storage_size => '10_h', :unique_timestamp => true, :load_shedding => 5)
    assert ! virtual_sensor.valid?
    virtual_sensor = VirtualSensor.new(:name => 'my_vs_01', :priority => 10, :protected => false, :pool_size => 3, :storage_size => '10_h', :unique_timestamp => true, :load_shedding => 5)
    assert ! virtual_sensor.valid?
    virtual_sensor = VirtualSensor.new(:name => 'my_vs_01', :priority => 10, :protected => false, :pool_size => 3, :storage_size => '10hh', :unique_timestamp => true, :load_shedding => 5)
    assert ! virtual_sensor.valid?
    virtual_sensor = VirtualSensor.new(:name => 'my_vs_01', :priority => 10, :protected => false, :pool_size => 3, :storage_size => '10hd', :unique_timestamp => true, :load_shedding => 5)
    assert ! virtual_sensor.valid?
  end

end
