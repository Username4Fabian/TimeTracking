# Motion Highlighter

Motion Highlighter is a sophisticated web application that provides a live video feed with advanced motion detection capabilities. It also includes a scoreboard feature to track and display results.

## Table of Contents

1. [Features](#features)
2. [Technologies Used](#technologies-used)
3. [Setup and Installation](#setup-and-installation)
4. [Usage](#usage)
5. [Contributing](#contributing)
6. [License](#license)

## Features

- **Live video feed**: Provides real-time video streaming. The video feed is displayed in a user-friendly interface, allowing users to monitor the feed in real time.

- **Motion detection with adjustable sensitivity**: Detects motion in the video feed with customizable sensitivity settings. This feature uses advanced algorithms to accurately detect motion, and the sensitivity can be adjusted to suit the user's needs.

- **Autoscroll option**: Automatically scrolls through the video feed. This feature is useful for monitoring long video feeds, as it automatically scrolls through the feed, saving the user the trouble of manually scrolling.

- **Scoreboard to track and display results**: Tracks and displays the results in a user-friendly scoreboard. The scoreboard is updated in real time, allowing users to easily track the results.

- **Delete all videos functionality**: Allows users to delete all videos with a single click. This feature is useful for managing storage space, as it allows users to easily delete all videos when they are no longer needed.

## Technologies Used

- [Java](https://www.java.com/): Used for backend development. Java is a popular and versatile programming language known for its robustness and ease of use.

- [JavaScript](https://www.javascript.com/): Used for frontend development. JavaScript is a dynamic programming language that allows for interactive web pages.

- [Spring Boot](https://spring.io/projects/spring-boot): Used to create stand-alone, production-grade Spring based applications. Spring Boot simplifies the setup and development of Spring applications.

- [Maven](https://maven.apache.org/): Used for building and managing the project. Maven is a powerful project management tool that provides developers with a comprehensive understanding of their project's state.

## Setup and Installation

1. Clone the repository to your local machine using `git clone <repository_link>`.
2. Navigate to the project directory using `cd <project_directory>`.
3. Create H2 Database and add the hostname to the `application.properties` file.
4. Run `mvn clean install` to build the project.
5. Start the application with `mvn spring-boot:run`.

## Usage

Open your web browser and navigate to `localhost:8080` to access the application. The application provides a user-friendly interface for easy navigation. The live video feed can be accessed from the main page, and the scoreboard can be accessed by clicking on the "Open Scoreboard" button.

## Contributing

We welcome contributions from the community. Please follow the steps below to contribute:

1. Fork the repository.
2. Create a new branch for your changes.
3. Make the changes in your branch.
4. Submit a pull request.

Before making a major change, please open an issue first to discuss what you would like to change. Please ensure that your code adheres to our coding standards and that all tests pass before submitting a pull request.

## License

This project is licensed under the [MIT License](https://choosealicense.com/licenses/mit/). This means that you are free to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the software, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
