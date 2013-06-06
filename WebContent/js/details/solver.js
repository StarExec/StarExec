var commentTable;
var solverId;
$(document).ready(function(){
	initUI();
	attachButtonActions();
	
	$("#dialog-confirm-copy").css("display", "none");


	$('img').click(function(event){
		popUp($(this).attr('enlarge'));
	});
	
	$('.confirmation').on('click', function () {
        return confirm('Are you sure?');
    });
});

//	solverId = getParameterByName('id');
//	initComments(solverId);



/*
 * Get comments and display them
 * Set-up add comment functionality 
 */
function initComments(solverId){
	getComments(solverId);
	
	// Handles adding a new comment - Vivek
	$("#addComment").click(function(){
		var comment = HtmlEncode($("#comment_text").val());
		
		if(comment.trim().length == 0) {
			showMessage('error', 'comments can not be empty', 6000);
			return;
		}	
		var data = {comment: comment};
		$.post(
				starexecRoot+"services/comments/add/solver/" + solverId,
				data,
				function(returnCode) {
			    	if(returnCode == 0) {
			    		$("#comment_text").val("");
			    		$.getJSON(starexecRoot+'services/comments/solver/' + solverId, displayComments).error(function(){
			    			showMessage('error',"Internal error posting comment",5000);
			    		});
			    	} else {
			    		showMessage('error', "adding your comment was unsuccessful; please try again", 5000);
			    	}
				},
				"json"
		);
		
	});
}


function initUI(){
	$('#downLink3').button({
		icons: {
			secondary: "ui-icon-arrowthick-1-s"
    }});
	$('#fieldSites').expandable(true);
	$('#actions').expandable(true);
	$('#commentField').expandable(true);
	// Setup '+ add comment' animation
	$('#toggleComment').click(function() {
		$('#new_comment').slideToggle('fast');
		togglePlusMinus(this);
	});	
	$('#new_comment').hide();
	
	$('#addComment').button({
		icons: {
			secondary: "ui-icon-plus"
    }});
	
	// Setup datatable of configurations
	$('#tblSolverConfig').dataTable( {
        "sDom": 'rt<"bottom"flpi><"clear">',        
        "bPaginate": true,        
        "bSort": true        
    });

	commentTable = $('#comments').dataTable( {
        "sDom": 'rt<"bottom"flpi><"clear">',
        "aaSorting": [[ 1, "asc" ]]
    }); 
	
	$( "#dialog-confirm-delete" ).hide();
	$( "#dialog-warning").hide();
	// Setup button icons
	
	$('#uploadConfig, #uploadConfigMargin').button({
		icons: {
			primary: "ui-icon-arrowthick-1-n"
		}
    });
	
	$('#uploadPicture').button({
		icons: {
			primary: "ui-icon-gear"
		}
    });
	
	//Warn if solver is uploaded without configuration
	var flag = window.location.search.split('flag=')[1]; //Check to see if flag was set
	if (flag == 'true') {
		$('#dialog-warning-txt').text('WARNING: No Configurations.');
		
		$('#dialog-warning').dialog({
			modal: true,
			width: 380,
			height: 165,
			buttons: {
				'OK': function() {
					$('#dialog-warning').dialog('close');
				},
			}
		});
	}
}

function attachButtonActions() {
	$("#downLink3").click(function(){
		$('#dialog-confirm-copy-txt').text('How would you like to download the solver?');		
		$('#dialog-confirm-copy').dialog({
			modal: true,
			width: 380,
			height: 165,
			buttons: {
				'default': function() {
					log("default selected");
					$(this).dialog("close");
					createDialog("Processing your download request, please wait. This will take some time for large solvers.");
					token=Math.floor(Math.random()*100000000);
					window.location.href = starexecRoot+"secure/download?token=" + token + "&type=solver&id="+$("#solverId").attr("value");
					//$('#downLink3').attr('href', starexecRoot+"secure/download?token=" + token + "&type=solver&id=" + $("#solverId").attr("value"));
					destroyOnReturn(token);
				},
				'Re-upload': function() {
					log("upload selected");
					$(this).dialog("close");
					createDialog("Processing your download request, please wait. This will take some time for large solvers.");
					token=Math.floor(Math.random()*100000000);
					window.location.href = starexecRoot+"secure/download?token=" + token + "&type=reupload&id="+$("#solverId").attr("value");
					//$('#downLink3').attr('href', starexecRoot+"secure/download?token=" + token + "&type=reupload&id=" + $("#solverId").attr("value"));
					destroyOnReturn(token);
				},
				"cancel": function() {
					log('user canceled copy action');
					$(this).dialog("close");
				}
			}
		});
	});
}

function popUp(uri) {
	imageDialog = $("#popDialog");
	imageTag = $("#popImage");
	
	imageTag.attr('src', uri);

	imageTag.load(function(){
		$('#popDialog').dialog({
			dialogClass: "popup",
			modal: true,
			resizable: false,
			draggable: false,
			height: 'auto',
			width: 'auto'
		});
	});  
}

	

/**
 * Displays comments related to this space
 * @param data - json response 
 * @author Vivek Sardeshmukh
 */
function displayComments(data) {
	var delCount=0; //keep tracks of how many comments are deleted to update the span number
	$('#commentField legend').children('span:first-child').text(data.length);
	// Ensures the comments table is empty
	commentTable.fnClearTable();
	// Injects the clickable delete button that's always present
	$.each(data, function(i, comment) {
		var hiddenUserId;
		hiddenUserId = '<input type="hidden" value="'+comment.userId+'">';
		var fullName = comment.firstName + ' ' + comment.lastName;
		var userLink = '<a href=starexecRoot+"secure/details/user.jsp?id=' + comment.userId + '" target="blank">' + fullName + '<img class="extLink" src=starexecRoot+"images/external.png"/></a>' + hiddenUserId;
		var hiddenCommentId = '<input type="hidden" value="' + comment.id + '" >';
		var delbutton = '<a class="commentDelBtn" cid="' + comment.id + '" uid="' + comment.userId +'">delete</a>';
		var cmt = comment.description;
		var brcmt = cmt.replace(/\n/g, "<br />"); //replace all newlines to <br>
		commentTable.fnAddData([userLink,  comment.uploadDate, brcmt, delbutton]);
	});
	
	// Handles deletion of comments
	$('.commentDelBtn').click(function(){
		var cid = $(this).attr('cid');
		var uid = $(this).attr('uid');
		//get the selected row
		var row = $(this).closest("tr").get(0); 

		$('#dialog-confirm-delete-txt').text('are you sure you want to delete this comment?');
		$('#dialog-confirm-delete').dialog({
			modal: true,
			buttons: {
				'yes': function() {
					$('#dialog-confirm-delete').dialog('close');
					$.post(
							starexecRoot+"services/comments/delete/solver/" + solverId + "/" + uid + "/" + cid,
						function(returnData){
							if (returnData == 0) {
								commentTable.fnDeleteRow(commentTable.fnGetPosition(row));
								delCount=delCount+1; 
								$('#commentField legend').children('span:first-child').text(data.length-delCount);
							} else if (returnData == 2) {
								showMessage('error',"deleting comments is restricted to the owner of the solver and the comment's owner", 5000);
							} else {
								showMessage('error', "unable to delete comment at this moment, please try again", 5000);
							}
						},
					    "json"
					).error(function(){
						showMessage('error',"Internal error deleting comment",5000);
					});
				},
				"cancel": function() {
					$(this).dialog("close");
				}
			}		
		});
		
	});
}

//get comment information for the given solver
function getComments(id) {
	$.getJSON(starexecRoot+'services/comments/solver/' + id, displayComments).error(function(){
		showMessage('error',"Internal error getting comments",5000);
	});
}
/**
 * Toggles the plus-minus text of the "+ add new" comment button
 */
function togglePlusMinus(addCommentButton){
	if($(addCommentButton).children('span:first-child').text() == "+"){
		$(addCommentButton).children('span:first-child').text("-");
	} else {
		$(addCommentButton).children('span:first-child').text("+");
	}
}

