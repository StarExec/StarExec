package org.starexec.test.integration.app;

import com.google.gson.Gson;
import org.junit.Assert;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.starexec.app.RESTServices;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.User;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.starexec.test.resources.ResourceLoader;
import org.starexec.util.SessionUtil;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by agieg on 2/16/2017.
 */
public class RESTServicesTests extends TestSequence {
    final RESTServices services = new RESTServices();

    User user = null;
    User admin = null;
    final ResourceLoader loader = new ResourceLoader();
    Gson gson = new Gson();


    @StarexecTest
    private void adminCantSubscribeNormalUserToErrorLogsTest() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        BDDMockito.given(SessionUtil.getUserId(request)).willReturn(admin.getId());
        String json = services.subscribeUserToErrorLogs(user.getId(), request);
        ValidatorStatusCode status = gson.fromJson(json, ValidatorStatusCode.class);

        Assert.assertFalse(status.isSuccess());
    }

    @Override
    protected String getTestName() {
        return "RESTServicesTests";
    }

    @Override
    protected void setup() throws Exception {
        user = loader.loadUserIntoDatabase();
        admin = loader.loadAdminIntoDatabase();

    }

    @Override
    protected void teardown() throws Exception {
        loader.deleteAllPrimitives();
    }
}
