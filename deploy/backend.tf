resource "aws_elastic_beanstalk_application" "fresco_backend" {
  name        = "fresco-backend"
  description = "Fresco Backend Application"
}

resource "aws_elastic_beanstalk_environment" "fresco_backend_env" {
  name                = "fresco-backend-env"
  application         = aws_elastic_beanstalk_application.fresco_backend.name
  solution_stack_name = "64bit Amazon Linux 2 v3.7.7 running Corretto 11"

  setting {
    namespace = "aws:elasticbeanstalk:environment"
    name      = "ServiceRole"
    value     = aws_iam_role.eb_service_role.name
  }

  setting {
    namespace = "aws:elasticbeanstalk:environment"
    name      = "EnvironmentType"
    value     = "SingleInstance"
  }

  setting {
    namespace = "aws:elasticbeanstalk:application:environment"
    name      = "FRESCO_ENV"
    value     = "prod"
  }

  setting {
    namespace = "aws:elasticbeanstalk:environment"
    name      = "LoadBalancerType"
    value     = "application"
  }

  setting {
    namespace = "aws:elbv2:listener:443"
    name      = "Protocol"
    value     = "HTTPS"
  }

  setting {
    namespace = "aws:elbv2:listener:443"
    name      = "SSLCertificateArns"
    value     = data.aws_acm_certificate.jplorenzo_cert.arn
  }

  # On October 2024, AWS Elastic Beanstalk deprecated the ability to use launch templates.
  # Because of this, this resource couldn't be created fully with Terraform.
  # This lifecycle rule ignores all changes to this resource that was imported manually.
  lifecycle {
    ignore_changes = all
  }
}

resource "aws_iam_instance_profile" "eb_instance_profile" {
  name = "fresco-backend-eb-instance-profile"
  role = aws_iam_role.eb_instance_role.name
}

# Data source to get the Elastic Beanstalk hosted zone ID
data "aws_elastic_beanstalk_hosted_zone" "current" {}

output "elastic_beanstalk_url" {
  value = aws_elastic_beanstalk_environment.fresco_backend_env.cname
}

output "elastic_beanstalk_environment_name" {
  value = aws_elastic_beanstalk_environment.fresco_backend_env.name
}
