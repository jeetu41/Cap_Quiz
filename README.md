# AI-Powered Quiz Application

This is a web-based quiz application that generates questions using Google's Gemini AI. The application allows users to select subjects, topics, and difficulty levels to generate custom quiz questions.

## Features

- Generate quiz questions on various subjects (OS, CN, DSA, Cloud, etc.)
- Multiple difficulty levels (Easy, Medium, Hard)
- Real-time answer validation
- Clean and responsive UI
- Powered by Google's Gemini AI
- Ready for deployment on Render

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Google Cloud Account with Gemini API access
- Docker (for local development and testing)

## Local Development

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd ai-quiz-app
   ```

2. **Set up Google Cloud Credentials**
   - Go to [Google AI Studio](https://makersuite.google.com/)
   - Create an API key
   - Set the API key as an environment variable:
     ```bash
     # On Windows
     set GEMINI_API_KEY=your-api-key-here
     
     # On macOS/Linux
     export GEMINI_API_KEY=your-api-key-here
     ```

3. **Run with Maven**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

4. **Or run with Docker**
   ```bash
   docker build -t ai-quiz-app .
   docker run -p 8080:8080 -e GEMINI_API_KEY=your-api-key-here ai-quiz-app
   ```

5. **Access the application**
   Open your browser and navigate to: http://localhost:8080/quiz/

## Deployment to Render

### Prerequisites
- A Render.com account
- GitHub/GitLab/Bitbucket repository with the application code

### Deployment Steps

1. **Create a new Web Service on Render**
   - Log in to your Render dashboard
   - Click "New" and select "Web Service"
   - Connect your repository where the code is hosted

2. **Configure the Web Service**
   - **Name**: ai-quiz-app (or your preferred name)
   - **Region**: Select the region closest to your users
   - **Branch**: main (or your preferred branch)
   - **Runtime**: Docker
   - **Build Command**: (leave empty, we're using a Dockerfile)
   - **Start Command**: (leave empty, defined in Dockerfile)

3. **Set Environment Variables**
   - `GEMINI_API_KEY`: Your Google Gemini API key
   - `SPRING_PROFILES_ACTIVE`: production

4. **Deploy**
   - Click "Create Web Service"
   - Render will automatically build and deploy your application
   - The deployment will be available at: `https://your-app-name.onrender.com`

## Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `GEMINI_API_KEY` | Yes | Your Google Gemini API key |
| `SPRING_PROFILES_ACTIVE` | No | Set to 'production' for production environment |
| `PORT` | No | Port the application will run on (automatically set by Render) |

## Project Structure

```
.
├── src/                          # Source code
│   ├── main/
│   │   ├── java/                # Java source files
│   │   └── resources/           # Configuration and static resources
├── .dockerignore                # Docker ignore file
├── Dockerfile                   # Docker configuration
├── pom.xml                      # Maven configuration
├── README.md                    # This file
└── render.yaml                  # Render deployment configuration
```

## Configuration

Application properties can be configured in `src/main/resources/application.properties` or overridden using environment variables.

## License

This project is licensed under the MIT License - see the LICENSE file for details.
