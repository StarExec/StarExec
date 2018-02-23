# Installation Notes

This document describes how to describe Starexec and its
dependencies on a Linux system.

## Java

Starexec requires Ant to build, and has been tested with version 1.9.2. An
installation guide for Ant is below, or you may use any applicable package manager.

http://ant.apache.org/manual/install.html

## Apache Tomcat

Starexec depends on Apache Tomcat 7.0.64. While newer versions may work, we have
frequently seen that even minor version releases of Tomcat can have breaking
changes for Starexec, so using a different version of Tomcat is not recommended.

A full release of Tomcat is included in the starexec package under the `distribution/`
directory. This is identical to a release that you can download from Apache,
with the exception that the `mysql-connector-java-5.1.22-bin.jar` and
`drmaa.jar` files are included in the `lib/` directory. These `.jar` files are required for Starexec
to connect to its database and backend, and as such we recommend that you install
Tomcat using the provided archive. If you would like to install a clean copy of
Tomcat, you will need to copy the MySQL connector and DRMAA files to the new lib directory.

If you install Tomcat using the provided archive, you may need to update permissions
on the install directory to make Tomcat's scripts executable. This can be done, for
example, by using `chmod 700 -R tomcat_directory`


## MySQL and MariaDB

StarExec depends on MariaDB 5.5.56. MariaDB can
be obtained at the following site.

https://downloads.mariadb.org/

In starexec-config.xml, you must specify MYSQL_USERNAME and
COMPUTE_NODE_MYSQL_USERNAME. These are MySQL users that must have access to the
MySQL database. If desired, they can be the same user-- the only difference is that
COMPUTE_NODE_MYSQL_USERNAME requires fewer permissions. The user set as
MYSQL_USERNAME will require ALL permissions in the Starexec database, excluding
server administration permissions. The user COMPUTE_NODE_MYSQL_USERNAME will
require SELECT, INSERT, UPDATE, and EXECUTE permissions.

A description of MySQL permissions can be found at the link below.

http://dev.mysql.com/doc/refman/5.7/en/privileges-provided.html

## Backend

Starexec's backend refers to the utility that is responsible for accepting
new jobs from the web app and distributing them over the available compute nodes.
Starexec supports 3 different backend implementations, and which one you use
can be configured using the BACKEND_TYPE field in starexec-config.xml. You have
the option to use SGE (https://arc.liv.ac.uk/trac/SGE), OAR (https://oar.imag.fr/),
or a simple local backend implemented in Starexec itself.

To install SGE or OAR, you will need to refer to their documentation. In the case
of OAR, the document `OAR installation notes.txt` describes extra installation
steps you should take to configure OAR for use with Starexec.

You will need to make sure that you have mapped the Starexec data directory,
configured in starexec-config.xml, to a matching path on each compute node,
as your compute nodes will need access to the Starexec data directory
that exists on the head node.

The local backend is a primitive solution if you only want to run jobs on the same
machine that Starexec is running off of. The local backend does not support multiple
queues or nodes, and only a single job will run at a time.

## Email

Starexec sends automated emails for several purposes, such as sending notifications
when new users are registered or sending weekly status updates. To do this, Starexec
requires an email address that it can send emails from. In starexec-config.xml,
the EMAIL_USER field should be set to the email address to send from, and EMAIL_PWD,
EMAIL_SMTP, and EMAIL_SMTP_PORT should be set as decribed.

Any email server that supports SMTP can be used, but the default settings included
in the config file show how to use a Gmail account. The EMAIL_SMTP field is set
correctly for Gmail, and the SMTP_PORT field is set to 587, as Starexec uses
TLS. You should be able to use any desired Gmail account by plugging in your own
account and password. Note that Gmail may initially block the Starexec app from
sending mail until you log onto your account and confirm the activity. Moreover,
you may need to go into your Gmail account settings and allow access from less
secure apps. Creating a fresh Gmail account for sole use by Starexec may be
beneficial if you do not want to change your settings.

Starexec has been tested using Gmail, but other email servers should also work
provided you look up the correct SMTP server and port. Various settings may
need to be changed in other email providers as well.

Starexec is also configured to use a CONTACT_EMAIL, which is intended to
receive emails directed at Starexec admins. This email address will appear
on the site for users who want to send bug reports or ask questions.

## Starexec Configuration

After all of the above dependencies have been installed, you can take the following
steps to configure and deploy Starexec.

1) Under src/org/starexec/config you will see the file starexec-config.xml. This file
contains the bulk of Starexec configuration, including specifying Starexec data directories,
email settings, database connection settings, and several other items. You will need
to go through this file and configure Starexec as you want, according to the documentation
provided in that file.

2) In the top-level file local.properties, you will need to provide several configurable values.
These values are mostly identical to values in starexec-config.xml.

3) Run 'ant' at the top level. This will cause Ant to build starexec according to the rules
specified in build.xml.

4) In the deployed-sql directory, which should have been created by the Ant build, you
will need to execute the NewInstall.sql script in MySQL. This script will create the
Starexec database and stored procedures, along with creating a few data entries that
Starexec expects by default. For example, you can execute

```sh
mysql -u"username" -p"password" < NewInstall.sql
```

5) Certain users and groups will need to exist to give Starexec appropriate permissions.

Ensure SANDBOX_USER_ONE and SANDBOX_USER_TWO have been created on your head node and
all compute nodes.

Create the `star-web` group.

Create the user 'tomcat' and add this user to the 'star-web' group. Add the
tomcat user to the star-web group, and change the primary group for tomcat
to star-web. Tomcat is the user that you will need to use when starting up
tomcat using startup.sh in the tomcat bin folder. You should also ensure
that 'tomcat' is the owner of the entire tomcat installation directory

Finally, any users that are going to be administering Starexec should
also be added to the 'star-web' group. Being a member of star-web will
be necessary for correctly executing the Starexec deploy scripts.

Create the `sandbox` group, and add SANDBOX_USER_ONE to this group.
Create another group `sandbox2` and add SANDBOX_USER_TWO to this group.
Add the `tomcat` user to both of these groups.

If you are using SGE as a backend, you need to create the user `sgeadmin`
and ensure this user does have administrator privileges in SGE.

6) A sandbox directory will need to be created on the Starexec
head node. This directory is used to execute user-provided scripts in a
sandboxed environment, preventing them from affecting other parts of the
system. You should create a directory named 'sandbox' at the location
you specified in starexec-config.xml. Make the owner SANDBOX_USER_ONE,
and make the group `sandbox`. Use chmod on the directory to make
permissions 770. Additionally, use chmod g+s to set the GID for the
directory. Finally, use the following command to ensure that new
directories in the sandbox have g+rwx permissions.

```sh
setfacl -d -m g::rwx sandbox
```

7) The directory that you configured as BACKEND_WORKING_DIR needs to be created. tomcat
should be the owner and star-web should be the group. Under this directory, create
two directories named `sandbox` and `sandbox2`. These should also use the tomcat
user and the star-web group.

8) Sudo permissions need to be configured. Starexec uses sudo in several locations
to execute commands as other users, most often to execute commands using the
SANDBOX_USER_ONE and SANDBOX_USER_TWO users. The 'tomcat' user will need all
of the following sudo permissions.


HEAD NODE
User tomcat may run the following commands on this host:

    (SANDBOX_USER_ONE) NOPASSWD: ALL
    (SANDBOX_USER_TWO) NOPASSWD: ALL
    (root) NOPASSWD: /sbin/service tomcat7 restart, /sbin/service tomcat7 stop, /sbin/service tomcat7 start

The following entries are needed only if you are using an SGE backend. Replace `/cluster/sge-6.2u5/bin/lx24-amd64/`
in each path with your install directory

    (sgeadmin) NOPASSWD: /cluster/sge-6.2u5/bin/lx24-amd64/qconf, /cluster/sge-6.2u5/bin/lx24-amd64/qmod, /cluster/gridengine-8.1.8/bin/lx-amd64/qconf,
    /cluster/gridengine-8.1.8/bin/lx-amd64/qmod


COMPUTE NODE (or head node if you are using a local backend)



User tomcat may run the following commands on this host:

    (sandbox) NOPASSWD: ALL

For all of the following commands, the prefix `/export/starexec` should be replaced with your configured value of
`BACKEND_WORKING_DIR`.

    (root) NOPASSWD: /bin/chown -R SANDBOX_USER_ONE /export/starexec/sandbox, /bin/chown -R tomcat /export/starexec/sandbox, /bin/chown -R SANDBOX_USER_ONE
    /export/starexec/sandbox/benchmark/theBenchmark.cnf, /bin/chown -R tomcat /export/starexec/sandbox/benchmark/theBenchmark.cnf
    (sandbox2) NOPASSWD: ALL
    (root) NOPASSWD: /bin/chown -R SANDBOX_USER_TWO /export/starexec/sandbox2, /bin/chown -R tomcat /export/starexec/sandbox2, /bin/chown -R SANDBOX_USER_TWO
    /export/starexec/sandbox2/benchmark/theBenchmark.cnf, /bin/chown -R tomcat /export/starexec/sandbox2/benchmark/theBenchmark.cnf

The same applies as on the head node for the following commands

    (sgeadmin) NOPASSWD: /cluster/sge-6.2u5/bin/lx24-amd64/qconf, /cluster/sge-6.2u5/bin/lx24-amd64/qmod, /cluster/gridengine-8.1.8/bin/lx-amd64/qconf,
    /cluster/gridengine-8.1.8/bin/lx-amd64/qmod
