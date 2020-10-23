package com.myorg;

import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.codedeploy.LambdaDeploymentConfig;
import software.amazon.awscdk.services.codedeploy.LambdaDeploymentGroup;
import software.amazon.awscdk.services.lambda.Alias;
import software.amazon.awscdk.services.lambda.CfnParametersCode;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.Version;

public class LambdaStack extends Stack {

    // private attribute to hold our Lambda's code, with public getters
    private CfnParametersCode lambdaCode;

    public CfnParametersCode getLambdaCode() {
        return lambdaCode;
    }

    // Constructor without props argument
    public LambdaStack(final App scope, final String id) {
        this(scope, id, null);
    }

    public LambdaStack(final App scope, final String id, final StackProps props) {
        super(scope, id, props);

        lambdaCode = CfnParametersCode.fromCfnParameters();

        Function func = Function.Builder.create(this, "Lambda")
                .code(lambdaCode)
                .handler("index.main")
                .runtime(Runtime.NODEJS_10_X).build();

        Version version = func.getCurrentVersion();
        Alias alias = Alias.Builder.create(this, "LambdaAlias")
                .aliasName("LambdaAlias")
                .version(version).build();

        LambdaDeploymentGroup.Builder.create(this, "DeploymentGroup")
                .alias(alias)
                .deploymentConfig(LambdaDeploymentConfig.ALL_AT_ONCE).build();
    }
}