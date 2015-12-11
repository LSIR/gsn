define([
	"../core",
	"../selector"
], function( jQuery ) {

jQuery.expr.filterFunctionList.hidden = function(elem ) {
	// Support: Opera <= 12.12
	// Opera reports offsetWidths and offsetHeights less than zero on some elements
	return elem.offsetWidth <= 0 && elem.offsetHeight <= 0;
};
jQuery.expr.filterFunctionList.visible = function(elem ) {
	return !jQuery.expr.filterFunctionList.hidden( elem );
};

});
