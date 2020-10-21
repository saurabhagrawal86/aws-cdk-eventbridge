package com.myorg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.events.EventPattern;
import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.events.targets.LambdaFunction;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;

public class TestCdkEventbridgeStack extends Stack {
    public TestCdkEventbridgeStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public TestCdkEventbridgeStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        //Create a lambda function
        final Function pullRequestBuildLambda = Function.Builder.create(this, "pullRequestBuildLambda")
                .runtime(Runtime.PYTHON_3_8)
                .code(Code.fromAsset("lambda"))
                .handler("pullRequestBuild.handler")
                .build();

        List<LambdaFunction> lambdaFunctionList = new ArrayList<>();
        lambdaFunctionList.add(new LambdaFunction(pullRequestBuildLambda));

        EventPattern codeCommitEventPattern = EventPattern.builder()
                .source(Collections.singletonList("aws.codecommit"))
                .detailType(Collections.singletonList("CodeCommit Pull Request State Change"))
                .build();

        Rule.Builder.create(this, "aNewEventRule")
                .ruleName("OnPullRequestStateChangeEventRule")
                .enabled(true)
                .description("Event rule for onPullRequestStateChange for any branch")
                .eventPattern(codeCommitEventPattern)
                .targets(lambdaFunctionList)
                .build();
    }
}
