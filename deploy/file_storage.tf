
# Create an S3 bucket to store photos and PDFs
resource "aws_s3_bucket" "recipe_storage" {
  bucket = "fresco-storage-bucket"  # Ensure the bucket name is globally unique

  tags = {
    Name        = "Recipe Storage"
    Environment = "Production"
  }
}

# Enable versioning for the S3 bucket
resource "aws_s3_bucket_versioning" "versioning" {
  bucket = aws_s3_bucket.recipe_storage.id

  versioning_configuration {
    status = "Enabled"
  }
}

# Enable default server-side encryption for the S3 bucket
resource "aws_s3_bucket_server_side_encryption_configuration" "sse" {
  bucket = aws_s3_bucket.recipe_storage.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# Manage the ownership controls for the S3 bucket (this must be a separate resource in newer Terraform versions)
resource "aws_s3_bucket_ownership_controls" "ownership_controls" {
  bucket = aws_s3_bucket.recipe_storage.id

  rule {
    object_ownership = "BucketOwnerPreferred"
  }
}

# Optionally create a public access block configuration for security
resource "aws_s3_bucket_public_access_block" "public_access_block" {
  bucket = aws_s3_bucket.recipe_storage.id

  block_public_acls   = true
  block_public_policy = true
  ignore_public_acls  = true
  restrict_public_buckets = true
}

output "fresco_bucket_name" {
  value = aws_s3_bucket.recipe_storage.id
}
