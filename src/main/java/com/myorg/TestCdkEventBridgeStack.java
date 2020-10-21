package com.myorg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.events.EventPattern;
import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.events.targets.LambdaFunction;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TestCdkEventBridgeStack extends Stack {

    Logger logger = LoggerFactory.getLogger(TestCdkEventBridgeStack.class);

    public TestCdkEventBridgeStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public TestCdkEventBridgeStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        logger.info("Creating Lambda [pullRequestBuild]");
        final Function pullRequestBuildLambda = Function.Builder.create(this, "pullRequestBuildLambda")
                .runtime(Runtime.PYTHON_3_8)
                .code(Code.fromAsset("lambda"))
                .handler("pullRequestBuild.handler")
                .build();

        List<LambdaFunction> pullRequestBuildLambdaList = new ArrayList<>();
        pullRequestBuildLambdaList.add(new LambdaFunction(pullRequestBuildLambda));

        logger.info("Creating EventPattern [CodeCommit Pull Request State Change]");
        EventPattern codeCommitEventPattern = EventPattern.builder()
                .source(Collections.singletonList("aws.codecommit"))
                .detailType(Collections.singletonList("CodeCommit Pull Request State Change"))
                .build();

        logger.info("Creating EventRule [OnPullRequest State Change]");
        Rule.Builder.create(this, "CodeCommitEventRule")
                .ruleName("OnPullRequestStateChangeEventRule")
                .enabled(true)
                .description("Event rule for onPullRequestStateChange for any branch")
                .eventPattern(codeCommitEventPattern)
                .targets(pullRequestBuildLambdaList)
                .build();

        logger.info("Creating Lambda [pullRequestBuildResult]");
        final Function pullRequestBuildResultLambda = Function.Builder.create(this, "pullRequestBuildResultLambda")
                .runtime(Runtime.PYTHON_3_8)
                .code(Code.fromAsset("lambda"))
                .handler("pullRequestBuildResult.handler")
                .build();

        List<LambdaFunction> pullRequestBuildResultLambdaList = new ArrayList<>();
        pullRequestBuildResultLambdaList.add(new LambdaFunction(pullRequestBuildResultLambda));

        logger.info("Creating EventPattern [CodeBuild Build State Change]");
        EventPattern codeBuildResultEventPattern = EventPattern.builder()
                .source(Collections.singletonList("aws.codebuild"))
                .detailType(Collections.singletonList("CodeBuild Build State Change"))
                .detail(Collections.singletonMap("build-status", Arrays.asList("SUCCEEDED", "FAILED", "STOPPED")))
                .build();

        logger.info("Creating EventRule [PullRequestBuildResult from CodeBuild]");
        Rule.Builder.create(this, "CodeBuildResultEventRule")
                .ruleName("PullRequestBuildResultEventRule")
                .enabled(true)
                .description("Event rule for PullRequestBuildResult from CodeBuild")
                .eventPattern(codeBuildResultEventPattern)
                .targets(pullRequestBuildResultLambdaList)
                .build();
    }
}
