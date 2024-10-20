package com.fresco.config

import com.amazonaws.auth._
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDB, AmazonDynamoDBClientBuilder}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.typesafe.config.Config

object AWSClientsProvider {
  private def getCredentialsProvider(awsConfig: Config): AWSCredentialsProvider = {
    if (awsConfig.hasPath("accessKey") && awsConfig.hasPath("secretKey")) {
      new AWSStaticCredentialsProvider(
        new BasicAWSCredentials(awsConfig.getString("accessKey"), awsConfig.getString("secretKey"))
      )
    } else {
      new DefaultAWSCredentialsProviderChain()
    }
  }

  def createDynamoDBClient(awsConfig: Config): AmazonDynamoDB =
    AmazonDynamoDBClientBuilder.standard()
      .withCredentials(getCredentialsProvider(awsConfig))
      .build()

  def createS3PresignedUrlClient(awsConfig: Config): AmazonS3 =
    AmazonS3ClientBuilder.standard()
      .withCredentials(getCredentialsProvider(awsConfig))
      .build()
}
