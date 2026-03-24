# WhatToEat

WhatToEat is a Java-based application designed to help users manage their food items. This project allows users to add, retrieve, and delete food entries, making it easier to keep track of dietary choices.

## Features

- Add new food items with details such as name, calories, and type.
- Retrieve a list of all food items.
- Delete food items from the list.
- Filter and search food items based on various criteria.

## Project Structure

```
WhatToEat
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── example
│   │   │           ├── Main.java
│   │   │           ├── models
│   │   │           │   └── Food.java
│   │   │           └── services
│   │   │               └── FoodService.java
│   │   └── resources
│   │       └── application.properties
│   └── test
│       └── java
│           └── com
│               └── example
│                   └── MainTest.java
├── pom.xml
└── README.md
```

## Getting Started

To set up the project, follow these steps:

1. Clone the repository:
   ```
   git clone https://github.com/yourusername/WhatToEat.git
   ```

2. Navigate to the project directory:
   ```
   cd WhatToEat
   ```

3. Build the project using Maven:
   ```
   mvn clean install
   ```

4. Run the application:
   ```
   mvn exec:java -Dexec.mainClass="com.example.Main"
   ```

## Dependencies

This project uses Maven for dependency management. The required dependencies are specified in the `pom.xml` file.

## Contributing

Contributions are welcome! Please open an issue or submit a pull request for any enhancements or bug fixes.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.