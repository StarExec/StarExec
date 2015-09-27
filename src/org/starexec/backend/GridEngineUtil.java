package org.starexec.backend;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.ggf.drmaa.Session;
import org.ggf.drmaa.SessionFactory;
import org.starexec.backend.GridEngineBackend;
import org.starexec.util.Util;


/**
 * Contains methods for interacting with the sun grid engine. This class is NOT operating system independent
 * and may require environmental set up to function properly in Windows.
 * @author Tyler Jensen
 */
public class GridEngineUtil {
	private static final Logger log = Logger.getLogger(GridEngineUtil.class);

	// The regex patterns used to parse SGE output
	public static Pattern nodeKeyValPattern;
	public static Pattern queueKeyValPattern;
	public static Pattern queueAssocPattern;

	@SuppressWarnings("unused")
	private static String testString = "queuename                      qtype resv/used/tot. load_avg arch          states\r\n" + 
	"---------------------------------------------------------------------------------\r\n" + 
	"all.q@n001.star.cs.uiowa.edu   BIP   0/0/8          0.05     lx24-amd64    \r\n" + 
	"---------------------------------------------------------------------------------\r\n" + 
	"all.q@n002.star.cs.uiowa.edu   BIP   0/0/8          0.02     lx24-amd64    \r\n" + 
	"---------------------------------------------------------------------------------\r\n" + 
	"all.q@n003.star.cs.uiowa.edu   BIP   0/0/8          0.07     lx24-amd64    \r\n" + 
	"---------------------------------------------------------------------------------\r\n" + 
	"all.q@n004.star.cs.uiowa.edu   BIP   0/0/8          0.02     lx24-amd64    \r\n" + 
	"---------------------------------------------------------------------------------\r\n" + 
	"n001.q@n001.star.cs.uiowa.edu  BIP   0/0/1          0.05     lx24-amd64    \r\n" + 
	"---------------------------------------------------------------------------------\r\n" + 
	"n002.q@n002.star.cs.uiowa.edu  BIP   0/0/1          0.02     lx24-amd64    \r\n" + 
	"---------------------------------------------------------------------------------\r\n" + 
	"n003.q@n003.star.cs.uiowa.edu  BIP   0/0/1          0.07     lx24-amd64    \r\n" + 
	"---------------------------------------------------------------------------------\r\n" + 
	"n004.q@n004.star.cs.uiowa.edu  BIP   0/0/1          0.02     lx24-amd64    ";

	static {
		// Compile the SGE output parsing patterns when this class is loaded
		nodeKeyValPattern = Pattern.compile(GridEngineR.NODE_DETAIL_PATTERN, Pattern.CASE_INSENSITIVE);
		queueKeyValPattern = Pattern.compile(GridEngineR.QUEUE_DETAIL_PATTERN, Pattern.CASE_INSENSITIVE);
		queueAssocPattern = Pattern.compile(GridEngineR.QUEUE_ASSOC_PATTERN, Pattern.CASE_INSENSITIVE);

	}
	
    public static boolean createPermanentQueue(boolean isNewQueue,String BACKEND_ROOT,String queueName, String[] nodeNames,String[] queueNames){
		try {
			log.debug("begin createPermanentQueue");
			String[] split = queueName.split("\\.");
			String shortQueueName = split[0];
	
			StringBuilder sb = new StringBuilder();
			
			String[] envp = new String[1];
			envp[0] = "SGE_ROOT="+BACKEND_ROOT;
	
			
			if (isNewQueue) {
				//This is being called from "Create new permanent queue"
				if (nodeNames != null) {
				    for (int i=0;i<nodeNames.length;i++) {
						String fullName = nodeNames[i];
						String[] split2 = fullName.split("\\.");
						String shortName = split2[0];
						sb.append(shortName);
						sb.append(" ");
						

						String sourceQueueName = queueNames[i];
						//if node is not orphaned
						if(sourceQueueName != null){
						    String[] split3 = sourceQueueName.split("\\.");
						    String shortQName = split3[0];
						    log.debug("About to execute sudo command 1");
						    Util.executeCommand("sudo -u sgeadmin /cluster/sge-6.2u5/bin/lx24-amd64/qconf -dattr hostgroup hostlist " + fullName + " @" + shortQName + "hosts", envp);
						}
					}
				}
				
			} else {
			    //This is being called from "Make existing queue permanent"
				
			    //TODO : What's this supposed to do?  Doesn't seem to be doing what it should
			    //Get the nodes we are going to transfer
			    //List<WorkerNode> nodes = Queues.getNodes(1);
			    for (int i = 0; i < nodeNames.length; i++) {
				String fullName = nodeNames[i];
				String[] split2 = fullName.split("\\.");
				String shortName = split2[0];
				sb.append(shortName);
				sb.append(" ");
					
				// Transfer nodes out of @allhosts
				log.debug("About to execute sudo command 2");
					Util.executeCommand("sudo -u sgeadmin /cluster/sge-6.2u5/bin/lx24-amd64/qconf -dattr hostgroup hostlist " + fullName + " @allhosts", envp);
				}
			}
			
			String hostList = sb.toString();
	
			/***** CREATE A QUEUE *****/
			// Create newHost.hgrp
			String newHost;
		
			newHost = "group_name @" + shortQueueName + "hosts" +
					  "\nhostlist " + hostList;
			File f = new File("/tmp/newHost30.hgrp");
			FileUtils.writeStringToFile(f, newHost);
			f.setReadable(true, false);
			f.setWritable(true, false);

			

		//Add the host

			log.debug("About to execute sudo command 3");
		Util.executeCommand("sudo -u sgeadmin /cluster/sge-6.2u5/bin/lx24-amd64/qconf -Ahgrp /tmp/newHost30.hgrp", envp);
		
			
			
		// Create newQueue.q [COMPLETE]
		String newQueue;
		
			newQueue = "qname                   " + queueName + 
						"\nhostlist             @" + shortQueueName + "hosts" + 
						"\nseq_no                0" +
						"\nload_thresholds       np_load_avg=1.75" +
						"\nsuspend_thresholds    NONE" +
						"\nnsuspend              1" +
						"\nsuspend_interval      00:05:00" +
						"\npriority              0" +
						"\nmin_cpu_interval      00:05:00" +
						"\nprocessors            UNDEFINED" +
						"\nqtype                 BATCH INTERACTIVE" +
						"\nckpt_list             NONE" +
						"\npe_list               make" +
						"\nrerun                 FALSE" +
						"\nslots                 2" +
						"\ntmpdir                /tmp" +
						"\nshell                 /bin/csh" +
						"\nprolog                NONE" +
						"\nepilog                NONE" +
						"\nshell_start_mode      posix_compliant" +
						"\nstarter_method        NONE" +
						"\nsuspend_method        NONE" +
						"\nresume_method         NONE" +
						"\nterminate_method      NONE" +
						"\nnotify                00:00:60"+
						"\nowner_list            NONE"+
						"\nuser_lists            NONE"+
						"\nxuser_lists           NONE"+
						"\nsubordinate_list      NONE"+
						"\ncomplex_values        NONE"+
						"\nprojects              NONE"+
						"\nxprojects             NONE"+
						"\ncalendar              NONE"+
						"\ninitial_state         default"+
						"\ns_rt                  INFINITY"+
						"\nh_rt                  INFINITY"+
						"\ns_cpu                 INFINITY"+
						"\nh_cpu                 INFINITY"+
						"\ns_fsize               INFINITY"+
						"\nh_fsize               INFINITY"+
						"\ns_data                INFINITY"+
						"\nh_data                INFINITY"+
						"\ns_stack               INFINITY"+
						"\nh_stack               INFINITY"+
						"\ns_core                INFINITY"+
						"\nh_core                INFINITY"+
						"\ns_rss                 INFINITY"+
						"\nh_rss                 INFINITY"+
						"\ns_vmem                INFINITY"+
						"\nh_vmem                INFINITY";
			
			File f2 = new File("/tmp/newQueue30.q");
			FileUtils.writeStringToFile(f2, newQueue);
			f2.setReadable(true, false);
			f2.setWritable(true, false);
				
			log.debug("About to execute sudo command 4");
			Util.executeCommand("sudo -u sgeadmin /cluster/sge-6.2u5/bin/lx24-amd64/qconf -Aq /tmp/newQueue30.q", envp);
		
			
	

		    log.debug("created permanent queue successfully");
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		return false;

	}

	/**
	 * Moves the given set of nodes into the given queue
	 * @param BACKEND_ROOT the location to the backend root, if sge found at R.SGE_ROOT
	 * @param queueName The name of the queue we are moving nodes to
	 * @param nodeNames the nodes to be moved into <queueName>
	 * @param sourceQueueNames the names of the source queues, currently own their corresponding node
	 */

    public static boolean moveNodes(String BACKEND_ROOT,String queueName, String[] nodeNames, String[] sourceQueueNames) {
    	try {
    		log.info("moveNodes begins, for queue "+queueName);
    		String[] split = queueName.split("\\.");
    		String shortQueueName = split[0];
    		StringBuilder sb = new StringBuilder();
    			
    		String[] envp = new String[1];
    		envp[0] = "SGE_ROOT="+BACKEND_ROOT;

    		if ((nodeNames == null) || (nodeNames.length == 0))
    			log.warn("No nodes to move");
    		else {
    		    for(int i=0;i<nodeNames.length;i++){
    			//String fullName = n.getName();
			String nodeFullName = nodeNames[i];
    			String[] split2 = nodeFullName.split("\\.");
    			String shortName = split2[0];
    			sb.append(shortName);
    			sb.append(" ");
    					
    			log.debug("moving node "+nodeFullName);


		//remove the association with this node and the queue it is currently associated with and add it to the permanent queue

		
		if (sourceQueueNames[i] != null) {
		    // orphaned nodes could have null queues
		    
		    String name = sourceQueueNames[i];
		    String[] split3 = name.split("\\.");
		    String shortQName = split3[0];
		    Util.executeCommand("sudo -u sgeadmin /cluster/sge-6.2u5/bin/lx24-amd64/qconf -dattr hostgroup hostlist " + nodeFullName + " @" + shortQName + "hosts", envp);
		}
		log.debug("adding node with name = "+nodeFullName +" to queue = "+shortQueueName);
		Util.executeCommand("sudo -u sgeadmin /cluster/sge-6.2u5/bin/lx24-amd64/qconf -aattr hostgroup hostlist " + nodeFullName + " @" + shortQueueName + "hosts", envp);
	    }
	}

	log.debug("Move nodes ending.");
    	return true;
    	} catch (Exception e) {
    		log.error(e.getMessage(),e);
    	}
    	return false;
	

    }
}