# Crypto Trading Strategy Analyzer

## Description

Crypto Trading Strategy Analyzer is a project designed to assist analysts and traders in evaluating various trading strategies across different cryptocurrency pairs. The project provides the capability to automate trading using the most effective strategies identified during the analysis. The application integrates advanced analysis tools and automation features, making it a valuable utility for both novice and professional cryptocurrency traders.

---

## Features

- Analyze trading strategies for multiple crypto pairs.
- Identify top-performing strategies based on key performance metrics.
- Automate trading processes with the best-rated strategies.
- Built with a focus on scalability and ease of use.

---

## Prerequisites

Make sure the following tools are pre-installed on your system before running the project:

- **Docker** and **Docker Compose**

--- 

## Installation and Startup

To start and run the project using Docker Compose, follow these instructions:

1. Clone the project repository to your local machine:

   ```sh
   git clone <repository-url>
   cd <project-folder>
   ```

2. Use the following command to start the application in your Docker environment:

   ```sh
   docker-compose -f docker-local.yml up
   ```

3. If needed, stop the application with:

   ```sh
   docker-compose -f docker-local.yml down
   ```

4. Access the application URL from your web browser or API client. Specific details (such as ports) can be found in the `docker-local.yml` file.

---

## Notes

- Ensure that the `docker-local.yml` file is configured correctly to match your system's setup and dependencies before starting the application.
- Additional custom configurations can be added to the `docker-local.yml` file if required for specialized environments.

---

## License

This project is licensed under the [MIT License](LICENSE). 
