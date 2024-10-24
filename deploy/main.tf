provider "aws" {
  region = "eu-central-1"
}

provider "aws" {
  alias  = "us_east_1"
  region = "us-east-1"
}

terraform {
  backend "s3" {
    bucket = "fresco-terraform-state-bucket"
    key    = "terraform.tfstate"
    region = "eu-central-1"
    encrypt = true
  }
}
