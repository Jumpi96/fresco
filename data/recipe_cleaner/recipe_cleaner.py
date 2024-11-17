import pandas as pd
import boto3
from botocore.exceptions import ClientError

# Load IDs to remove from CSV
def load_ids_to_remove(file_path):
    df = pd.read_csv(file_path)
    return df['id'].tolist()

# Remove recipes from DynamoDB
def remove_recipes_from_dynamodb(table_name, ids_to_remove):
    dynamodb = boto3.resource('dynamodb')
    table = dynamodb.Table(table_name)

    for recipe_id in ids_to_remove:
        try:
            response = table.delete_item(
                Key={
                    'id': recipe_id
                }
            )
            print(f"Deleted recipe with ID: {recipe_id}")
        except ClientError as e:
            print(f"Error deleting recipe with ID {recipe_id}: {e.response['Error']['Message']}")

def main():
    ids_file_path = 'output/ids_to_remove.csv'  # Path to the CSV file
    table_name = 'fresco-recipes'  # DynamoDB table name

    # Load IDs to remove
    ids_to_remove = load_ids_to_remove(ids_file_path)

    # Remove recipes from DynamoDB
    remove_recipes_from_dynamodb(table_name, ids_to_remove)

if __name__ == "__main__":
    main()
