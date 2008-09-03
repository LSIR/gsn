/* Use this to add behavior to DOM elements once the page is loaded. */
jQuery(function() {

    /* Use the Flexigrid jQuery plugin to show the list of VS */

    $("#flexi-search-bar").flexigrid
	(
	{
			url: 'populate_search_list_of_vs',
			dataType: 'json',
			colModel : [
				{display: 'Name', name: 'get_name', width: 160, sortable: false, align: 'left', hide: false },
				{display: 'Last Update', name: 'get_last_update', width: 160, sortable: false, align: 'left', hide: false},
				{display: 'Description', name: 'get_description', width: 324, sortable: false, align: 'left', hide: false},
				{display: 'Enabled', name: 'is_enabled', width: 40, sortable: false, align: 'center', hide: false}
				],
			searchitems : [
				{display: 'Name', name : 'get_name', isdefault: true},
				{display: 'Description', name : 'get_description'}
				],
			/*sortname: "get_name",*/
			sortorder: "asc",
			usepager: true,
			title: 'Virtual Sensors',
			useRp: true,
			rpOptions: [10,50,100,500],
			hideOnSubmit: false,
			rp: 10,
			singleSelect: true,
			resizable: false,
			showTableToggleBtn: true,
			width: 750,
			height: 120
	}
	);
})
