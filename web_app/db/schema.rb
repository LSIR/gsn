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

ActiveRecord::Schema.define(:version => 20080819141844) do

  create_table "_2121924095", :id => false, :force => true do |t|
    t.integer "PK",        :limit => 20, :null => false
    t.integer "timed",     :limit => 20, :null => false
    t.integer "TIMESTAMP", :limit => 20
    t.integer "RECORD",    :limit => 11
    t.float   "RH"
    t.float   "TA"
    t.float   "VW_WVC_1"
    t.float   "VW_WVC_2"
    t.float   "VW_WVC_3"
    t.float   "VW_WVC_4"
    t.float   "VW_MAX"
    t.float   "TSS"
    t.float   "HS1"
    t.float   "SW_IN"
    t.float   "SW_OUT"
    t.float   "LW_IN"
    t.float   "LW_OUT"
  end

  add_index "_2121924095", ["pk"], :name => "primary", :unique => true
  add_index "_2121924095", ["timed"], :name => "_2121924095_index", :unique => true

  create_table "__sql_view_helper_table__", :id => false, :force => true do |t|
    t.integer "PK",    :limit => 20, :null => false
    t.integer "timed", :limit => 20, :null => false
    t.string  "UID",   :limit => 17
  end

  add_index "__sql_view_helper_table__", ["pk"], :name => "primary", :unique => true
  add_index "__sql_view_helper_table__", ["timed"], :name => "__sql_view_helper_table___index"

  create_table "sensorscope_wannengrat_n10", :id => false, :force => true do |t|
    t.integer "PK",              :limit => 20, :null => false
    t.integer "timed",           :limit => 20, :null => false
    t.float   "AIRHUMIDITY"
    t.float   "AIRTEMPERATURE"
    t.integer "NTWCOSTTOBS",     :limit => 6
    t.integer "NTWSENDERID",     :limit => 6
    t.float   "RAINMETER"
    t.integer "REPORTERID",      :limit => 6
    t.float   "SKINTEMPERATURE"
    t.integer "TIMESTAMP",       :limit => 20
    t.float   "SOILMOISTURE"
    t.float   "SOLARRADIATION"
    t.integer "TSPHOPCOUNT",     :limit => 6
    t.integer "TSPPACKETSN",     :limit => 6
    t.float   "WATERMARK"
    t.float   "WINDDIRECTION"
    t.float   "WINDSPEED"
  end

  add_index "sensorscope_wannengrat_n10", ["pk"], :name => "primary", :unique => true
  add_index "sensorscope_wannengrat_n10", ["timed"], :name => "sensorscope_wannengrat_n10_index", :unique => true

  create_table "sensorscope_wannengrat_n11", :id => false, :force => true do |t|
    t.integer "PK",              :limit => 20, :null => false
    t.integer "timed",           :limit => 20, :null => false
    t.float   "AIRHUMIDITY"
    t.float   "AIRTEMPERATURE"
    t.integer "NTWCOSTTOBS",     :limit => 6
    t.integer "NTWSENDERID",     :limit => 6
    t.float   "RAINMETER"
    t.integer "REPORTERID",      :limit => 6
    t.float   "SKINTEMPERATURE"
    t.integer "TIMESTAMP",       :limit => 20
    t.float   "SOILMOISTURE"
    t.float   "SOLARRADIATION"
    t.integer "TSPHOPCOUNT",     :limit => 6
    t.integer "TSPPACKETSN",     :limit => 6
    t.float   "WATERMARK"
    t.float   "WINDDIRECTION"
    t.float   "WINDSPEED"
  end

  add_index "sensorscope_wannengrat_n11", ["pk"], :name => "primary", :unique => true
  add_index "sensorscope_wannengrat_n11", ["timed"], :name => "sensorscope_wannengrat_n11_index", :unique => true

  create_table "sensorscope_wannengrat_n12", :id => false, :force => true do |t|
    t.integer "PK",              :limit => 20, :null => false
    t.integer "timed",           :limit => 20, :null => false
    t.float   "AIRHUMIDITY"
    t.float   "AIRTEMPERATURE"
    t.integer "NTWCOSTTOBS",     :limit => 6
    t.integer "NTWSENDERID",     :limit => 6
    t.float   "RAINMETER"
    t.integer "REPORTERID",      :limit => 6
    t.float   "SKINTEMPERATURE"
    t.integer "TIMESTAMP",       :limit => 20
    t.float   "SOILMOISTURE"
    t.float   "SOLARRADIATION"
    t.integer "TSPHOPCOUNT",     :limit => 6
    t.integer "TSPPACKETSN",     :limit => 6
    t.float   "WATERMARK"
    t.float   "WINDDIRECTION"
    t.float   "WINDSPEED"
  end

  add_index "sensorscope_wannengrat_n12", ["pk"], :name => "primary", :unique => true
  add_index "sensorscope_wannengrat_n12", ["timed"], :name => "sensorscope_wannengrat_n12_index", :unique => true

  create_table "ss_mem_vs", :id => false, :force => true do |t|
    t.integer "PK",                         :limit => 20, :null => false
    t.integer "timed",                      :limit => 20, :null => false
    t.integer "HEAP_MEMORY_USAGE",          :limit => 20
    t.integer "NON_HEAP_MEMORY_USAGE",      :limit => 20
    t.integer "PENDING_FINALIZATION_COUNT", :limit => 11
  end

  add_index "ss_mem_vs", ["pk"], :name => "primary", :unique => true
  add_index "ss_mem_vs", ["timed"], :name => "ss_mem_vs_index", :unique => true

  create_table "tramm_meadows_vs", :id => false, :force => true do |t|
    t.integer "PK",             :limit => 20, :null => false
    t.integer "timed",          :limit => 20, :null => false
    t.integer "TIMESTAMP",      :limit => 20
    t.integer "RECORD",         :limit => 11
    t.float   "BATT_VOLT_AVG"
    t.float   "PANEL_TEMP_AVG"
    t.float   "TOPPVWC_1"
    t.float   "TOPPVWC_2"
    t.float   "TOPPVWC_3"
    t.float   "TOPPVWC_4"
    t.float   "TOPPVWC_5"
    t.float   "TOPPVWC_6"
  end

  add_index "tramm_meadows_vs", ["pk"], :name => "primary", :unique => true
  add_index "tramm_meadows_vs", ["timed"], :name => "tramm_meadows_vs_index", :unique => true

  create_table "users", :force => true do |t|
    t.string   "email",           :limit => 60, :null => false
    t.string   "hashed_password",               :null => false
    t.string   "salt",                          :null => false
    t.datetime "created_at",                    :null => false
    t.datetime "updated_at",                    :null => false
  end

  create_table "wan7qualitytest", :id => false, :force => true do |t|
    t.integer "PK",       :limit => 20, :null => false
    t.integer "timed",    :limit => 20, :null => false
    t.float   "RH"
    t.float   "TA"
    t.float   "VW_WVC_1"
    t.float   "VW_WVC_2"
    t.float   "VW_WVC_3"
    t.float   "VW_WVC_4"
    t.float   "VW_MAX"
    t.float   "TSS"
    t.float   "HS1"
    t.float   "SW_IN"
    t.float   "SW_OUT"
    t.float   "LW_IN"
    t.float   "LW_OUT"
  end

  add_index "wan7qualitytest", ["pk"], :name => "primary", :unique => true
  add_index "wan7qualitytest", ["timed"], :name => "wan7qualitytest_index", :unique => true

end
