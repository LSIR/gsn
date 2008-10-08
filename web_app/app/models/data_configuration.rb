class DataConfiguration < ActiveRecord::Base
  belongs_to :user
  has_many :data_selections
end
