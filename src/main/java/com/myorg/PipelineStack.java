package com.myorg;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.codebuild.BuildEnvironment;
import software.amazon.awscdk.services.codebuild.BuildSpec;
import software.amazon.awscdk.services.codebuild.LinuxBuildImage;
import software.amazon.awscdk.services.codebuild.PipelineProject;
import software.amazon.awscdk.services.codecommit.IRepository;
import software.amazon.awscdk.services.codecommit.Repository;
import software.amazon.awscdk.services.codepipeline.Artifact;
import software.amazon.awscdk.services.codepipeline.Pipeline;
import software.amazon.awscdk.services.codepipeline.StageProps;
import software.amazon.awscdk.services.codepipeline.actions.CloudFormationCreateUpdateStackAction;
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildAction;
import software.amazon.awscdk.services.codepipeline.actions.CodeCommitSourceAction;
import software.amazon.awscdk.services.lambda.CfnParametersCode;

public class PipelineStack extends Stack {

    // alternate constructor for calls without props.
    // lambdaCode and repoName are both required.
    public PipelineStack(final App scope, final String id, final CfnParametersCode lambdaCode, final String repoName) {
        this(scope, id, null, lambdaCode, repoName);
    }

    @SuppressWarnings("serial")
    public PipelineStack(final App scope, final String id, final StackProps props,
                         final CfnParametersCode lambdaCode, final String repoName) {
        super(scope, id, props);

        IRepository code = Repository.fromRepositoryName(this, "ImportedRepo", repoName);

        PipelineProject cdkBuild = PipelineProject.Builder.create(this, "CDKBuild")
                .buildSpec(createCdkBuildSpecification())
                .environment(BuildEnvironment.builder().buildImage(LinuxBuildImage.STANDARD_2_0).build())
                .build();

        PipelineProject lambdaBuild = PipelineProject.Builder.create(this, "LambdaBuild")
                .buildSpec(createLambdaBuildSpecification())
                .environment(BuildEnvironment.builder().buildImage(LinuxBuildImage.STANDARD_2_0).build())
                .build();

        Artifact sourceOutput = new Artifact();
        Artifact cdkBuildOutput = new Artifact("CdkBuildOutput");
        Artifact lambdaBuildOutput = new Artifact("LambdaBuildOutput");

        Pipeline.Builder.create(this, "Pipeline")
                .stages(Arrays.asList(
                        getSourceStage(code, sourceOutput),
                        getBuildStage(cdkBuild, lambdaBuild, sourceOutput, cdkBuildOutput, lambdaBuildOutput),
                        getDeployStage(lambdaCode, cdkBuildOutput, lambdaBuildOutput)))
                .build();
    }

    private BuildSpec createLambdaBuildSpecification() {
        return BuildSpec.fromObject(new HashMap<String, Object>() {{
            put("version", "0.2");
            put("phases", new HashMap<String, Object>() {{
                put("install", new HashMap<String, List<String>>() {{
                    put("commands", Arrays.asList("cd lambda", "npm install",
                            "npm install typescript"));
                }});
                put("build", new HashMap<String, List<String>>() {{
                    put("commands", Collections.singletonList("npx tsc index.ts"));
                }});
            }});
            put("artifacts", new HashMap<String, Object>() {{
                put("base-directory", "lambda");
                put("files", Arrays.asList("index.js", "node_modules/**/*"));
            }});
        }});
    }

    private BuildSpec createCdkBuildSpecification() {
        return BuildSpec.fromObject(new HashMap<String, Object>() {{
            put("version", "0.2");
            put("phases", new HashMap<String, Object>() {{
                put("install", new HashMap<String, String>() {{
                    put("commands", "npm install aws-cdk");
                }});
                put("build", new HashMap<String, Object>() {{
                    put("commands", Arrays.asList("mvn compile -q -DskipTests",
                            "npx cdk synth -o dist"));
                }});
            }});
            put("artifacts", new HashMap<String, Object>() {{
                put("base-directory", "dist");
                put("files", Collections.singletonList("LambdaStack.template.json"));
            }});
        }});
    }

    private StageProps getDeployStage(final CfnParametersCode lambdaCode, final Artifact cdkBuildOutput,
                                      final Artifact lambdaBuildOutput) {
        return StageProps.builder()
                .stageName("Deploy")
                .actions(Collections.singletonList(
                        CloudFormationCreateUpdateStackAction.Builder.create()
                                .actionName("Lambda_CFN_Deploy")
                                .templatePath(cdkBuildOutput.atPath("LambdaStack.template.json"))
                                .adminPermissions(true)
                                .parameterOverrides(lambdaCode.assign(lambdaBuildOutput.getS3Location()))
                                .extraInputs(Collections.singletonList(lambdaBuildOutput))
                                .stackName("LambdaDeploymentStack")
                                .build()))
                .build();
    }

    private StageProps getBuildStage(final PipelineProject cdkBuild, final PipelineProject lambdaBuild,
                                     final Artifact sourceOutput, final Artifact cdkBuildOutput,
                                     final Artifact lambdaBuildOutput) {
        return StageProps.builder()
                .stageName("Build")
                .actions(Arrays.asList(
                        CodeBuildAction.Builder.create()
                                .actionName("Lambda_Build")
                                .project(lambdaBuild)
                                .input(sourceOutput)
                                .outputs(Collections.singletonList(lambdaBuildOutput)).build(),
                        CodeBuildAction.Builder.create()
                                .actionName("CDK_Build")
                                .project(cdkBuild)
                                .input(sourceOutput)
                                .outputs(Collections.singletonList(cdkBuildOutput))
                                .build()))
                .build();
    }

    private StageProps getSourceStage(final IRepository code, final Artifact sourceOutput) {
        return StageProps.builder()
                .stageName("Source")
                .actions(Collections.singletonList(
                        CodeCommitSourceAction.Builder.create()
                                .actionName("Source")
                                .repository(code)
                                .output(sourceOutput)
                                .build()))
                .build();
    }
}