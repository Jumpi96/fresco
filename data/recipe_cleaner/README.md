# Recipe Cleaner

The Recipe Cleaner is a Python-based tool designed to process and clean recipe data. It utilizes clustering algorithms to identify and remove duplicate recipes based on their names. The tool reads recipe data from JSON files, clusters similar recipes, and outputs the IDs of recipes that should be removed.

## Features

- Load recipes from JSON files exported from DynamoDB.
- Use TF-IDF vectorization and DBSCAN clustering to identify similar recipes.
- Generate a CSV file containing the IDs of recipes to be removed.

## Requirements

- Python 3.x
- pandas
- scikit-learn
- numpy

## Installation

1. Clone the repository:

   ```bash
   git clone <repository-url>
   cd recipe_cleaner
   ```

2. Install the required packages:

   ```bash
   pip install -r requirements.txt
   ```

## Usage

1. Place your JSON files containing recipe data in the `data/recipe_cleaner/data` directory.

2. Run the script to find similarity between the recipes:

   ```bash
   python similarity_finder.py
   ```

3. After the script completes, the IDs of the recipes to be removed will be saved in `output/ids_to_remove.csv`.

4. To remove the recipes from the DynamoDB table, run the following script:

   ```bash
   python recipe_cleaner.py
   ```

   Ensure that your AWS credentials are configured properly.

## Logging

The script includes logging to provide insights into the loading and cleaning process. You can adjust the logging level in the script if needed.
