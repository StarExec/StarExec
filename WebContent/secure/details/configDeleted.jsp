<%@page contentType="text/html" pageEncoding="UTF-8"
        import="org.apache.commons.io.FileUtils, org.starexec.data.database.Permissions, org.starexec.data.database.Solvers,org.starexec.data.security.GeneralSecurity, org.starexec.data.to.Configuration, org.starexec.data.to.Solver, org.starexec.util.SessionUtil, org.starexec.util.Util, java.io.File, org.starexec.logger.StarLogger"
        session="true" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<html lang="en">
<head>
    <title>Deleted Configuration - StarExec</title>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1" />
    <link rel="stylesheet" href="/starexec_abrown68/css/jqueryui/jquery-ui.css" />
    <link rel="stylesheet" href="/starexec_abrown68/css/global.css" />
    <link rel="stylesheet" href="/starexec_abrown68/css/error.css" />
    <script>
		var starexecRoot="/starexec_abrown68/";
		var defaultPageSize=10;
		var isLocalJobPage=false;
		var debugMode=false;
	</script>
    <script type="text/javascript" src="/starexec_abrown68/js/lib/jquery.min.js"></script>
    <script type="text/javascript" src="/starexec_abrown68/js/lib/jquery-ui.min.js"></script>
    <script type="text/javascript" src="/starexec_abrown68/js/lib/jquery.cookie.js"></script>
    <script type="text/javascript" src="/starexec_abrown68/js/master.js"></script>
    <link type="image/ico" rel="icon" href="/starexec_abrown68/images/favicon.ico">
</head>
<body>
<div id="wrapper">



    <header id="pageHeader">
        <div id="starexecLogoWrapper">
            <a href="/starexec_abrown68/secure/index.jsp"><img src="/starexec_abrown68/images/starlogo.png" alt="StarExec Logo"></a>
        </div>

        <div id="starexecNavWrapper">
            <nav>
                <ul>


                    <li class="round">
                        <a href="#">Account</a>
                        <ul class="subnav round">
                            <li class="round"><a href="/starexec_abrown68/secure/details/user.jsp?id=3">Profile</a></li>
                            <li class="round"><a href="#" onclick="logout();">Logout</a></li>
                        </ul>
                    </li>

                    <li class="round">
                        <a href="#">Spaces</a>
                        <ul class="subnav round">
                            <li class="round"><a href="/starexec_abrown68/secure/explore/spaces.jsp">Explore</a></li>
                            <li class="round"><a href="/starexec_abrown68/secure/explore/communities.jsp">Communities</a></li>
                            <li class="round"><a href="/starexec_abrown68/secure/explore/statistics.jsp">Statistics</a></li>
                            <li class="round"><a href="/starexec_abrown68/secure/explore/reports.jsp">Reports</a></li>
                        </ul>
                    </li>
                    <li class="round">
                        <a href="#">Cluster</a>
                        <ul class="subnav round">
                            <li class="round"><a href="/starexec_abrown68/secure/explore/cluster.jsp">Status</a></li>
                        </ul>
                    </li>
                    <li class="round" id="helpTab"><a id="helpTag" href="/starexec_abrown68/secure/help.jsp">Help</a></li>
                </ul>
            </nav>
        </div>

    </header>
    <div id="content" class="round">
        <div id="mainHeaderWrapper">
            <h1 style="width:100%; word-wrap:break-word;" id="mainTemplateHeader">This configuration has been deleted.</h1>
        </div>
        <img alt="loading" src="/starexec_abrown68/images/loader.gif" id="loader">

        <p>Although this configuration file was used in the job you were just viewing, since then it has been deleted and no longer exists.</p>
    </div>





    <footer id="pageFooter">
        <ul>

            <li><a target="_blank"
                   href="/starexec_abrown68/secure/details/user.jsp?id=3">Test User</a></li>
            <li>|</li>
            <li><a onclick="logout();">Logout</a></li>


            <li>|</li>
            <li><a id="about" href="/starexec_abrown68/public/about.jsp">About</a></li>
            <li>|</li>
            <li><a id="help" href="/starexec_abrown68/public/help.jsp">Support</a></li>
            <li>|</li>
            <li><a id="starexeccommand" href="/starexec_abrown68/public/starexeccommand.jsp">StarExec Command</a></li>
        </ul>
        <a class="copyright" href="http://www.cs.uiowa.edu" target="_blank">&copy;
            2012-18 The University of Iowa</a>
    </footer>


</div>
</body>
</html>
