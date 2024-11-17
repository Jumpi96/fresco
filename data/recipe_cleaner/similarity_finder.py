import pandas as pd
import json
import glob
import logging
import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
from sklearn.cluster import DBSCAN

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

def load_recipes_from_json(file_path):
    logging.info(f"Loading recipes from {file_path}")
    recipes = []
    ids = []
    with open(file_path, 'r') as file:
        for line in file:
            data = json.loads(line)
            # Extract the recipe name and ID
            recipe_name = data['Item']['name']['S']
            recipe_id = data['Item']['id']['S']
            recipes.append(recipe_name)
            ids.append(recipe_id)
            logging.info(f"Loaded recipe: {recipe_name} with ID: {recipe_id}")
    return recipes, ids

def load_all_recipes(directory):
    all_recipes = []
    all_ids = []
    # Use glob to find all JSON files in the specified directory
    logging.info(f"Searching for JSON files in directory: {directory}")
    for file_path in glob.glob(f"{directory}/*.json"):
        recipes, ids = load_recipes_from_json(file_path)
        all_recipes.extend(recipes)
        all_ids.extend(ids)
    logging.info(f"Total recipes loaded: {len(all_recipes)}")
    return pd.DataFrame({'name': all_recipes, 'id': all_ids})

def clean_recipes(recipes_df, similarity_threshold=0.9, min_samples=2):
    logging.info("Starting the cleaning process for recipes.")
    
    # Vectorize recipe names with TF-IDF
    vectorizer = TfidfVectorizer(stop_words='english')
    X = vectorizer.fit_transform(recipes_df['name'])
    logging.info("TF-IDF vectorization completed.")
    
    # Calculate cosine similarity matrix
    cosine_sim = np.clip(cosine_similarity(X), 0, 1)
    
    # Use DBSCAN clustering on cosine similarity matrix
    dbscan = DBSCAN(eps=(1 - similarity_threshold), min_samples=min_samples, metric='precomputed')
    recipes_df['cluster'] = dbscan.fit_predict(1 - cosine_sim)  # 1 - cosine_sim for dissimilarity
    logging.info("DBSCAN clustering completed.")
    
    # Log recipes in each cluster
    unique_clusters = recipes_df['cluster'].nunique() - (1 if -1 in recipes_df['cluster'].unique() else 0)
    logging.info(f"Identified {unique_clusters} clusters of similar recipes.")

    # Prepare a list to hold IDs to remove
    ids_to_remove = []

    # Group recipes by cluster for review
    for cluster in recipes_df['cluster'].unique():
        if cluster == -1:
            continue  # Skip noise points
        logging.info(f"Cluster {cluster}:")
        cluster_recipes = recipes_df[recipes_df['cluster'] == cluster]
        
        # Keep the first ID and mark the rest for removal
        ids_to_remove.extend(cluster_recipes['id'].tolist()[1:])  # Skip the first ID
        
        for recipe in cluster_recipes['name'].tolist():
            logging.info(f" - {recipe}")
        logging.info("")  # Add a newline for better readability

    # Export the IDs to remove to a CSV file
    output_file = 'output/ids_to_remove.csv'
    pd.DataFrame(ids_to_remove, columns=['id']).to_csv(output_file, index=False)
    logging.info(f"IDs to remove saved to {output_file}")

def main():
    directory = 'data'  # Update with your directory path
    logging.info("Starting the recipe cleaning script.")
    recipes_df = load_all_recipes(directory)
    clean_recipes(recipes_df)
    logging.info("Recipe cleaning process completed.")

if __name__ == "__main__":
    main()
