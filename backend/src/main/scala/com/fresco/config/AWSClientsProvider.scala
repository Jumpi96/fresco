package com.fresco.config

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDB, AmazonDynamoDBClientBuilder}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.typesafe.config.Config

object AWSClientsProvider {
  def createDynamoDBClient(awsConfig: Config): AmazonDynamoDB =
    AmazonDynamoDBClientBuilder.standard()
      .withCredentials(new AWSStaticCredentialsProvider(
        new BasicAWSCredentials(awsConfig.getString("accessKey"), awsConfig.getString("secretKey"))))
      .build()

  def createS3PresignedUrlClient(awsConfig: Config): AmazonS3 =
    AmazonS3ClientBuilder.standard()
      .withCredentials(new AWSStaticCredentialsProvider(
        new BasicAWSCredentials(awsConfig.getString("accessKey"), awsConfig.getString("secretKey"))))
      .build()

}
