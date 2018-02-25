# Installation Notes

## MySQL and MariaDB

In starexec-config.xml, you must specify MYSQL_USERNAME and
COMPUTE_NODE_MYSQL_USERNAME. These are MySQL users that must have access to the
MySQL database. If desired, they can be the same user-- the only difference is that
COMPUTE_NODE_MYSQL_USERNAME requires fewer permissions. The user set as
MYSQL_USERNAME will require ALL permissions in the StarExec database, excluding
server administration permissions. The user COMPUTE_NODE_MYSQL_USERNAME will
require SELECT, INSERT, UPDATE, and EXECUTE permissions.

A description of MySQL permissions can be found at the link below.

http://dev.mysql.com/doc/refman/5.7/en/privileges-provided.html

## Email

StarExec sends automated emails for several purposes, such as sending notifications
when new users are registered or sending weekly status updates. To do this, StarExec
requires an email address that it can send emails from. In starexec-config.xml,
the EMAIL_USER field should be set to the email address to send from, and EMAIL_PWD,
EMAIL_SMTP, and EMAIL_SMTP_PORT should be set as decribed.

Any email server that supports SMTP can be used, but the default settings included
in the config file show how to use a Gmail account. The EMAIL_SMTP field is set
correctly for Gmail, and the SMTP_PORT field is set to 587, as StarExec uses
TLS. You should be able to use any desired Gmail account by plugging in your own
account and password. Note that Gmail may initially block the StarExec app from
sending mail until you log onto your account and confirm the activity. Moreover,
you may need to go into your Gmail account settings and allow access from less
secure apps. Creating a fresh Gmail account for sole use by StarExec may be
beneficial if you do not want to change your settings.

StarExec has been tested using Gmail, but other email servers should also work
provided you look up the correct SMTP server and port. Various settings may
need to be changed in other email providers as well.

StarExec is also configured to use a CONTACT_EMAIL, which is intended to
receive emails directed at StarExec admins. This email address will appear
on the site for users who want to send bug reports or ask questions.

## StarExec Configuration

After all of the above dependencies have been installed, you can take the following
steps to configure and deploy StarExec.

1) Under src/org/starexec/config you will see the file starexec-config.xml. This file
contains the bulk of StarExec configuration, including specifying StarExec data directories,
email settings, database connection settings, and several other items. You will need
to go through this file and configure StarExec as you want, according to the documentation
provided in that file.

2) In the top-level file local.properties, you will need to provide several configurable values.
These values are mostly identical to values in starexec-config.xml.

3) Run 'ant' at the top level. This will cause Ant to build starexec according to the rules
specified in build.xml.

4) In the deployed-sql directory, which should have been created by the Ant build, you
will need to execute the NewInstall.sql script in MySQL. This script will create the
StarExec database and stored procedures, along with creating a few data entries that
StarExec expects by default. For example, you can execute

```sh
mysql -u"username" -p"password" < NewInstall.sql
```

5) Certain users and groups will need to exist to give StarExec appropriate permissions.

Ensure SANDBOX_USER_ONE and SANDBOX_USER_TWO have been created on your head node and
all compute nodes.

Create the `star-web` group.

Create the user 'tomcat' and add this user to the 'star-web' group. Add the
tomcat user to the star-web group, and change the primary group for tomcat
to star-web. Tomcat is the user that you will need to use when starting up
tomcat using startup.sh in the tomcat bin folder. You should also ensure
that 'tomcat' is the owner of the entire tomcat installation directory

Finally, any users that are going to be administering StarExec should
also be added to the 'star-web' group. Being a member of star-web will
be necessary for correctly executing the StarExec deploy scripts.

Create the `sandbox` group, and add SANDBOX_USER_ONE to this group.
Create another group `sandbox2` and add SANDBOX_USER_TWO to this group.
Add the `tomcat` user to both of these groups.

If you are using SGE as a backend, you need to create the user `sgeadmin`
and ensure this user does have administrator privileges in SGE.

6) A sandbox directory will need to be created on the StarExec
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

8) Sudo permissions need to be configured. StarExec uses sudo in several locations
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
