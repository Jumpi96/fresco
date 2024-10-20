resource "aws_elastic_beanstalk_application" "fresco_backend" {
  name        = "fresco-backend"
  description = "Fresco Backend Application"
}

resource "aws_elastic_beanstalk_environment" "fresco_backend_env" {
  name                = "fresco-backend-env"
  application         = aws_elastic_beanstalk_application.fresco_backend.name
  solution_stack_name = "64bit Amazon Linux 2 v3.7.7 running Corretto 11"

  setting {
    namespace = "aws:autoscaling:launchconfiguration"
    name      = "IamInstanceProfile"
    value     = aws_iam_instance_profile.eb_instance_profile.name
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
}

resource "aws_iam_instance_profile" "eb_instance_profile" {
  name = "fresco-backend-eb-instance-profile"
  role = aws_iam_role.eb_instance_role.name
}

output "elastic_beanstalk_url" {
  value = aws_elastic_beanstalk_environment.fresco_backend_env.endpoint_url
}
