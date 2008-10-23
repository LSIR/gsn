# This file is auto-generated from the current state of the database. Instead of editing this file, 
# please use the migrations feature of Active Record to incrementally modify your database, and
# then regenerate this schema definition.
#
# Note that this schema.rb definition is the authoritative source for your database schema. If you need
# to create the application database on another system, you should be using db:schema:load, not running
# all the migrations from scratch. The latter is a flawed and unsustainable approach (the more migrations
# you'll amass, the slower it'll run and the greater likelihood for issues).
#
# It's strongly recommended to check this file into your version control system.

ActiveRecord::Schema.define(:version => 20081010142403) do

  create_table "__1660901570", :primary_key => "PK", :force => true do |t|
    t.integer "timed",                      :limit => 8, :null => false
    t.integer "HEAP",                       :limit => 8
    t.integer "NON_HEAP",                   :limit => 8
    t.integer "PENDING_FINALIZATION_COUNT"
  end

  add_index "__1660901570", ["timed"], :name => "__1660901570_INDEX", :unique => true

  create_table "__1824071790", :id => false, :force => true do |t|
    t.integer "timed", :limit => 8, :null => false
    t.integer "heap",  :limit => 8
  end

  create_table "__sql_view_helper_table__", :primary_key => "PK", :force => true do |t|
    t.integer "timed", :limit => 8,  :null => false
    t.string  "UID",   :limit => 17
  end

  add_index "__sql_view_helper_table__", ["timed"], :name => "__sql_view_helper_table___INDEX"

  create_table "addressing_types", :force => true do |t|
    t.string   "name"
    t.string   "description"
    t.datetime "created_at"
    t.datetime "updated_at"
  end

  add_index "addressing_types", ["name"], :name => "index_addressing_types_on_name", :unique => true

  create_table "addressings", :force => true do |t|
    t.string   "value"
    t.integer  "addressing_type_id"
    t.integer  "virtual_sensor_id"
    t.datetime "created_at"
    t.datetime "updated_at"
  end

  create_table "bp_mmdatavs2", :primary_key => "PK", :force => true do |t|
    t.integer "timed", :limit => 8, :null => false
    t.integer "H2",    :limit => 8
  end

  add_index "bp_mmdatavs2", ["timed"], :name => "bp_mmdatavs2_INDEX"

  create_table "data_types", :force => true do |t|
    t.string   "name"
    t.string   "description"
    t.datetime "created_at"
    t.datetime "updated_at"
  end

  create_table "deployments", :force => true do |t|
    t.integer  "admin_id"
    t.boolean  "private"
    t.string   "name"
    t.text     "description"
    t.datetime "created_at"
    t.datetime "updated_at"
  end

  add_index "deployments", ["name"], :name => "index_deployments_on_name", :unique => true

  create_table "deployments_users", :id => false, :force => true do |t|
    t.integer "deployment_id", :null => false
    t.integer "user_id",       :null => false
  end

  create_table "output_formats", :force => true do |t|
    t.string   "name"
    t.string   "description"
    t.integer  "data_type_id"
    t.integer  "processor_id"
    t.datetime "created_at"
    t.datetime "updated_at"
  end

  create_table "pc_inits", :force => true do |t|
    t.string   "name"
    t.string   "description"
    t.string   "default_value"
    t.boolean  "optional"
    t.integer  "data_type_id"
    t.integer  "processor_id"
    t.datetime "created_at"
    t.datetime "updated_at"
  end

  create_table "pc_instances", :force => true do |t|
    t.string   "name"
    t.string   "web_password"
    t.integer  "processor_id"
    t.datetime "created_at"
    t.datetime "updated_at"
  end

  add_index "pc_instances", ["name"], :name => "index_pc_instances_on_name", :unique => true

  create_table "pc_parameters", :force => true do |t|
    t.integer  "pc_init_id"
    t.integer  "pc_instance_id"
    t.string   "value"
    t.string   "note"
    t.datetime "created_at"
    t.datetime "updated_at"
  end

  create_table "processors", :force => true do |t|
    t.string   "name"
    t.string   "description"
    t.string   "class_name"
    t.datetime "created_at"
    t.datetime "updated_at"
  end

  add_index "processors", ["name"], :name => "index_processors_on_name", :unique => true

  create_table "sources", :force => true do |t|
    t.string   "name"
    t.string   "window_size"
    t.string   "sliding"
    t.float    "load_shedding"
    t.text     "query"
    t.integer  "stream_id"
    t.datetime "created_at"
    t.datetime "updated_at"
  end

  create_table "sources_wrapper_instances", :id => false, :force => true do |t|
    t.integer "wrapper_instance_id"
    t.integer "source_id"
  end

  create_table "streams", :force => true do |t|
    t.string   "name"
    t.text     "query"
    t.integer  "virtual_sensor_id"
    t.datetime "created_at"
    t.datetime "updated_at"
  end

  create_table "users", :force => true do |t|
    t.string   "email",           :limit => 80,                     :null => false
    t.string   "hashed_password"
    t.string   "salt"
    t.string   "type",                          :default => "User", :null => false
    t.datetime "created_at"
    t.datetime "updated_at"
  end

  add_index "users", ["email"], :name => "index_users_on_email", :unique => true

  create_table "virtual_sensors", :force => true do |t|
    t.string   "name"
    t.integer  "priority"
    t.boolean  "protected"
    t.integer  "pool_size"
    t.string   "storage_size"
    t.boolean  "unique_timestamp"
    t.float    "load_shedding"
    t.integer  "pc_instance_id"
    t.integer  "deployment_id"
    t.datetime "created_at"
    t.datetime "updated_at"
  end

  add_index "virtual_sensors", ["name"], :name => "index_virtual_sensors_on_name", :unique => true

  create_table "web_commands", :force => true do |t|
    t.string   "name"
    t.string   "description"
    t.string   "rule"
    t.integer  "web_input_id"
    t.datetime "created_at"
    t.datetime "updated_at"
  end

  create_table "web_inputs", :force => true do |t|
    t.string   "name"
    t.string   "description"
    t.integer  "processor_id"
    t.datetime "created_at"
    t.datetime "updated_at"
  end

  create_table "wrapper_inits", :force => true do |t|
    t.string   "name"
    t.string   "description"
    t.string   "default_value"
    t.boolean  "optional"
    t.integer  "data_type_id"
    t.integer  "wrapper_id"
    t.datetime "created_at"
    t.datetime "updated_at"
  end

  create_table "wrapper_instances", :force => true do |t|
    t.string   "name"
    t.text     "description"
    t.integer  "wrapper_id"
    t.integer  "source_id"
    t.datetime "created_at"
    t.datetime "updated_at"
  end

  add_index "wrapper_instances", ["name"], :name => "index_wrapper_instances_on_name", :unique => true

  create_table "wrapper_parameters", :force => true do |t|
    t.integer  "wrapper_init_id"
    t.integer  "wrapper_instance_id"
    t.string   "value"
    t.string   "note"
    t.datetime "created_at"
    t.datetime "updated_at"
  end

  create_table "wrappers", :force => true do |t|
    t.string   "name"
    t.string   "description"
    t.string   "class_name"
    t.datetime "created_at"
    t.datetime "updated_at"
  end

  add_index "wrappers", ["name"], :name => "index_wrappers_on_name", :unique => true

end
