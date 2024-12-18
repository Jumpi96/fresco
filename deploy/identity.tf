
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

# Create IAM User for DEV backend
resource "aws_iam_user" "backend_user" {
  name = "backend_user"
}

# Create IAM policy for read-only S3 and DynamoDB access
resource "aws_iam_policy" "backend_access_policy" {
  name        = "backend-access-policy"
  path        = "/"
  description = "IAM policy for backend access to S3 and DynamoDB"

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
          "${aws_dynamodb_table.ingredients.arn}",
          "${aws_dynamodb_table.favourites.arn}",
          "${aws_dynamodb_table.shopping_cart.arn}",
        ]
      },
      {
        Action = [
          "dynamodb:PutItem",
          "dynamodb:DeleteItem",
        ],
        Effect   = "Allow",
        Resource = [
          "${aws_dynamodb_table.favourites.arn}",
          "${aws_dynamodb_table.shopping_cart.arn}",
        ]
      },
      {
        Action = [
          "dynamodb:DescribeTable"
        ],
        Effect   = "Allow",
        Resource = [
          "${aws_dynamodb_table.recipes.arn}"
        ]
      },
      {
        Action = [
          "dynamodb:Query"  # Add permission for querying the GSI
        ],
        Effect   = "Allow",
        Resource = "${aws_dynamodb_table.recipes.arn}/index/IndexNumberIndex"  # Specify the GSI ARN
      }
    ]
  })
}

# Attach the policy to the backend user
resource "aws_iam_user_policy_attachment" "backend_user_policy_attachment" {
  user       = aws_iam_user.backend_user.name
  policy_arn = aws_iam_policy.backend_access_policy.arn
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

resource "aws_iam_role" "eb_instance_role" {
  name = "fresco-backend-eb-instance-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "eb_instance_role_s3_dynamodb_policy" {
  policy_arn = aws_iam_policy.backend_access_policy.arn
  role       = aws_iam_role.eb_instance_role.name
}

resource "aws_iam_role_policy_attachment" "eb_instance_role_web_tier_policy" {
  policy_arn = "arn:aws:iam::aws:policy/AWSElasticBeanstalkWebTier"
  role       = aws_iam_role.eb_instance_role.name
}

resource "aws_iam_role_policy_attachment" "eb_instance_role_worker_tier_policy" {
  policy_arn = "arn:aws:iam::aws:policy/AWSElasticBeanstalkWorkerTier"
  role       = aws_iam_role.eb_instance_role.name
}

resource "aws_iam_role_policy_attachment" "eb_instance_role_multicontainer_docker_policy" {
  policy_arn = "arn:aws:iam::aws:policy/AWSElasticBeanstalkMulticontainerDocker"
  role       = aws_iam_role.eb_instance_role.name
}

resource "aws_iam_role" "eb_service_role" {
  name = "fresco-backend-eb-service-role"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = ""
        Effect = "Allow"
        Principal = {
          Service = "elasticbeanstalk.amazonaws.com"
        }
        Action = "sts:AssumeRole"
        Condition = {
          StringEquals = {
            "sts:ExternalId" = "elasticbeanstalk"
          }
        }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "eb_service_role_enhanced_health_policy" {
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSElasticBeanstalkEnhancedHealth"
  role       = aws_iam_role.eb_service_role.name
}

resource "aws_iam_role_policy_attachment" "eb_service_role_managed_updates_policy" {
  policy_arn = "arn:aws:iam::aws:policy/AWSElasticBeanstalkManagedUpdatesCustomerRolePolicy"
  role       = aws_iam_role.eb_service_role.name
}
