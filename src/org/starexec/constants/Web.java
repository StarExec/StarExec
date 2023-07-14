package org.starexec.constants;

/**
 * Class that stores our web-related constants.
 */
public final class Web {

	/**
	 * Private constructor to make the class un-instantiable.
	 */
	private Web() throws UnsupportedOperationException {
		throw new UnsupportedOperationException("Cannot instantiate class because it is static.");
	}

	public static final String ANONYMOUS_LINK_PARAMETER = "anonId";

	public static final String APPROVE_COMMUNITY_REQUEST = "approve";
	public static final String DECLINE_COMMUNITY_REQUEST = "decline";
	// This is used as a AJAX parameter to identify requests that were sent from the community page
	// versus requests that were sent from email to accept or decline join community requests
	public static final String SENT_FROM_COMMUNITY_PAGE = "sentFromCommunityPage";
	public static final String LOCAL_JOB_PAGE_PARAMETER = "localJobPage";

	public static final String[] JOB_DETAILS_JS_FILES = {"util/sortButtons", "util/jobDetailsUtilityFunctions", "common/delaySpinner", "lib/jquery.jstree", "lib/jquery.dataTables.min", "details/shared", "details/job", "lib/jquery.ba-throttle-debounce.min", "lib/jquery.qtip.min", "lib/jquery.heatcolor.0.0.1.min", "util/datatablesUtility"};
	public static final String[] GLOBAL_JS_FILES = {"lib/jquery.min", "lib/jquery-ui.min", "lib/jquery.cookie", "master"};
	public static final String[] JS_FILES_FOR_LOCAL_JOB = {"lib/jquery.min", "lib/jquery-ui.min", "lib/jquery.cookie", "master", "details/pair", "lib/prettify", "lib/lang-log"};
	public static final String[] JOB_DETAILS_CSS_FILES = {"jobDetails"};
	public static final String[] GLOBAL_CSS_FILES = {"jqueryui/jquery-ui", "global"};
	//this needs to be seperate, or it will break everything!
	public static final String[] CSS_FILES_FOR_LOCAL_JOB = {"jqueryui/jquery-ui", "global", "details/job", "details/shared", "explore/common", "common/table", "common/delaySpinner", "details/pair", "prettify/prettify"};
	public static final String[] GLOBAL_PNG_FILES = {"loadingGraph", "starlogo", "external"};
	public static final String[] GLOBAL_GIF_FILES = {"ajaxloader", "loader"};
	public static final String[] GLOBAL_ICO_FILES = {"favicon"};

	public static final String COPY_TO_STARDEV_BUTTON_TEXT = "copy to stardev";

}
