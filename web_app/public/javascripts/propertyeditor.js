// Contains all property editors loaded into the page
var property_editors = [];

// Where all images are found
var img_folder = "/images/"

// Some styling information
var td_style = 'margin: 0; padding: 0; padding-left: 10px; font-family: Verdana, Arial; border: none; border-bottom :1px solid #CCC;';
var table_param = 'cellpadding="0" cellspacing="0" width="410"';
var table_style = 'border: 1px solid #CCC; margin: 0; padding: 0;'

function PropertyEditor(owner_type, owner_id, div_id, allow_editing) {
	// Arrays for storing all the data
	var groups = [];
	var properties = []
	var values = [];
	
	var pEditor = this;
	
	// Used when handling the row saying "No values found"
	var no_values_row_shown = false;
 	var num_rows = 0;
	
	// Store this property editor array with all editors
	property_editors["" + owner_id] = this;

	// True if user is about to add an property value
	var currently_adding = false;

	// Variables for easily accessing DOM elements
	var div = null;
	var value_div = null;
	var value_table = null;
	var top_table = null;
	
	function init() {
		div = $("#" + div_id);

		// Insert the table which will contain all the values
		div.append('<div id="value_div_' + owner_id + '">' +
				   '<table id="value_table_' + owner_id + '"' + table_param + ' style="' + table_style + ' border-top: none; border-bottom: none;"></table></div>');
		
		value_div = $("#value_div_" + owner_id);
		value_table = $("#value_table_" + owner_id);
		
		// Add the top table
		var html = '<table ' + table_param + ' style="' + table_style + ' border-bottom: none;" id="top_table_' + owner_id + '">' + 
				   '<tr><td width="40" style="margin: 0; padding: 5px; padding-left: 20px; background-color: #CCC"><img src="' + img_folder + 'property_editor_loading_big.gif" /></td>' + 
				   '<td style="margin: 0; padding: 5px; font-family: Verdana, Arial; font-size: 14pt; background-color: #CCC">Loading...</td></tr></table>'
		value_div.prepend(html);
		top_table = $("#top_table_" + owner_id);
		
		$.getJSON("/data/get_properties/" + owner_id + "?type=" + owner_type,
	        function(data){
				groups = data[0];
				properties = data[1];
				values = data[2];
				
				top_table.empty();
				
				var html = '<tr style="background-color: #CCC; padding: 0; margin: 0;"><td style="margin: 0; padding: 10px; border-right: none;"><div style="font-family: Verdana, Arial; font-size: 10pt;">' + 
						   'Group: &nbsp;<select id="group_select_' + owner_id + '" onChange="_groupSelectChanged(' + owner_id + ');" ' +
						   'style="margin: 0px; padding: 4px; border: 1px solid #999;">'

				for (var group_id in groups)
					html += '<option value="' + group_id + '">' + groups[group_id] + '</option>';

				html += '</select></td>'
				html += '<td style="margin: 0; padding: 10px; border-left: none; text-align: right;">&nbsp;'
				
				if (allow_editing == true)
						html += '<a style="cursor:pointer;cursor:hand; font-family: Arial, Verdana; font-size: 11pt; color: #000763" onClick="_newValue(' + owner_id + ')">Add value</a>';
				
				html += '</td></tr>';
				
				top_table.append(html);
				
				populateTable(getSelectedGroup());
	        }
		);
	}
	
	function getSelectedGroup() {
		return $("#group_select_" + owner_id).val();
	}

	function populateTable(group_id) {
		// Reset the whole table
		value_table.empty();
		num_rows = 0;
		no_values_row_shown = false;

		for (var value_id in values) {
			
			// Don't add row if the id is "new", the value is null
			// or the value doesn't belong to selected group
			if (value_id == "new" || 
				values[value_id] == null || 
				properties[values[value_id]["property_id"]]["group_id"] != group_id)
				continue;
			
			// Add the value to the table
			addRow(null, null, value_id, false);
			
			// Add edit delete buttons
			pEditor.editValue(value_id, false);
		}
		
		updateNoValuesRow();
	}
	
	function populatePropertySelect() {
		var select = $('#value_prop_select_' + owner_id);
		
		for (var prop in properties) {
			if (properties[prop]["group_id"] != getSelectedGroup())
				continue;
			select.append('<option value="' + prop + '">' + properties[prop]["name"] + '</option>');
		}
	}
	
	function addRow(prop, value, value_id, add_on_top) {
		if (prop == null)
			prop = properties[values[value_id]["property_id"]]["name"];
		
		if (value == null)
			value = values[value_id]["value"];
		
		var html = '<tr id="value_row_' + owner_id + '_' + value_id + '" style="padding: 0; margin: 0; height: 35px;">';
		html += '<td width="100" style="border-right: none; font-size: 8pt; font-weight:bold' + td_style + '">' + prop + '</td>';
		
		html +=	'<td style="border-right: none; border-left: none;' + td_style + '"><div style="position: relative;" id="value_div_' + owner_id + '_' + value_id + '">' + value + '</div></td>' + 
				'<td id="value_buttons_' + owner_id + '_' + value_id + '" width="50" style="border-left: none; margin: 0; padding: 0;' + td_style + '">&nbsp;</td></tr>';
		
		if (add_on_top == true)
			value_table.prepend(html);
		else
			value_table.append(html);

		num_rows++;
		
		if (value_id != "novalues")
			updateNoValuesRow();
	}
	
	// Removes a row belonging to value with specified id
	function removeRow(id) {
		$("#value_row_" + owner_id + '_' + id).remove();
		num_rows--;

		if (id != "novalues")
			updateNoValuesRow();
	}
	
	// Check if the no values row should be added/removed
	function updateNoValuesRow() {
		if (num_rows == 0) {
			addRow("&nbsp;", "<em>No values found</em>", "novalues", true);
			no_values_row_shown = true;
		}
		else if (no_values_row_shown == true) { 
			removeRow("novalues");
			no_values_row_shown = false;
		}
	}
	
	// Shows/Hides an loading indicator on a specific row in the table
	function loadingIndicator(id, show) {
		if (show == true) {
			// Get the position of the indicator
			var left = parseInt($('#value_div_' + owner_id + '_' + id).width()) - 30;
			var top = (parseInt($('#value_div_' + owner_id + '_' + id).height()) - 16) / 2;

			var html = '<div id="value_loading_' + owner_id + '_' + id + '" style="position: absolute; top:' + top  + 'px; left: ' + left + 'px;">' + 
					   '<img src="' + img_folder + 'property_editor_loading.gif" /></div>';
			
			$('#value_div_' + owner_id + '_' + id).append(html);
		}
		else
			$('#value_loading_' + owner_id + '_' + id).remove();	
	}
	
	// Different colors used for highlighting a row
	var ERROR = "#ff7c7c"; var SUCCESS = "#a4c29d"; var NORMAL = "#ffff99";
	function highlightRow(value_id, color) {
		$('#value_row_' + owner_id + '_' + value_id).effect("highlight", {color: color}, 1000);
	}
	
	this.deleteValue = function (id) {
		loadingIndicator(id, true);

		$.getJSON("/data/delete_value/" + id,
	        function(data){
				if (data == "success") {
					// remove the value from values
					values[id] = null;
					
					// delete the row from the table
					removeRow(id);
				}
				else {
					// if delete failed just remove loading indicator and highlight row
					loadingIndicator(id, false);
					highlightRow(id, ERROR);
				}
	        }
		);
	}
	
	// Goes into or out if editing mode
	this.editValue = function (id, edit) {
		var value = null;
		var html = "";
		
		// Cancel editing, remove input box and restore the old value
		if (edit == false || allow_editing == false) {
			// If stop editing a new item just remove the whole row
			if (id == "new") {
				removeRow(id);
				currently_adding = false;
				return;
			}

			// Add the value to the div in the table
			$('#value_div_' + owner_id + '_' + id).empty();
			$('#value_div_' + owner_id + '_' + id).append(values[id]["value"]);

			// Add edit and remove button
			if (allow_editing)
				html = '<a style="cursor:pointer;cursor:hand;" onClick="_editValue(\'' + owner_id + '\', \'' + id + '\', true)"><img src="' + img_folder + 'property_editor_edit.gif" /></a>' +
					   '<a style="cursor:pointer;cursor:hand;" onClick="_deleteValue(\'' + owner_id +  '\', \'' + id + '\')"><img src="' + img_folder + 'property_editor_delete.gif" /></a>';
			else
				html = '&nbsp;'

			$('#value_buttons_' + owner_id + '_' + id).empty();
			$('#value_buttons_' + owner_id + '_' + id).append(html);
		}
		
		// Edit the value, add an input box and corresponding buttons
		else {
			// Do a highlightning effect on the whole row
			highlightRow(id, NORMAL);
			
			// Get and save the value
			value = values[id]["value"];
			$('#value_div_' + owner_id + '_' + id).empty();

			// Change the buttons to Submit and Cancel buttons
			html = '<a style="cursor:pointer;cursor:hand;" onClick="_submitValue(\'' + owner_id + '\', \'' + id + '\')"><img src="' + img_folder + 'property_editor_send.gif" /></a>' +
					'<a style="cursor:pointer;cursor:hand;" onClick="_editValue(\'' + owner_id +  '\', \'' + id + '\', false)"><img src="' + img_folder + 'property_editor_delete.gif" /></a>';
			$('#value_buttons_' + owner_id + '_' + id).empty();
			$('#value_buttons_' + owner_id + '_' + id).append(html);
			
			// Get the width of the div containing the value
			var width = parseInt($('#value_div_' + owner_id + '_' + id).width()) -20;
			html = '<input id="value_input_' + owner_id + '_' + id + '" style="width: ' + width + 'px; border: 1px solid #999; margin: 0; padding: 5px;" ' +
				   'type="text" value="' + value + '" />';
			$('#value_div_' + owner_id + '_' + id).append(html);
		}
	}

	this.submitValue = function (id) {
		// retrieve the value from the input field
		var value = $('#value_input_' + owner_id + '_' + id).val();
		
		// Show loading indicator	
		loadingIndicator(id, true);

		// If id is "new" then a value is added to the db
		if (id == "new") {
			// Get the selected owner id
			var property_id = $('#value_prop_select_' + owner_id).val();
			
			$.getJSON("/data/add_value/" + id + "?value=" + value + '&owner_type=' + owner_type + '&owner_id=' + owner_id + '&property_id=' + property_id,
		        function(data){
					if (data != "failure") {
						
						// id is returned
						id = data;

						// save the value in values array
						values[id] = [];
						values[id]["value"] = value;
						values[id]["property_id"] = property_id;
						currently_adding = false;
						
						addRow(null, null, id, true);
						
						removeRow("new");

						pEditor.editValue(id, false);
						highlightRow(id, SUCCESS);
					}
					else {
						highlightRow(id, ERROR);
						loadingIndicator(id, false);
					}
		        }
			);
		}
		// else the value is updated
		else {
			$.getJSON("/data/update_value/" + id + "?value=" + value,
		        function(data){
					if (data == "success") {
						// update the values array
						values[id]["value"] = value;
						
						// switch to non-editing mode
						pEditor.editValue(id, false);

						// confirm by a highlighting the row
						highlightRow(id, SUCCESS);
					}
					else {
						// if failed highlight the row and remove the loading indicator
						highlightRow(id, ERROR);
						loadingIndicator(id, false);
					}
		        }
			);
		}
	}

	this.newValue = function () {
		// If user is already about to add a value just higlight the row
		if (currently_adding == true) {
			highlightRow("new", NORMAL);
			return;
		}

		// create value with id "new" in values array
		values["new"] = [];
		values["new"]["value"] = "";
		values["new"]["property_id"] = -1;
		
		// Add the input row with a select box
		addRow('<select id="value_prop_select_' + owner_id + '" style="margin: 0px; padding: 4px; border: 1px solid #999;">', "", "new", true);
		
		// Go into editing mode
		pEditor.editValue("new", true);
		
		// Populate the select with properties
		populatePropertySelect();
		
		currently_adding = true;

	}

	// Called when user changes group
	this.groupSelectChanged = function () {
		// if user is about to add a value cancel it
		if (currently_adding == true) {
			pEditor.editValue("new", false);
			values["new"] = null;
		}
		
		// populate the table with values
		populateTable(getSelectedGroup());
	}

	init();
}

function _groupSelectChanged(owner_id) {
	property_editors[owner_id].groupSelectChanged();
}

function _deleteValue(owner_id, value_id) {
	property_editors[owner_id].deleteValue(value_id);
}

function _editValue(owner_id, value_id, edit) {
	property_editors[owner_id].editValue(value_id, edit);
}

function _submitValue(owner_id, value_id) {
	property_editors[owner_id].submitValue(value_id);
}

function _newValue(owner_id) {
	property_editors[owner_id].newValue();
}