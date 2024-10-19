terraform {
  backend "s3" {
    bucket = "fresco-terraform-storage-bucket"
    key    = "terraform.tfstate"
    region = "eu-central-1"
    encrypt = true
  }
}
