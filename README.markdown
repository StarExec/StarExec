# StarExec

StarExec is a cross community logic solving service developed at the University
of Iowa under the direction of principal investigators Aaron Stump (Iowa), Geoff
Sutcliffe (University of Miami), and Cesare Tinelli (Iowa).

Its main goal is to facilitate the experimental evaluation of logic solvers,
broadly understood as automated tools based on formal reasoning. The service is
designed to provide a single piece of storage and computing infrastructure to
logic solving communities and their members. It aims at reducing duplication of
effort and resources as well as enabling individual researchers or groups with
no access to comparable infrastructure.

StarExec allows

 * community organizers to store, manage and make available benchmark libraries;
 * competition organizers to run logic solver competitions; and
 * community members to perform comparative evaluations of logic solvers on
   public or private benchmark problems.

Development details can be found on our
[public development wiki](http://wiki.uiowa.edu/display/stardev/Home).

## Dependencies

### Java

StarExec requires Ant to build, and has been tested with version 1.9.2.
An installation guide for Ant is below, or you may use any applicable
package manager.

http://ant.apache.org/manual/install.html

### SASS

StarExec requires [Sass](https://sass-lang.com) at build time to compile `.scss`
stylesheets to `.css`. Sass depends on [Ruby](https://www.ruby-lang.org/en/).

https://sass-lang.com/install

### Apache Tomcat

StarExec depends on Apache Tomcat 7.0.64. While newer versions may work, we have
frequently seen that even minor version releases of Tomcat can have breaking
changes for StarExec, so using a different version of Tomcat is not recommended.

A full release of Tomcat is included in the starexec package under the
`distribution/` directory. This is identical to a release that you can download
from Apache, with the exception that the `mysql-connector-java-5.1.22-bin.jar`
and `drmaa.jar` files are included in the `lib/` directory. These `.jar` files
are required for StarExec to connect to its database and backend, and as such we
recommend that you install Tomcat using the provided archive. If you would like
to install a clean copy of Tomcat, you will need to copy the MySQL connector and
DRMAA files to the new lib directory.

If you install Tomcat using the provided archive, you may need to update
permissions on the install directory to make Tomcat's scripts executable. This
can be done, for example, by using `chmod 700 -R tomcat_directory`

### MySQL and MariaDB

StarExec depends on MariaDB 5.5.56.

https://downloads.mariadb.org/


### Backend

StarExec's backend refers to the utility that is responsible for accepting new
jobs from the web app and distributing them over the available compute nodes.
StarExec supports 3 different backend implementations:
[SGE](https://arc.liv.ac.uk/trac/SGE),
[OAR](https://oar.imag.fr/),
or a simple local backend implemented in StarExec itself.

To install SGE or OAR, you will need to refer to their documentation. In the
case of OAR, the document
[`distribution/OAR installation notes.txt`](distribution/OAR%20installation%20notes.txt)
describes extra installation steps you should take to configure OAR for use with
StarExec.

The local backend is a primitive solution if you only want to run jobs on the
same machine that StarExec is running off of. The local backend does not support
multiple queues or nodes, and only a single job will run at a time.

## Configuration

StarExec is configured by Ant at build time.

StarExec's default configuration is specified by
[`build/default.properties`](build/default.properties), but several properties
will need to be overridden for a particular StarExec instance. These properties
may be overridden in three ways:

1) If `build/overrides.properties` exists, any properties specified in this file
   will override the defaults
2) Ant can be passed a `.properties` file if invoked with the `-propertyfile`
   option. Any properties specified in this file will override the defaults
   _and_ any properties that exist in `build/overrides.properties`
3) Individual properties can be set by invoking Ant with the
   `-D<property>=<value>` option. Any properties set this way will take
   precidence.

An empty configuration file is provided as
[`example.properties`](example.properties).
This file also explains the properties that must be set for a particular
StarExec instance.
