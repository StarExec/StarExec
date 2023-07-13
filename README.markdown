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

We are currently using Ruby Sass 3.4.24

https://sass-lang.com/install

### Apache Tomcat

StarExec depends on Apache Tomcat 7.0.64. While newer versions may work, we have
frequently seen that even minor version releases of Tomcat can have breaking
changes for StarExec, so using a different version of Tomcat is not recommended.

A full release of Tomcat is included in the starexec package under the
`distribution/` directory. This is identical to a release that you can download
from Apache, with the exception that the `mysql-connector-java-5.1.22-bin.jar`
and file is included in the `lib/` directory. This `.jar` file
is required for StarExec to connect to its database, and as such we
recommend that you install Tomcat using the provided archive. If you would like
to install a clean copy of Tomcat, you will need to copy MySQL connector to the new lib directory.

PLEASE NOTE: We recently migrated our servers from centOS to Rocky8. Due to this, DRMAA is no longer 
a required dependency. Instead, we will be using qsub. For more information about this command, please 
see [https://www.jlab.org/hpc/PBS/qsub.html](https://www.jlab.org/hpc/PBS/qsub.html)

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

### Database

`DB.User` must be set to the username of a MariaDB user that has full
permissions for the database. `DB.Pass` must be set to the password for that
user. This user will require _all_ permissions in the StarExec database,
excluding server administration permissions.
If desired, a user with fewer permissions may be used by the compute nodes to
report results to the database. This user _only_ needs `EXECUTE` permission, and
is configured via `Cluster.DB.User` and `Cluster.DB.Pass`. If unspecified, these
will default to the values of `DB.User` and `DB.Pass`.

### Email

StarExec sends automated emails for several purposes, such as sending
notifications when new users are registered or sending weekly status updates.
To do this, StarExec requires an email account that it can send emails from.
`Email.User` should be set to the username of the account to send from, and
`Email.Pass`, `Email.Smtp` and `Email.Port` should be set as decribed.

StarExec is also configured to use a `Email.Contact`, which is intended to
receive emails directed at StarExec admins. This email address will appear
on the site for users who want to send bug reports or ask questions.

### Backend

You will need to make sure that you have mapped the StarExec data directory,
(`data_dir`), to a matching path on each compute node, as your compute nodes
will need access to the StarExec data directory that exists on the head node.

`Cluster.UserOne` and `Cluster.UserTwo` (by default, `sandbox` and `sandbox2`
respectively) refer to users that will execute jobs on compute nodes.
Ensure that these accounts exist on the head node and all compute nodes, and
have appropriate permissions.

Create the `star-web` group.

Create the user `tomcat` and add this user to the `star-web` group, and change
the primary group for `tomcat` to `star-web`.
This is the user that you will need to use when starting up **Tomcat** using
`startup.sh` in the **Tomcat** `bin/` folder.
You should also ensure that `tomcat` is the owner of the entire
**Tomcat** installation directory.

Finally, any users that are going to be administering StarExec should also be
added to the `star-web` group. Being a member of `star-web` will be necessary
for correctly executing the StarExec deploy scripts.

Create the `sandbox` group, and add `Cluster.UserOne` to this group.
Create another group `sandbox2` and add `Cluster.UserTwo` to this group.
Add the `tomcat` user to both of these groups.

If you are using SGE as a backend, you need to create the user `sgeadmin` and
ensure this user does have administrator privileges in SGE.

A sandbox directory will need to be created on the StarExec head node.
This directory is used to execute user-provided scripts in a sandboxed
environment, preventing them from affecting other parts of the system.
You should create a directory at the location specified by `sandbox_dir`.
Make the owner `Cluster.UserOne`, and make the group `sandbox`.
Use `chmod` on the directory to make permissions `770`.
Additionally, use `chmod g+s` to set the GID for the directory.
Finally, use the following command to ensure that new directories in the sandbox
have `g+rwx` permissions.

    setfacl -d -m g::rwx sandbox

The directory configured as `Backend.WorkingDir` needs to be created.
`tomcat` should be the owner and `star-web` should be the group.
Under this directory, create two directories named `sandbox/` and `sandbox2/`.
These should also use the `tomcat` user and the `star-web` group.

Sudo permissions need to be configured.
StarExec uses `sudo` in several locations to execute commands as other users,
most often to execute commands using the `Cluster.UserOne` and `Cluster.UserTwo`
users. The `tomcat` user will need all of the following `sudo` permissions.


#### HEAD NODE

User `tomcat` may run the following commands on this host:

    (SANDBOX_USER_ONE) NOPASSWD: ALL
    (SANDBOX_USER_TWO) NOPASSWD: ALL
    (root) NOPASSWD: /sbin/service tomcat7 restart, /sbin/service tomcat7 stop, /sbin/service tomcat7 start

The following entries are needed only if you are using an SGE backend.
Replace `/cluster/gridengine-8.1.8/bin/lx-amd64/` in each path with your install directory

    (sgeadmin) NOPASSWD: /cluster/gridengine-8.1.8/bin/lx-amd64/qconf, /cluster/gridengine-8.1.8/bin/lx-amd64/qmod


####  COMPUTE NODE (or head node if you are using a local backend)

User `tomcat` may run the following commands on this host:

For all of the following, the prefix `/export/starexec` should be replaced with
your configured value of `Backend.WorkingDir`, and `UserOne` and `UserTwo`
should be replaced with the values of `Cluster.UserOne` and `Cluster.UserTwo`
respectively.

    (UserOne) NOPASSWD: ALL
    (root) NOPASSWD: /bin/chown -R UserOne /export/starexec/sandbox, /bin/chown -R tomcat /export/starexec/sandbox, /bin/chown -R tomcat /export/starexec/sandbox/benchmark, /bin/chown tomcat
    (UserTwo) NOPASSWD: ALL
    (root) NOPASSWD: /bin/chown -R UserTwo /export/starexec/sandbox2, /bin/chown -R tomcat /export/starexec/sandbox2, /bin/chown -R tomcat /export/starexec/sandbox2/benchmark, /bin/chown tomcat

The same applies as on the head node for the following commands

    (sgeadmin) NOPASSWD: /cluster/gridengine-8.1.8/bin/lx-amd64/qconf, /cluster/gridengine-8.1.8/bin/lx-amd64/qmod
