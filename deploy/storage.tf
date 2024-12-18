# DynamoDB table for storing recipes
resource "aws_dynamodb_table" "recipes" {
  name         = "fresco-recipes"
  billing_mode = "PAY_PER_REQUEST"

  attribute {
    name = "id"
    type = "S"
  }

  attribute {
    name = "indexNumber"
    type = "N"
  }

  hash_key = "id"

  global_secondary_index {
    name               = "IndexNumberIndex"
    hash_key           = "indexNumber"
    projection_type    = "ALL"
  }

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

# DynamoDB table for storing favourites per user
resource "aws_dynamodb_table" "favourites" {
  name         = "fresco-favourites"
  billing_mode = "PAY_PER_REQUEST"

  attribute {
    name = "userId"
    type = "S"  # String type
  }

  attribute {
    name = "recipeId"
    type = "S"  # String type
  }

  hash_key  = "userId"   # Partition key
  range_key = "recipeId"  # Sort key

  stream_enabled   = false
  point_in_time_recovery {
    enabled = true
  }

  tags = {
    Name        = "Fresco Favourites Table"
    Environment = "Production"
  }
}

# DynamoDB table for storing shopping cart state
resource "aws_dynamodb_table" "shopping_cart" {
  name         = "fresco-shopping-cart"
  billing_mode = "PAY_PER_REQUEST"

  attribute {
    name = "userId"
    type = "S"  # String type
  }

  hash_key = "userId"  # Partition key

  stream_enabled   = false
  point_in_time_recovery {
    enabled = true
  }

  tags = {
    Name        = "Fresco Shopping Cart Table"
    Environment = "Production"
  }
}

output "recipes_table_name" {
  value = aws_dynamodb_table.recipes.name
}

output "ingredients_table_name" {
  value = aws_dynamodb_table.ingredients.name
}

output "favourites_table_name" {
  value = aws_dynamodb_table.favourites.name
}

output "shopping_cart_table_name" {
  value = aws_dynamodb_table.shopping_cart.name
}
