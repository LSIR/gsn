ActiveRecord::Base.class_eval { include MultilevelForms::Associations }
ActionView::Helpers::FormBuilder.class_eval { include MultilevelForms::FormBuilderHelpers }
