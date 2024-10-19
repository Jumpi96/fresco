export function calculateKcal(protein, carbs, fat) {
    return Math.round((protein * 4) + (carbs * 4) + (fat * 9));
  }