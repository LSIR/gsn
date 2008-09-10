class Wrapper < Parametrizable
  belongs_to :source, :foreign_key=>:resource_id
end