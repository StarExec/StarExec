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

	Gson gson = new Gson();
	@Before
	public void initialize() {
		PowerMockito.mockStatic(RESTHelpers.class);
		PowerMockito.mockStatic(Util.class);
	}

    @Test
    public void copyToStarDevFailValidationTest() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		final String type = Primitive.BENCHMARK.toString();
		ValidatorStatusCode failedValidation = new ValidatorStatusCode(false);
		given(RESTHelpers.validateCopyToStardev(request, type)).willReturn(failedValidation);

		RESTServices services = new RESTServices();
		final String instance = "test_instance";
		assertEquals("Should fail validation.", services.copyToStarDev(instance, type, 10, request), gson.toJson(failedValidation));
    }
}
