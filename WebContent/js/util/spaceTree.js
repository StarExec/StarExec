/**
 * Creates the space explorer tree for the left-hand side of the page, also
 * creates tooltips for the space explorer, .expd class, and userTable (if applicable)
 * @author Tyler Jensen & Todd Elvers & Skylar Stark
 */
function makeSpaceTree(selector,usingCookies){
	// Set the path to the css theme for the jstree plugin
	$.jstree._themes = starexecRoot+"css/jstree/";
	plugins = [ "types", "themes", "json_data", "ui"];
	if (typeof usingCookies == 'undefined' || usingCookies) {
		plugins[4]="cookies";
	}
	var id;
	// Initialize the jstree plugin for the explorer list
	jsTree=$(selector).jstree({  
		"json_data" : { 
			"ajax" : { 
				"url" : starexecRoot+"services/space/subspaces",	// Where we will be getting json data from 
				"data" : function (n) {
					
					return { id : n.attr ? n.attr("id") : -1 }; 	// What the default space id should be
				} 
			} 
		}, 
		"themes" : { 
			"theme" : "default", 					
			"dots" : true, 
			"icons" : true
		},		
		"types" : {				
			"max_depth" : -2,
			"max_children" : -2,					
			"valid_children" : [ "space" ],
			"types" : {						
				"space" : {
					"valid_children" : [ "space" ],
					"icon" : {
						"image" : starexecRoot+"images/jstree/db.png"
					}
				}
			}
		},
		"ui" : {			
			"select_limit" : 1,			
			"selected_parent_close" : "select_parent",			
			"initially_select" : [ "1" ]			
		},
		"plugins" : plugins,
		"core" : { animation : 200 }
	}).on( "click","a", function (event, data) { event.preventDefault();  });// This just disable's links in the node title
	
	return jsTree;
}

function openSpace(curSp,childId) {
	$("#exploreList").jstree("open_node", "#" + curSp, function() {
		$.jstree._focused().select_node("#" + childId, true);	
	});	
}
function getSpaceChain(selector) {
	chain=new Array();
	spaceString=$(selector).attr("value");
	if (spaceString.length==0) {
		return spaceString;
	}
	spaces=spaceString.split(",");
	index=0;
	for (i=0;i<spaces.length;i++) {
		if (spaces[i].trim().length>0) {
			chain[index]=spaces[i];
		}
		index=index+1;
	}

	return chain;
}
/**
 * Takes a list of spaces and moves the space tree down to the final space
 */
function handleSpaceChain(selector) {
	spaceChain=getSpaceChain(selector);
	if (spaceChain.length<2) {
		return;
	}
	p=spaceChain[0];

	spaceChainIndex=1;
	spaceChainInterval=setInterval(function() {
		if (spaceChainIndex>=spaceChain.length) {
			clearInterval(spaceChainInterval);
		}
		if (openDone) {
			openDone=false;
			c=spaceChain[spaceChainIndex];
			openSpace(p,c);
			spaceChainIndex=spaceChainIndex+1;
			p=c;	
		}
		},100);
}



