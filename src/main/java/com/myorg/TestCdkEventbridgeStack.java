package com.myorg;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.codecommit.Repository;
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

        //Create a CodeCommit Repository
        Repository repository = Repository.Builder.create(this, "myTestRepository")
                .repositoryName("mytestrepository")
                .description("This is a test repository")
                .build();

        //Create a lambda function
        final Function pullRequestBuildLambda = Function.Builder.create(this, "pullRequestBuildLambda")
                .runtime(Runtime.PYTHON_3_8)
                .code(Code.fromAsset("lambda"))
                .handler("pullRequestBuild.handler")
                .build();

        //Create a onPullRequest state change event rule on a repository and trigger a lambda
        Rule eventRule = repository.onPullRequestStateChange("OnPullRequestStateChangeEventRule");
        eventRule.addTarget(new LambdaFunction(pullRequestBuildLambda));
    }
}
