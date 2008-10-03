class CreateSourcesWrapperInstances < ActiveRecord::Migration
  def self.up
    create_table :sources_wrapper_instances, :id => false do |t|
      t.integer :wrapper_instance_id
      t.integer :source_id
    end
  end

  def self.down
    drop_table :sources_wrapper_instances
  end
end
