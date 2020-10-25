package com.myorg;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.codebuild.BuildEnvironment;
import software.amazon.awscdk.services.codebuild.BuildEnvironmentVariable;
import software.amazon.awscdk.services.codebuild.BuildEnvironmentVariableType;
import software.amazon.awscdk.services.codebuild.BuildSpec;
import software.amazon.awscdk.services.codebuild.LinuxBuildImage;
import software.amazon.awscdk.services.codebuild.PipelineProject;
import software.amazon.awscdk.services.codecommit.IRepository;
import software.amazon.awscdk.services.codecommit.Repository;
import software.amazon.awscdk.services.codepipeline.Artifact;
import software.amazon.awscdk.services.codepipeline.Pipeline;
import software.amazon.awscdk.services.codepipeline.StageProps;
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildAction;
import software.amazon.awscdk.services.codepipeline.actions.CodeCommitSourceAction;
import software.amazon.awscdk.services.iam.IRole;
import software.amazon.awscdk.services.iam.Role;
import software.constructs.Construct;

public class PipelineStack extends Stack {

    Logger logger = LoggerFactory.getLogger(PipelineStack.class);

    final static String CODECOMMIT_REPO_NAME = "my-new-dummy-repository";

    final static ObjectMapper JSON = new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true);

    Map<String, Object> masterReleaseBuildSpecificationMap;

    public PipelineStack(final @Nullable Construct scope, @Nullable final String id) {
        super(scope, id);
    }

    public PipelineStack(final @Nullable Construct scope, @Nullable final String id, final @Nullable StackProps props) {
        super(scope, id, props);

        try {
            ClassLoader classLoader = getClass().getClassLoader();
            File file = new File(Objects.requireNonNull(classLoader.getResource("masterReleaseBuildSpec.json")).getFile());
            Gson gson = new Gson();
            JsonReader reader = new JsonReader(new FileReader(file));
            Type mapType = new TypeToken<Map<String, Object>>() {
            }.getType();
            masterReleaseBuildSpecificationMap = gson.fromJson(reader, mapType);
        } catch (IOException e) {
            logger.error("Unable to read the buildSpec JSON file. Error [" + e.getMessage() + "]");
        }

        //Create an ECR repository
        software.amazon.awscdk.services.ecr.Repository repository =
                software.amazon.awscdk.services.ecr.Repository.Builder.create(this,
                        "ecrRepository")
                        .repositoryName("releasepipelineecrrepository")
                        .build();

        //Get ECR repository URI
        String ecrRepositoryURI = repository.getRepositoryUri();

        //Build a CodeBuild environment variable for ECR repository URI
        BuildEnvironmentVariable variable = BuildEnvironmentVariable.builder()
                .type(BuildEnvironmentVariableType.PLAINTEXT)
                .value(ecrRepositoryURI).build();

        Map<String, BuildEnvironmentVariable> buildEnvironmentVariableMap = new HashMap<>();
        buildEnvironmentVariableMap.put("ECR_REPOSITORY_URI", variable);

        //Custom role - grant read access from CodeBuild to ECR (Attach policy: AmazonEC2ContainerRegistryPowerUser)
        IRole roleForCodeBuildReadAccessToEcr = Role.fromRoleArn(this, "aCodeBuildEcrReadOnlyRole",
                "arn:aws:iam::424151071692:role/MyCodeBuildEcrReadOnlyRole");

        //CodeCommit repository
        IRepository codeRepository = Repository.fromRepositoryName(this, "ImportedRepo", CODECOMMIT_REPO_NAME);

        //CodeBuild project with a custom buildSpec YAML file passed to it along with required ECR repository URI
        //environment variable
        PipelineProject masterReleaseBuild = PipelineProject.Builder.create(this, "masterReleaseBuild")
                .buildSpec(BuildSpec.fromObject(masterReleaseBuildSpecificationMap))
                .environment(BuildEnvironment.builder().buildImage(LinuxBuildImage.AMAZON_LINUX_2_3).build())
                .environmentVariables(buildEnvironmentVariableMap)
                .role(roleForCodeBuildReadAccessToEcr)
                .build();

        Artifact sourceOutput = new Artifact();
        Artifact masterReleaseBuildOutput = new Artifact("masterReleaseBuildOutput");

        Pipeline.Builder.create(this, "masterReleasePipeline")
                .stages(Arrays.asList(
                        prepareSourceStage(codeRepository, sourceOutput),
                        prepareBuildStage(masterReleaseBuild, sourceOutput, masterReleaseBuildOutput)))
                .build();
    }

    private StageProps prepareSourceStage(final IRepository code, final Artifact sourceOutput) {
        return StageProps.builder()
                .stageName("source")
                .actions(Collections.singletonList(
                        CodeCommitSourceAction.Builder.create()
                                .actionName("source")
                                .repository(code)
                                .output(sourceOutput)
                                .build()))
                .build();
    }

    private StageProps prepareBuildStage(final PipelineProject masterReleaseBuildProject,
                                         final Artifact sourceOutput,
                                         final Artifact masterReleaseBuildProjectOutput) {
        return StageProps.builder()
                .stageName("build")
                .actions(Collections.singletonList(
                        CodeBuildAction.Builder.create()
                                .actionName("build")
                                .project(masterReleaseBuildProject)
                                .input(sourceOutput)
                                .outputs(Collections.singletonList(masterReleaseBuildProjectOutput))
                                .build()))
                .build();
    }

/*
    private StageProps prepareDeployStage(final Artifact masterReleaseBuildProjectOutput) {
        return StageProps.builder()
                .stageName("deploy")
                .actions(Collections.singletonList(
                        EcsDeployAction.Builder.create()
                                .actionName("deploy")
                                .input(masterReleaseBuildProjectOutput)
                                .imageFile(masterReleaseBuildProjectOutput.atPath("imageDefinitions.json"))
                                .build()))
                .build();
    }
*/
}
