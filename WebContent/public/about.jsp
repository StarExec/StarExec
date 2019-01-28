<%@page contentType="text/html" pageEncoding="UTF-8" %>
<%@page trimDirectiveWhitespaces="true" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>

<star:template title="StarExec" css="about">

  <p id="advisory">StarExec is a cross community logic solving service
    developed at the University of Iowa and the University of Miami
        under the direction of principal
    investigators Aaron Stump (Iowa), Geoff Sutcliffe (University
    of Miami), and Cesare Tinelli (Iowa).
  </p>

  <p>Its main goal is to facilitate the experimental evaluation of logic
    solvers, broadly understood as automated tools based on formal
    reasoning. The service is designed to provide a single piece of storage
    and computing infrastructure to logic solving communities and their
    members. It aims at reducing duplication of effort and resources as well
    as enabling individual researchers or groups with no access to
    comparable infrastructure.
  </p>

  <p>StarExec allows </p>
  <ul>
    <li>community organizers to store, manage and make available
      benchmark libraries;
    </li>
    <li>competition organizers to run logic solver competitions; and
    <li>
      community members to perform comparative evaluations of logic
      solvers on public or private benchmark problems.
    </li>
  </ul>

  <p>See <a href="machine-specs.txt">here</a> for the machine 
       specifications for
    the compute nodes at the University of Iowa, and 
        <a href="http://starexec.ccs.miami.edu/starexec/public/machine-specs.txt">here</a> 
        for the compute nodes at the University of Miami.</p>


  <!-- 
    For other institutions to reflect respective cluster specifications...
    See <a href="machine-specs.txt">here</a> for the machine speficications for the compute nodes.
   -->

  <p>A virtual machine for StarExec is <a
      href="https://www.starexec.org/vmimage/">here</a>. It is recommended
    to upgrade to the latest <a
        href="http://www.virtualbox.org">VirtualBox</a> for this
    (confirmed working for version 4.3.24).</p>

  <p>Use of StarExec is bound by the following
    <a href="terms-of-service.pdf">terms of service</a>.
  </p>

  <p>Development details can be found on our
    <a href="http://wiki.uiowa.edu/display/stardev/Home">public development
      wiki</a>.
  </p>
  <h2>StarExec Advisory Committee</h2>

  <ul>
    <li>Nikolaj Bjørner (Microsoft Research)</li>
    <li>Ewen Denney (NASA Ames)</li>
    <li>Aarti Gupta (NEC Labs)</li>
    <li>Ian Horrocks (Oxford)</li>
    <li>Giovambattista Ianni (University of Calabria)</li>
    <li>Daniel Le Berre (University of Artois)</li>
    <li>Johannes Waldmann (Leipzig University of Applied Sciences)</li>
  </ul>

  <h2>Credits</h2>

  <p>StarExec was first supported by a US$2.11 million grant from the
    National Science Foundation, the details of which can be found
    <a href="http://www.fastlane.nsf.gov/servlet/showaward?award=1058748">here (the Iowa part)</a>
    and
    <a href="http://www.fastlane.nsf.gov/servlet/showaward?award=1058925">here (the Miami part)</a>.
        A further grant of US$1.00 million provided support for futher
        development, the details of which can be found 
        <a href="http://www.fastlane.nsf.gov/servlet/showaward?award=1729603">here (the Iowa part)</a>
        and
        <a href="http://www.fastlane.nsf.gov/servlet/showaward?award=1730419">here (the Miami part)</a>.
  </p>

  <p>Many people have contributed in various capacities to the StarExec
    project.
  </p>

  <p>
    The following people were involved in the development
    of the software infrastructure at various stages of the project:
    Eric Burns (Iowa),
        Rahul Dass (Miami),
        Pedro Davila (Miami),
    Todd Elvers (Iowa),
    Albert Giegerich (Iowa),
    Pat Hawks (Iowa),
    Tyler Jensen (Iowa),
    Wyatt Kaiser (Iowa),
    Ben McCune (Iowa),
        Joseph Masterjohn (Miami),
    Muhammad Nassar (Miami),
    CJ Palmer (Iowa),
    Vivek Sardeshmukh (Iowa),
    Skylar Stark (Iowa),
    and
    Ruoyu Zhang (Iowa).
  </p>

  <p>
    Computer system support and assistance in designing and building
    the hardware infrastructure was provided by
    Hugh Brown (Iowa),
    Dan Holstad (Iowa),
    Jamie Tisdale (Iowa),
    JJ Ulrich (Iowa),
        and
        Joel Zysman (Miami).
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
