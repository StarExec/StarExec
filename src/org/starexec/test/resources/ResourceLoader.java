package org.starexec.test.resources;

import java.io.File;
import java.util.UUID;

import javax.servlet.ServletContext;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Requests;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.test.TestUtil;
import org.starexec.util.ArchiveUtil;

public class ResourceLoader {
	private static final Logger log = Logger.getLogger(ResourceLoader.class);	
	
	public static String getResourcePath() {
		return ResourceLoader.class.getResource("/org/starexec/test/resources").getFile();
	}
	public static File getResource(String name) {
		return new File(ResourceLoader.class.getResource("/org/starexec/test/resources/"+name).getFile());
	}
	public static Solver loadSolverIntoDatabase(String archiveName, int parentSpaceId, int userId) {
		try {
			Solver s=new Solver();
			s.setName(TestUtil.getRandomSolverName());
			s.setDescription("solver coming from here");
			s.setUserId(userId);
			File archive=getResource(archiveName);
			String filePath=Solvers.getDefaultSolverPath(userId, s.getName());
			s.setPath(filePath);
			File solverDir=new File(filePath);
			solverDir.mkdirs();
			
			FileUtils.copyFileToDirectory(archive, solverDir);
			File archiveFile=new File(solverDir,s.getName());
			ArchiveUtil.extractArchive(archiveFile.getAbsolutePath());
			
			//Find configurations from the top-level "bin" directory
			for(Configuration c : Solvers.findConfigs(solverDir.getAbsolutePath())) {
				s.addConfiguration(c);
			}
			
			int id=Solvers.add(s, parentSpaceId);
			if (id>0) {
				s.setId(id);
				return s;
			} else {
				// there was an error of some kind
				return null;
			}
		} catch (Exception e) {
			log.error("loadSolverIntoDatabase says "+e.getMessage(),e);
		}
		return null;	
	}
	
	public static Space loadSpaceIntoDatabase(int userId, int parentSpaceId) {
		Space space=new Space();
		space.setName(TestUtil.getRandomSpaceName());
		space.setDescription("test desc");
		int id=Spaces.add(space, parentSpaceId, userId);
		if (id>0) {
			space.setId(id);
			return space;
		} else {
			return null;
		}
	}
	
	public static User loadUserIntoDatabase(String fname, String lname) {
		User u=new User();
		u.setFirstName(fname);
		u.setLastName(lname);
		u.setPassword(TestUtil.getRandomPassword());
		u.setEmail(TestUtil.getRandomPassword());
		u.setInstitution("The University of Iowa");
		u.setRole("user");
		String code=UUID.randomUUID().toString();
		Users.register(u,Communities.getTestCommunity().getId(),code,"");
		int id=Requests.redeemActivationCode(code); //required to "activate" the user
		if (id>0) {
			u.setId(id);
			return u;
		}
		log.debug("loadUserIntoDatabase could not generate a user, returning null");
		return null;
	}
	
}
