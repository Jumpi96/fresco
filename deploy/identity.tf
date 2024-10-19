
# Create IAM User for crawler
resource "aws_iam_user" "crawler_user" {
  name = "crawler_user"
}

# Attach policies for S3 and DynamoDB access to the crawler user
resource "aws_iam_user_policy" "crawler_user_policy" {
  name = "crawler_user_policy"
  user = aws_iam_user.crawler_user.name

  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Action = [
          "s3:PutObject",
          "s3:GetObject",
          "s3:DeleteObject",
          "s3:ListBucket"
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

# Create IAM User for backend
resource "aws_iam_user" "backend_user" {
  name = "backend_user"
}

# Attach policies for read-only S3 and DynamoDB access to the backend user
resource "aws_iam_user_policy" "backend_user_policy" {
  name = "backend_user_policy"
  user = aws_iam_user.backend_user.name

  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Action = [
          "s3:GetObject",
          "s3:ListBucket"
        ],
        Effect   = "Allow",
        Resource = "${aws_s3_bucket.recipe_storage.arn}/*"
      },
      {
        Action = [
          "dynamodb:GetItem",
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

# Create access keys for the backend user
resource "aws_iam_access_key" "backend_user_access_key" {
  user = aws_iam_user.backend_user.name
}

# Create access keys for the user to be used in your script
resource "aws_iam_access_key" "crawler_user_access_key" {
  user = aws_iam_user.crawler_user.name
}

# Output the access key and secret (important to manage this securely)
output "crawler_user_access_secret" {
  value = aws_iam_access_key.crawler_user_access_key.secret
  sensitive = true
}

output "backend_user_access_secret" {
  value = aws_iam_access_key.backend_user_access_key.secret
  sensitive = true
}
