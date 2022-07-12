package ee.buerokratt.ruuter.domain.steps.http;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import ee.buerokratt.ruuter.StepTestBase;
import ee.buerokratt.ruuter.configuration.ApplicationProperties;
import ee.buerokratt.ruuter.helper.HttpHelper;
import ee.buerokratt.ruuter.helper.MappingHelper;
import ee.buerokratt.ruuter.service.ConfigurationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@WireMockTest
class HttpGetStepTest extends StepTestBase {

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private HttpHelper httpHelper;

    @Mock
    private MappingHelper mappingHelper;

    @Mock
    private ApplicationProperties.DefaultAction defaultAction;

    @Mock
    private ConfigurationService configurationService;

    @BeforeEach
    protected void mockDependencies() {
        when(ci.getHttpHelper()).thenReturn(httpHelper);
        when(ci.getProperties()).thenReturn(applicationProperties);
    }

    @Test
    void execute_shouldQueryEndpointAndStoreResponse(WireMockRuntimeInfo wireMockRuntimeInfo) {
        HashMap<String, Object> testContext = new HashMap<>();
        HttpQueryArgs expectedGetArgs = new HttpQueryArgs() {{
            setQuery(new HashMap<>() {{
                put("some_val", "Hello World");
                put("another_val", 123);
            }});
            setUrl("http://localhost:%s/endpoint".formatted(wireMockRuntimeInfo.getHttpPort()));
        }};
        HttpStep expectedGetStep = new HttpGetStep() {{
            setName("get_message");
            setArgs(expectedGetArgs);
            setResultName("the_response");
        }};
        ResponseEntity<Object> httpResponse = new ResponseEntity<>("body", null, HttpStatus.OK);

        when(ci.getMappingHelper()).thenReturn(mappingHelper);
        when(ci.getContext()).thenReturn(testContext);
        when(httpHelper.doGet(expectedGetArgs.getUrl(), expectedGetArgs.getQuery(), expectedGetArgs.getHeaders())).thenReturn(httpResponse);
        expectedGetStep.execute(ci);

        assertEquals(HttpStatus.OK, ((HttpStepResult) testContext.get("the_response")).getResponse().getStatusCode());
        assertEquals(httpResponse.getBody(), ((HttpStepResult) testContext.get("the_response")).getResponse().getBody());
    }

    @Test
    void execute_shouldExecuteDefaultActionWhenRequestIsInvalidAndStopInCaseOfExceptionIsTrue(WireMockRuntimeInfo wireMockRuntimeInfo) {
        HashMap<String, Object> testContext = new HashMap<>();
        HttpQueryArgs expectedGetArgs = new HttpQueryArgs() {{
            setQuery(new HashMap<>() {{
                put("some_val", "Hello World");
                put("another_val", 123);
            }});
            setUrl("http://localhost:%s/endpoint".formatted(wireMockRuntimeInfo.getHttpPort()));
        }};
        HttpStep expectedGetStep = new HttpGetStep() {{
            setName("get_message");
            setArgs(expectedGetArgs);
            setResultName("the_response");
        }};
        ResponseEntity<Object> httpResponse = new ResponseEntity<>("body", null, HttpStatus.CREATED);

        when(httpHelper.doGet(expectedGetArgs.getUrl(), expectedGetArgs.getQuery(), expectedGetArgs.getHeaders())).thenReturn(httpResponse);
        when(ci.getConfigurationService()).thenReturn(configurationService);
        when(ci.getMappingHelper()).thenReturn(mappingHelper);
        when(ci.getContext()).thenReturn(testContext);
        when(ci.getRequestOrigin()).thenReturn("");
        when(applicationProperties.getDefaultAction()).thenReturn(defaultAction);
        when(applicationProperties.getHttpCodesAllowList()).thenReturn(new ArrayList<>() {{add(HttpStatus.OK.value());}});
        when(defaultAction.getService()).thenReturn("default-action");
        when(defaultAction.getBody()).thenReturn(new HashMap<>());
        when(defaultAction.getQuery()).thenReturn(new HashMap<>());

        expectedGetStep.execute(ci);

        verify(configurationService, times(1)).execute(eq("default-action"), anyString(), anyMap(), anyMap(), anyString());
    }

    @Test
    void execute_shouldNotExecuteDefaultActionWhenRequestIsInvalidButDefaultActionIsNotDefined(WireMockRuntimeInfo wireMockRuntimeInfo) {
        HashMap<String, Object> testContext = new HashMap<>();
        HttpQueryArgs expectedGetArgs = new HttpQueryArgs() {{
            setQuery(new HashMap<>() {{
                put("some_val", "Hello World");
                put("another_val", 123);
            }});
            setUrl("http://localhost:%s/endpoint".formatted(wireMockRuntimeInfo.getHttpPort()));
        }};
        HttpStep expectedGetStep = new HttpGetStep() {{
            setName("get_message");
            setArgs(expectedGetArgs);
            setResultName("the_response");
        }};
        ResponseEntity<Object> httpResponse = new ResponseEntity<>("body", null, HttpStatus.CREATED);

        when(ci.getContext()).thenReturn(testContext);
        when(ci.getHttpHelper().doGet(expectedGetArgs.getUrl(), expectedGetArgs.getQuery(), expectedGetArgs.getHeaders())).thenReturn(httpResponse);
        when(applicationProperties.getHttpCodesAllowList()).thenReturn(new ArrayList<>() {{add(HttpStatus.OK.value());}});
        expectedGetStep.execute(ci);

        verify(configurationService, times(0)).execute(anyString(), anyString(), anyMap(), anyMap(), anyString());
    }
}
