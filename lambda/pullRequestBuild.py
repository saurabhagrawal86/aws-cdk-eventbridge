import json
import datetime
import boto3

client = boto3.client('codebuild')


def handler(event, context):
    pull_request_id = event['detail']['pullRequestId']
    repository_name = event['resources'][0]
    source_commit = event['detail']['sourceCommit']
    destination_commit = event['detail']['destinationCommit']
    print('Cool')
    print(event)
    response = client.create_project(
        name=pull_request_id,
        description='string',
        source={
            'type': 'CODECOMMIT',
            'location': event['resources'][0].split(sep=":")[5]
        },
        sourceVersion=event['detail']['sourceReference'],
        artifacts={
            'type': 'NO_ARTIFACTS'
        },
        environment={
            'type': 'LINUX_CONTAINER',
            'image': 'aws/codebuild/standard:latest',
            'computeType': 'BUILD_GENERAL1_SMALL',
            'imagePullCredentialsType': 'CODEBUILD',
            'environmentVariables': [
                {
                    'name': 'repository_name',
                    'value': event['resources'][0].split(sep=":")[5],
                    'type': 'PLAINTEXT'
                },
                {
                    'name': 'pull_request_id ',
                    'value': pull_request_id,
                    'type': 'PLAINTEXT'
                },
                {
                    'name': 'before_commit_id ',
                    'value': source_commit,
                    'type': 'PLAINTEXT'
                }
                ,
                {
                    'name': 'after_commit_id ',
                    'value': destination_commit,
                    'type': 'PLAINTEXT'
                }
            ]
        },
        serviceRole='arn:aws:iam::902487264658:role/CodeBUILDROLE',
        badgeEnabled=True
    )
    client.start_build(
        projectName=pull_request_id
    )
    return response
