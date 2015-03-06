var pairTable;
var pairType;
var jobId;
var spaceId;
var configId;
var useWallclock=true;
$(document).ready(function(){
	jobId = getParameterByName('id');
	spaceId=getParameterByName('sid');
	configId=getParameterByName("configid");
	pairType=getParameterByName("type");
	stageid=getParameterByName("stagenum");
	if (stringExists(stageid)) {
		setSelectedStage(stageid);
	} else {
		setSelectedStage("0");
	}
	$('.id_100 ').prop('selected',true);
	$('#pairFilter option[value='+pairType+']').prop('selected',true);

	initUI();
	initDataTables();
	setTimeButtonText();	
	
});


/**
 * Initializes the user-interface
 */
function initUI(){
	$(".changeTime").button({
		icons: {
			primary: "ui-icon-refresh"
		}
	
	});
	
	$(".changeTime").click(function() {
		useWallclock=!useWallclock;
		setTimeButtonText();
		pairTable.fnDraw(false);
	});
	
	$(".stageSelector").change(function() {
		//set the value of all .stageSelectors to this one to sync them.
		//this does not trigger the change event, which is good because it would loop forever
		$(".stageSelector").val($(this).val());
		pairTable.fnDraw(false);
	});
	
	$('#pairTbl tbody').on( "click", "a", function(event) {
		event.stopPropogation();
	});

	
	//Set up row click to send to pair details page
	$("#pairTbl tbody").on( "click", "tr", function(){
		var pairId = $(this).find('input').val();
		window.location.assign(starexecRoot+"secure/details/pair.jsp?id=" + pairId);
	});
	$("#pairFilter").change(function(){
		pairTable.fnDraw(false);
	});
	attachSortButtonFunctions();

}

/**
 * Initializes the DataTable objects
 */
function initDataTables(){
	extendDataTableFunctions();
	// Job pairs table
	pairTable=$('#pairTbl').dataTable( {
        "sDom"			: 'rt<"bottom"flpi><"clear">',
        "iDisplayStart"	: 0,
        "iDisplayLength": defaultPageSize,
        "bServerSide"	: true,
		"pagingType"    : "full_numbers",

        "sAjaxSource"	: starexecRoot+"services/jobs/",
        "sServerMethod" : "POST",
        "fnServerData"	: fnPaginationHandler 
    });
	setSortTable(pairTable);
	$("#pairTbl thead").click(function(){
		resetSortButtons();
	});
	// Change the filter so that it only queries the server when the user stops typing
	$('#pairTbl').dataTable().fnFilterOnDoneTyping();
	
}

/**
 * Adds fnProcessingIndicator and fnFilterOnDoneTyping to dataTables api
 */
function extendDataTableFunctions(){
	// Changes the filter so that it only queries when the user is done typing
	jQuery.fn.dataTableExt.oApi.fnFilterOnDoneTyping = function (oSettings) {
	    var _that = this;
	    this.each(function (i) {
	        $.fn.dataTableExt.iApiIndex = i;
	        var anControl = $('input', _that.fnSettings().aanFeatures.f);
	        anControl.unbind('keyup').bind('keyup', $.debounce( 400, function (e) {
                $.fn.dataTableExt.iApiIndex = i;
                _that.fnFilter(anControl.val());
	        }));
	        return this;
	    });
	    return this;
	};
}

/**
 * Handles querying for pages in a given DataTable object
 * 
 * @param sSource the "sAjaxSource" of the calling table
 * @param aoData the parameters of the DataTable object to send to the server
 * @param fnCallback the function that actually maps the returned page to the DataTable object
 */
function fnPaginationHandler(sSource, aoData, fnCallback) {
	curType = $('#pairFilter').find(":selected").attr("value");
	if (sortOverride!=null) {
		aoData.push( { "name": "sort_by", "value":getSelectedSort() } );
		aoData.push( { "name": "sort_dir", "value":isASC() } );

	}
	$.post(  
			sSource + jobId + "/pairs/pagination/"+spaceId+"/"+configId+"/"+curType+"/"+useWallclock+"/"+getSelectedStage(),
			aoData,
			function(nextDataTablePage){
				s=parseReturnCode(nextDataTablePage);
				if (s) {
					fnCallback(nextDataTablePage);
				}
			},  
			"json"
	).error(function(){
		showMessage('error',"Internal error populating data table",5000);
	});
}