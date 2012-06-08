var bid;
var commentTable;

$(document).ready(function(){
	bid = getParameterByName('id');	
	$('#fieldType').expandable(true);
	$('#fieldAttributes').expandable(true);
	$('#fieldDepends').expandable(true);
	$('#fieldContents').expandable(true, function() {
		if($(this).data('requested') == undefined) {
			$(this).data('requested', true);
			
			$('#fieldContents legend img').show();
			$.get('/starexec/services/benchmarks/' + bid + '/contents?limit=100', function(data) {
				$('#benchContent').text(data);
				$('#fieldContents legend img').hide();
			}).error(function(){				
				$('#benchContent').text('unavailable');
				$('#fieldContents legend img').hide();
			});
		}
	});
	
	initCommentsUI();
	initComments(bid);
	
});

function initCommentsUI(){
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
	$( "#dialog-confirm-delete" ).hide();
	commentTable = $('#comments').dataTable( {
        "sDom": 'rt<"bottom"flpi><"clear">',
        "aaSorting": [[ 1, "asc" ]]
    }); 
}

/*
 * displays comments for benchmark with id bid
 * also handles addition of  a new comment
 */
function initComments(bid){
	getComments(bid);
	$("#addComment").click(function(){
		var comment = HtmlEncode($("#comment_text").val());
		
		if(comment.trim().length == 0) {
			showMessage('error', 'comments can not be empty', 6000);
			return;
		}	
		var data = {comment: comment};
		$.post(
				"/starexec/services/comments/add/benchmark/" + bid,
				data,
				function(returnCode) {
			    	if(returnCode == 0) {
			    		$("#comment_text").val("");
			    		$.getJSON('/starexec/services/comments/benchmark/' + bid, displayComments).error(function(){
			    			alert('Session expired');
			    			window.location.reload(true);
			    		});
			    	} else {
			    		showMessage('error', "adding your comment was unsuccessful; please try again", 5000);
			    		//console.log('error: comment not added. please try again');
			    	}
				},
				"json"
		);
		
	});
}
/**
 * Displays comments related to this benchmark
 * @param data - json response 
 * @author Vivek Sardeshmukh
 */
function displayComments(data) {
	
	// Ensures the comments table is empty
	var delCount=0;
	$('#commentField legend').children('span:first-child').text(data.length);
	commentTable.fnClearTable();
	// Injects the clickable delete button that's always present
	$.each(data, function(i, comment) {
		var hiddenUserId;
		hiddenUserId = '<input type="hidden" value="'+comment.userId+'">';
		var fullName = comment.firstName + ' ' + comment.lastName;
		var userLink = '<a href="/starexec/secure/details/user.jsp?id=' + comment.userId + '" target="blank">' + fullName + '<img class="extLink" src="/starexec/images/external.png"/></a>' + hiddenUserId;
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
						"/starexec/services/comments/delete/benchmark/" + bid + "/" + uid + "/" + cid,
						function(returnData){
							if (returnData == 0) {
								commentTable.fnDeleteRow(commentTable.fnGetPosition(row));
								delCount=delCount+1; 
								$('#commentField legend').children('span:first-child').text(data.length-delCount);
							} else if (returnData == 2) {
								showMessage('error',"deleting comments is restricted to the owner of the benchmark and the comment's owner", 5000);
							} else {
								showMessage('error', "unable to delete comment at this moment, please try again", 5000);
							}
						},
						"json"
					).error(function(){
					alert('Session expired');
					window.location.reload(true);
				});
			  },
			  "cancel": function() {
				  $(this).dialog("close");
			  }
		   }		
		});
		
	});
}
function getComments(id) {
	//get comment information for the given benchmark
	$.getJSON('/starexec/services/comments/benchmark/' + id, displayComments).error(function(){
		alert('Session expired');
		window.location.reload(true);
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


