<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags"%>

<star:template title="StarExec" css="about">
	<style type="text/css">
		#content a:link {
			text-decoration: underline;
			color: #ae0000;
		}
	</style>
	<p id="advisory">StarExec is a cross community logic solving service
	developed at the University of Iowa under the direction of principal
	investigators Aaron Stump (Iowa), Geoff Sutcliffe (University
	of Miami), and Cesare Tinelli (Iowa).
  </p>
	
	<p>Its main goal is to facilitate the experimental evaluation of logic
	solvers, broadly understood as automated tools based on formal reasoning.
	The service is designed to provide a single piece of storage and computing
	infrastructure to logic solving communities and their members. 
	It aims at reducing duplication of effort and resources as well as 
	enabling individual researchers or groups with no access to comparable
	 infrastructure. 
	</p>
	
	<p>StarExec allows 
	<ul>
	<li>community organizers to store, manage and make available
	benchmark libraries;
	</li>
	<li>competition organizers to run logic solver competitions; and 
	</li>
	</li>community members to perform comparative evaluations of logic solvers 
	on public or private benchmark problems.
	</p>

	<p>The first public events ran on StarExec in Summer 2013 (CoCo 2013 and SMT-EVAL 2013). 
  Several more will run in Summer 2014.
	</p>

	<p>See <a href="machine-specs.txt">here</a> for machine specifications for the compute nodes.</p>

	<p>Use of StarExec is bound by the following 
	<a href="STAREXEC TERMS OF SERVICE.doc">terms of service</a>.
	</p>

	<p>Development details can be found on our
		<a href="http://wiki.uiowa.edu/display/stardev/Home">public development wiki</a>.
	</p>
  <h2>StarExec Advisory Committee</h2>
  
  <p>
	<ul>
		<li>Nikolaj Bjørner (Microsoft Research)</li>
		<li>Ewen Denney (NASA Ames)</li>
		<li>Aarti Gupta (NEC Labs)</li>
		<li>Ian Horrocks (Oxford)</li>
		<li>Giovambattista Ianni (University of Calabria)</li>
		<li>Daniel Le Berre (University of Artois)</li>
		<li>Johannes Waldmann (Leipzig University of Applied Sciences)</li>
	</ul>
	</p>

  <h2>Credits</h2>

	<p>StarExec is supported by a $1.85 million USD grant from the 
	National Science Foundation, the details of which can be found 
	<a href="http://www.fastlane.nsf.gov/servlet/showaward?award=1058748">here</a>
	and
	<a href="http://www.fastlane.nsf.gov/servlet/showaward?award=1058925">here</a>.
	</p>

	<p>Several people have contributed in various capacities to the StarExec project so far.
	</p>

<p>
The following people were involved in the development 
of the software infrastructure at various stages of the project:
Eric Burns,
Todd Elvers, 
Tyler Jensen, 
Wyatt Kaiser,
Ben McCune,
Muhammad Nassar,
CJ Palmer,
Vivek Sardeshmukh, 
Skylar Stark,
and
Ruoyu Zhang.
</p>

<p>
Computer system support and assistance in designing and building 
the hardware infrastructure was provided by 
Hugh Brown,
Dan Holstad,
Jamie Tisdale,
and
JJ Ulrich.
</p>

<p>
In addition to the members of the Advisory Board,
the following people have provided useful feedback and input:
Clark Barrett,
Christoph Benzm&uuml;ller,
Armin Biere,
David Cok,
Morgan Deters,
J&uuml;rgen Giesl,
Alberto Griggio,
Thomas Krennwallner,
Jens Otten,
Andrei Paskevich,
Olivier Roussel,
Martina Seidl,
Stephan Schulz,
Michael Tautschnig,
Christoph Wintersteiger,
and
Harald Zankl.
</p>
</star:template>
