import json
import datetime
import boto3

client = boto3.client('codecommit')


def handler(event, context):
    print('Comment Result')
    print(event)
    pullRequestId = event['detail']['project-name']
    status = event['detail']['build-status']
    environment_variables = event['detail']['additional-information']['environment']['environment-variables']
    client.post_comment_for_pull_request(
        pullRequestId=pullRequestId,
        repositoryName=environment_variables[0]['value'],
        beforeCommitId=environment_variables[2]['value'],
        afterCommitId=environment_variables[3]['value'],
        content=status
    )
