package com.fresco.domain.services

import com.amazonaws.HttpMethod
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest
import java.net.URL
import scala.concurrent.{ExecutionContext, Future}
import java.util.Date


class S3Service(s3Client: AmazonS3, bucketName: String)(implicit ec: ExecutionContext) {

  // Generate a pre-signed URL for a given S3 path
  def generatePresignedUrl(s3Path: String): Future[URL] = Future {
    val expiration = new Date(System.currentTimeMillis() + 60 * 60 * 1000)


    val key = s3Path.replaceAll("s3://[^/]+/", "")
    val generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, key)
      .withMethod(HttpMethod.GET)
      .withExpiration(expiration)

    // Generate the pre-signed URL
    s3Client.generatePresignedUrl(generatePresignedUrlRequest)
  }
}
