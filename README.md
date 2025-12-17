# Probenmanagement Software

This repository contains the code and documentation for the project "Probenmanagement Software" as part of the Advanced Software Lab of the University of Marburg. The project aims to develop a comprehensive sample management system.

## Table of Contents

- [Introduction](#Introduction)
- [Features](#Featrues)
- [Installation](#Installation)
- [Usage](#Usage)
- [Imprint](#Imprint)
- [License](#License)

## Introduction

The Probenmanagement Software project is designed to facilitate efficient and effective management of samples in a laboratory setting. It includes functionalities for tracking metadata of certain studies, managing sample data, and creation of a comprehensive report at the end of the workflow..

## Features

- **Creating and managing studies**: Keep track of all the relevcant studies, including their metadata such as Start Date, End Date, Expected number of Subjects, Number of Sample Deliveries, the transmitter of the samples, as well as the sponsor of the study
- **Sample Delivery**: This feature lets the user upload an EXCEL-sheet including the data of the samples
- **Verify Sample Delivery**: Ensure that the uploaded data matches the expected one by comparing the barcodes of each sample
- **Manual Sample Delivery**: This feature allows the user to manually input the details of each sample if the EXCEL-sheet is not available.
- **Add Sample Analysis to Study**: Add a relevant analysis to the study if it is to be used for a sample in it
- **Add Analysis to Sample**: Add the analysis to the sample(s) of choice.
- **Create Workplace List**: Create a corresponding “WorkplaceList” which includes all the analyses and samples selected by the user
- **Enter Sample Analysis**: After the analysis is done, the results can be entered manually by the user
- **View Sample Analysis**: View the entered analysis results
- **Generate Report**: Automatically generate a comprehensive report including all sample data and analysis results
- **Read Results**: This feature lets the user upload an EXCEL-sheet that already includes all the relevant analysis results, in order to streamline the process of entering the analysis data

## Installation

The Usage of an IDE, such as IntelliJ IDEA is recommended when working with the code and repository

1. Clone the repository:
    
    ```bash
    git clone https://github.com/Bela4321/Probenmanagement_FoPra.git
    ```
    
2. The usage of the program requires a PostgreSQL database called sample_management. Using a tool like pgAdmin create a database and give it the name sample_management
3. Since the project uses Maven, after opening the project in your IDE, a notification should advise you to install all missing dependencies. If not, run 

```bash
mvn clean install 
```

4. Set the environment variables of the run configuration.

```bash
DB_USER=postgres;DB_PASSWORD=<your Database password>
```

---

## Usage

To start using the sample management system, follow these steps:

1. Start the application by running the SampleManagementApplication.Java
2. Open your web browser and navigate to:
    
    ```
    http://localhost:8080
    ```
    
3. Follow the on-screen instructions to begin managing your samples.

## Testing

This project contains a growing suite of automated tests to ensure code quality and prevent regressions. The tests are structured in multiple layers to cover different aspects of the application.

### How to Run Tests

To run the entire test suite, use the following Maven command from the `sample-management` directory:

```bash
mvnw.cmd test
```
(or `./mvnw test` on Linux/macOS)

### Test Environment

The tests run in a dedicated test environment with the following configuration:
-   **Database:** An in-memory H2 database is used to ensure that tests are fast and do not affect the real database.
-   **Test Properties:** Test-specific properties are defined in `src/test/resources/application.properties`. This includes dummy API keys and feature flags to control the application's behavior during tests.
-   **Database Migrations:** The automatic database schema migration with Flyway is disabled in the test environment to allow for a clean database for each test run. This is controlled by the `app.flyway.enabled=false` property and the `@ConditionalOnProperty` annotation on the `FlywayInitializer` class.

### Testing Strategy & Progress

The test suite is being built incrementally in the following phases:

**Phase 1: Foundational Unit & Integration Tests (Completed)**

This phase focused on setting up the test environment and creating a foundational set of tests for the core backend components.

**Components Tested:**
-   **Unit Tests:**
    -   `SampleDelivery`: Verifies the business logic for calculating the running number of a delivery.
    -   `ExcelTemplateFiller`: Verifies the content and structure of the generated Excel workplace lists.
-   **Integration Tests:**
    -   `StudyRepository`: Verifies creating, reading, and updating studies in the database.
    -   `SampleRepository`: Verifies creating, reading, and updating samples and their relationships.
    -   `AnalysisRepository`: Verifies creating and reading analyses and their relationships.
    -   `StudyService`: Verifies the business logic for managing analysis types within a study.

**Phase 2: Advanced Backend & Report Testing (In Progress)**

This phase focuses on expanding the test coverage to more complex backend features and the PDF report generation.

**Completed Activities:**
-   **Refactored PDF Report Generation:** The PDF generation logic was moved from the `CreateStudyReport` UI component into a new, dedicated `PdfReportService`. This improves the application's architecture and makes the logic easier to test.
-   **Added PDF Service Test:** A unit test for the `PdfReportService` was created to ensure the PDF generation process works correctly.
-   **Expanded Integration Tests:** The integration tests for `StudyService` were expanded to cover more complex business logic, such as the filtering of studies.

**Next Steps:**
-   Continue to expand integration tests to cover more edge cases and complex scenarios for the existing components.

**Phase 3: UI and End-to-End Testing (Planned)**

This phase will focus on testing the application's UI and the complete user workflows from end to end.

**Planned Activities:**
-   Set up a UI testing framework (e.g., Vaadin TestBench).
-   Write E2E tests for critical user flows, such as creating a study, adding samples, and generating reports through the UI.


## Imprint
Institut für Virologie Marburg<br />
Immunmonitoring Labor<br />
Hans-Meerwein-Straße 2<br />
35043 Marburg<br />

Institutsleitung: Prof. Dr. Stephan Becker<br />
Laborleitung: Dr. Verena Krähling<br />

Kontakt:
Internet: https://www.uni-marburg.de/de/fb20/bereiche/ziei/virologie<br />
Telefon: ++49 (0)6421 2865158<br />
Email: immunmonitoring.labor@uni-marburg.de<br />

Gestaltung & technische Realisierung:
Bela Schinke: bela.schinke@gmail.com<br />
David Meyer:<br />
David Riemer: david.riemer07@gmail.com<br />
Edbert Faustine:<br />
Sayedfarhad Emami Dehcheshmeh:<br />
Mohsen Saleki:<br />

## License

This project is licensed under the MIT License. See the [LICENSE](https://github.com/Bela4321/Probenmanagement_FoPra/blob/main/LICENSE) file for more details.