# Cognito User Pool
resource "aws_cognito_user_pool" "fresco_pool" {
  name = "fresco-user-pool"

  password_policy {
    minimum_length    = 8
    require_lowercase = true
    require_numbers   = true
    require_symbols   = true
    require_uppercase = true
  }

  auto_verified_attributes = ["email"]
}

# Cognito User Pool Client
resource "aws_cognito_user_pool_client" "fresco_client" {
  name         = "fresco-client"
  user_pool_id = aws_cognito_user_pool.fresco_pool.id

  generate_secret     = false
  explicit_auth_flows = ["ALLOW_USER_PASSWORD_AUTH", "ALLOW_REFRESH_TOKEN_AUTH"]
}

# Output the User Pool ID and Client ID
output "cognito_user_pool_id" {
  value = aws_cognito_user_pool.fresco_pool.id
}

output "cognito_client_id" {
  value = aws_cognito_user_pool_client.fresco_client.id
}

