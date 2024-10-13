package com.fresco.config

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDB, AmazonDynamoDBClientBuilder}
import com.typesafe.config.Config

object DynamoDBClientProvider {
  def createDynamoDBClient(awsConfig: Config): AmazonDynamoDB =
    AmazonDynamoDBClientBuilder.standard()
      .withCredentials(new AWSStaticCredentialsProvider(
        new BasicAWSCredentials(awsConfig.getString("accessKey"), awsConfig.getString("secretKey"))))
      .build()
}
