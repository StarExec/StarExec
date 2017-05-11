package org.starexec.test.junit.app;
import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.starexec.app.RESTHelpers;
import org.starexec.app.RESTServices;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.enums.Primitive;
import org.starexec.util.Util;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RESTHelpers.class, Util.class})
public class RESTServicesTests {

	private final Gson gson = new Gson();
    private final String instance = "test_instance";
    private final String benchType = Primitive.BENCHMARK.toString();


    @Before
	public void initialize() {
		PowerMockito.mockStatic(RESTHelpers.class);
		PowerMockito.mockStatic(Util.class);
	}

    @Test
    public void copyToStarDevFailValidationTest() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		ValidatorStatusCode failedValidation = new ValidatorStatusCode(false);
		given(RESTHelpers.validateCopyToStardev(request, benchType)).willReturn(failedValidation);
        RESTServices services = new RESTServices();
		assertEquals("Should fail validation.", services.copyToStarDev(instance, benchType, 10, request), gson.toJson(failedValidation));
    }

    @Test
    public void copyToStarDevFailLoginTest() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        ValidatorStatusCode successValidation = new ValidatorStatusCode(true);
        given(RESTHelpers.validateCopyToStardev(request, benchType)).willReturn(successValidation);
    }

}
