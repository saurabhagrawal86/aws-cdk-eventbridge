package com.myorg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.junit.Test;

import java.io.IOException;

import software.amazon.awscdk.core.App;

import static org.junit.Assert.assertEquals;

public class AwsCdkAppTest {
    private final static ObjectMapper JSON =
            new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true);

    //@Test
    public void test_EventBridgeStack() throws IOException {
        App app = new App();
        EventBridgeStack stack = new EventBridgeStack(app, "test");

        // synthesize the stack to a CloudFormation template and compare against
        // a checked-in JSON file.
        JsonNode actual = JSON.valueToTree(app.synth().getStackArtifact(stack.getArtifactId()).getTemplate());
        JsonNode expected = JSON.readTree(getClass().getResource("expected.eventbridgestack.json"));
        assertEquals(expected, actual);
    }

    @Test
    public void test_PipelineStack() throws IOException {
        App app = new App();

        LambdaStack lambdaStack = new LambdaStack(app, "LambdaStack");
        PipelineStack stack = new PipelineStack(app, "PipelineDeployingLambdaStack", lambdaStack.getLambdaCode(),
                "repositoryName");

        // synthesize the stack to a CloudFormation template and compare against a checked-in JSON file.
        JsonNode actual = JSON.valueToTree(app.synth().getStackArtifact(stack.getArtifactId()).getTemplate());
        JsonNode expected = JSON.readTree(getClass().getResource("expected.pipelinestack.json"));
        assertEquals(expected, actual);
    }

}
