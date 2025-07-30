# Synthetic App Testing

This project is a backend service for proactive application monitoring. It provides a flexible system to define applications, configure health checks, and schedule them to run at regular intervals. By continuously testing your endpoints and storing the results, you can ensure your applications are available, performant, and reliable.

#### Core Features:
* Define Targets: Register applications to monitor by their URL, IP address, or hostname.
* Configure Checks: Create specific health checks for each target, such as HTTP GET requests, PING tests, or TCP port checks.
* Automated Scheduling: Set custom intervals for each check to run automatically, providing continuous monitoring.
* Persist Results: Every check execution is recorded, capturing success status, response time, and other relevant metrics.
* Historical Analysis: The stored data allows you to query and analyze the historical health and performance trends of your applications.

#### Technologies Used:
* Java 17
* Spring Boot 3
* H2

## How to use
1. Start application `mvn spring-boot:run`
2. Add app targets
```bash
curl --location 'localhost:8080/api/v1/targets' \
--header 'Content-Type: application/json' \
--data '{
    "name": "Google Search",
    "targetUrlOrIp": "https://google.com",
    "type": "URL",
    "enabled": true
}'
```
3. Add checks
```bash
curl --location 'localhost:8080/api/v1/targets/1/checks' \
--header 'Content-Type: application/json' \
--data '{
    "type": "GET",
    "intervalSeconds": 5
}'
```
4. Restart application

### Persistence
The application uses embedded H2. Go to `http://localhost:8080/h2-console` for the UI client.

## Tasks
- [ ] Implement check commands
- [ ] Update scheduler at runtime when a new command is added
- [ ] Add edit/remove check endpoint
- [ ] Expose check results as metrics
- [ ] Add Grafana