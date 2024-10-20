# fresco

Fresco is a modern web application for exploring and cooking delicious recipes. It features a Vue.js frontend and a Scala backend, providing a seamless experience for users to discover, view, and prepare various dishes.

## 🚀 Features

- Browse a wide variety of recipes
- View detailed recipe information, including ingredients, preparation steps, and nutritional facts
- Responsive design for both desktop and mobile devices
- Efficient data loading with pagination support

## 🛠️ Tech Stack

### Frontend
- Vue.js 3
- Vuex for state management
- Vue Router for navigation
- Axios for API requests
- Vite as the build tool

### Backend
- Scala
- Akka HTTP for the web server
- Amazon DynamoDB for data storage
- Amazon S3 for file storage

## 🚀 Getting Started

### Prerequisites

- Node.js (v14 or later)
- npm (v6 or later)
- Java Development Kit (JDK) 11 or later
- sbt (Scala Build Tool)
- AWS account with DynamoDB and S3 set up

### Frontend Setup

1. Navigate to the frontend directory:
   ```
   cd frontend
   ```

2. Install dependencies:
   ```
   npm install
   ```

3. Start the development server:
   ```
   npm run dev
   ```

The frontend will be available at `http://localhost:5173`.

### Backend Setup

1. Navigate to the backend directory:
   ```
   cd backend
   ```

2. Create a `src/main/resources/application.conf` file with your AWS credentials and other configuration details. Use the `application.conf.test` file as a template.

3. Run the backend server:
   ```
   sbt run
   ```

The backend API will be available at `http://localhost:8080`.

## 🧪 Running Tests

### Frontend Tests
```sh
cd frontend
npm run test
```

### Backend Tests
```sh
cd backend
sbt test
```

## 📚 API Documentation

The backend provides the following main endpoints:

- `GET /recipes`: Fetch a list of recipes
- `GET /recipes/:id`: Fetch a specific recipe by ID
- `GET /ingredients`: Fetch a list of ingredients
- `GET /ingredients/:id`: Fetch a specific ingredient by ID

For more detailed API documentation, please refer to the backend code and comments.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is licensed under the MIT License.
