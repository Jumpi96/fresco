provider "aws" {
  region = "eu-central-1"  # Adjust to your desired AWS region
}

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

# DynamoDB table for storing recipes
resource "aws_dynamodb_table" "recipes" {
  name         = "fresco-recipes"
  billing_mode = "PAY_PER_REQUEST"

  attribute {
    name = "id"
    type = "S"  # String type
  }

  hash_key = "id"

  stream_enabled   = false
  point_in_time_recovery {
    enabled = true
  }

  tags = {
    Name        = "Fresco - Recipes Table"
    Environment = "Production"
  }
}

# DynamoDB table for storing ingredients
resource "aws_dynamodb_table" "ingredients" {
  name         = "fresco-ingredients"
  billing_mode = "PAY_PER_REQUEST"

  attribute {
    name = "id"
    type = "S"  # String type
  }

  hash_key = "id"

  stream_enabled   = false
  point_in_time_recovery {
    enabled = true
  }

  tags = {
    Name        = "Fresco Ingredients Table"
    Environment = "Production"
  }
}

# Create IAM User for script
resource "aws_iam_user" "script_user" {
  name = "script_user"
}

# Attach policies for S3 and DynamoDB access to the user
resource "aws_iam_user_policy" "script_user_policy" {
  name = "script_user_policy"
  user = aws_iam_user.script_user.name

  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Action = [
          "s3:PutObject",
          "s3:GetObject",
          "s3:DeleteObject"
        ],
        Effect   = "Allow",
        Resource = "${aws_s3_bucket.recipe_storage.arn}/*"
      },
      {
        Action = [
          "dynamodb:PutItem",
          "dynamodb:GetItem",
          "dynamodb:UpdateItem",
          "dynamodb:DeleteItem",
          "dynamodb:Scan",
          "dynamodb:Query"
        ],
        Effect   = "Allow",
        Resource = [
          "${aws_dynamodb_table.recipes.arn}",
          "${aws_dynamodb_table.ingredients.arn}"
        ]
      }
    ]
  })
}

# Create access keys for the user to be used in your script
resource "aws_iam_access_key" "script_user_access_key" {
  user = aws_iam_user.script_user.name
}

# Output the access key and secret (important to manage this securely)
output "script_user_access_key_id" {
  value = aws_iam_access_key.script_user_access_key.id
}

output "script_user_access_secret" {
  value = aws_iam_access_key.script_user_access_key.secret
  sensitive = true
}
