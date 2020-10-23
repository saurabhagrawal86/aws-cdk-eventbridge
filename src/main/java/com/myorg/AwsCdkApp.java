package com.myorg;

import software.amazon.awscdk.core.App;

public class AwsCdkApp {

    static final String CODECOMMIT_REPO_NAME = "my-new-dummy-repository";

    public static void main(final String[] args) {
        App app = new App();

        //new EventBridgeStack(app, "EventBridgeStack");
        LambdaStack lambdaStack = new LambdaStack(app, "LambdaStack");
        new PipelineStack(app, "PipelineDeployingLambdaStack", lambdaStack.getLambdaCode(), CODECOMMIT_REPO_NAME);

        app.synth();
    }
}
