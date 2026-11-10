# PROJECT: AI REQUIREMENT ANALYZER

## Complete Software Product Development Specification

Act as a Senior Software Architect, Business Analyst, Java Backend
Engineer, Python AI Engineer, React Developer, Database Architect,
DevOps Engineer and QA Engineer.

Your responsibility is to design and develop a production-oriented
AI-powered Requirement Analyzer application from scratch.

Do not build a simple chatbot or a basic CRUD application. Build a
structured software product following professional software engineering
standards, proper architecture, separation of concerns, security,
validation, testing, documentation and maintainability.

------------------------------------------------------------------------

# 1. PROJECT OVERVIEW

Project Name: AI Requirement Analyzer

Project Type: AI-powered Software as a Service (SaaS) application.

Primary Objective:

Build an AI-powered application that accepts software project
descriptions and requirement documents, analyzes their contents and
automatically converts unstructured information into structured software
requirements.

The system should help business analysts, project managers, software
architects and developers reduce manual requirement analysis work.

The application must support two input methods:

1.  Plain-text project descriptions.
2.  Document uploads such as PDF, DOCX, TXT and XLSX.

The system must use a Human-in-the-Loop approach.

AI can analyze, recommend, generate and validate requirements, but users
must review, edit, approve or reject generated results.

Never automatically treat AI-generated information as confirmed business
requirements.

# 2. TECHNOLOGY STACK

Use the following technologies.

## Frontend

-   React
-   TypeScript
-   Vite
-   React Router
-   Axios
-   Tailwind CSS
-   React Hook Form
-   Zod for client-side validation
-   Mermaid or an equivalent diagram renderer where useful

## Main Backend

-   Java 21
-   Spring Boot 3.x
-   Spring Web
-   Spring Data JPA
-   Spring Security
-   JWT authentication
-   Hibernate
-   Bean Validation
-   PostgreSQL driver
-   Flyway database migrations
-   Lombok only where it improves maintainability
-   Maven
-   OpenAPI / Swagger

## AI Backend

-   Python 3.12+
-   FastAPI
-   Pydantic
-   LangGraph
-   Configurable LLM provider integration
-   PyMuPDF or equivalent PDF parser
-   python-docx
-   openpyxl
-   PostgreSQL integration where required
-   pgvector for semantic retrieval when needed

## Database

-   PostgreSQL
-   pgvector extension for semantic search

## DevOps

-   Docker
-   Docker Compose
-   Git
-   GitHub
-   GitHub Actions

Use compatible stable dependency versions. Verify compatibility before
generating project configuration files.

Do not assume that a paid AI API or unlimited free API access is
available. Make the LLM provider configurable through environment
variables and support alternative compatible providers or local models
where feasible.

Never hardcode API keys.

# 3. SYSTEM ARCHITECTURE

Build the application using a modular monolith architecture for the Java
backend and a separately deployable Python AI service.

Responsibilities must be clearly separated.

## React Frontend

Responsible for: - User registration and login. - Project dashboard. -
Project creation and management. - Document upload. - Project
description submission. - Analysis progress. - Requirement
visualization. - Requirement editing and approval. - Export
functionality.

## Spring Boot Backend

Responsible for: - Authentication and authorization. - User
management. - Project management. - Document metadata and upload
orchestration. - Business validation. - Database persistence. - AI
service communication. - Analysis job management. - Requirement approval
workflow. - Audit history. - Export orchestration.

## Python AI Service

Responsible for: - Document text extraction. - Text preprocessing. -
Requirement extraction. - Requirement classification. - User story
generation. - Acceptance criteria generation. - Business rule
extraction. - Duplicate detection assistance. - Contradiction detection
assistance. - Missing requirement analysis. - Requirement prioritization
recommendations. - Structured LLM output. - AI workflow orchestration.

The Python service must not independently own or modify the core
business records. Spring Boot remains the authoritative business-data
owner.

## Communication

React communicates with Spring Boot through REST APIs.

Spring Boot communicates with Python FastAPI through authenticated
internal REST APIs.

Use asynchronous analysis jobs.

A typical workflow:

1.  User submits a project description or uploads documents.
2.  Spring Boot validates the request.
3.  Spring Boot creates an analysis job.
4.  The backend sends the analysis request to Python.
5.  Python processes the input.
6.  AI agents generate structured analysis results.
7.  Python returns validated structured JSON.
8.  Spring Boot validates and persists the results.
9.  The frontend retrieves job status and displays results.
10. User reviews and approves or edits the results.

Use background processing for long-running operations. Implement job
status tracking and safe retry mechanisms.

# 4. FUNCTIONAL REQUIREMENTS

Develop the following modules.

## MODULE A: Authentication and User Management

Implement:

-   User registration.
-   User login.
-   Logout.
-   JWT access token authentication.
-   Secure password hashing using BCrypt or Argon2.
-   User profile management.
-   Role-based authorization.
-   Protected routes.
-   Token expiration and refresh strategy.
-   Account status management.

Initial roles: - ADMIN - USER

Users must only access projects and documents they are authorized to
access.

## MODULE B: Project Management

Users should be able to:

-   Create projects.
-   View all their projects.
-   View project details.
-   Update project information.
-   Archive projects.
-   Delete projects according to retention policies.

Project attributes:

-   Project ID.
-   Project name.
-   Description.
-   Domain.
-   Project owner.
-   Creation date.
-   Updated date.
-   Status.

Support project statuses: - DRAFT - IN_PROGRESS - ANALYSIS_COMPLETED -
ARCHIVED

## MODULE C: Document Management

Support uploads of:

-   PDF
-   DOCX
-   TXT
-   XLSX

Requirements:

-   File size validation.
-   MIME type validation.
-   File extension validation.
-   File name sanitization.
-   Secure file storage.
-   Duplicate file detection.
-   Upload progress.
-   Upload status.
-   Document metadata.
-   Document download authorization.
-   Document deletion.
-   Document processing status.

Do not trust file extensions alone.

Use secure storage outside publicly accessible directories. Design the
storage layer so local storage can later be replaced with S3-compatible
object storage.

## MODULE D: Requirement Extraction

The AI must extract:

1.  Functional requirements.
2.  Non-functional requirements.
3.  Business requirements.
4.  Business rules.
5.  System constraints.
6.  External system dependencies.
7.  User roles and actors.
8.  Use cases.

Each requirement should contain:

-   Requirement ID.
-   Title.
-   Description.
-   Requirement type.
-   Priority.
-   Source document or text reference.
-   Source page or section where available.
-   AI confidence estimate.
-   Status.
-   Creation date.
-   Updated date.

Requirement types: - FUNCTIONAL - NON_FUNCTIONAL - BUSINESS -
TECHNICAL_CONSTRAINT

Priority: - CRITICAL - HIGH - MEDIUM - LOW

Status: - PENDING_REVIEW - APPROVED - REJECTED - MODIFIED

Preserve source traceability. Never fabricate source page numbers or
quotations.

## MODULE E: User Story Generator

Convert requirements into user stories using:

As a \[user role\], I want \[functionality\], So that \[business
benefit\].

Generate multiple stories when a requirement covers multiple independent
functionalities.

Each story must contain:

-   Story ID.
-   Requirement reference.
-   User role.
-   Action.
-   Business benefit.
-   Description.
-   Priority.
-   Status.

## MODULE F: Acceptance Criteria Generator

Generate testable acceptance criteria for each user story.

Use Given-When-Then format where appropriate.

Example:

Given a teacher is authenticated, When the teacher selects an assigned
class and submits attendance, Then the system must save attendance for
the selected date.

Criteria must be specific, measurable and verifiable.

Identify ambiguous criteria and ask clarification questions.

## MODULE G: Business Rule Extraction

Extract business rules from documents and descriptions.

Each rule should contain:

-   Rule ID.
-   Rule title.
-   Rule description.
-   Related requirement.
-   Source reference.
-   Status.

Identify conflicting business rules.

Do not resolve business conflicts without human approval.

## MODULE H: Requirement Validation

Implement AI-assisted validation for:

-   Missing information.
-   Duplicate requirements.
-   Contradictory requirements.
-   Ambiguous descriptions.
-   Incomplete requirements.
-   Unclear acceptance criteria.
-   Missing actors.
-   Missing business rules.
-   Potential feasibility concerns.

For every detected issue, provide:

-   Issue ID.
-   Issue type.
-   Description.
-   Related requirement.
-   Severity.
-   Suggested resolution.
-   Evidence or source reference.

Clearly distinguish verified conflicts from AI-suggested possible
conflicts.

## MODULE I: Requirement Prioritization

Recommend priority based on:

-   Business value.
-   User impact.
-   Technical dependencies.
-   Security impact.
-   Implementation urgency.

Allow users to modify AI-recommended priorities.

Do not allow AI recommendations to overwrite user decisions.

## MODULE J: Missing Requirement Questions

The AI should identify missing business information.

Example:

For a school attendance system:

-   Can teachers edit previously submitted attendance?
-   Can students view their attendance percentage?
-   Who can approve attendance corrections?
-   Can attendance be marked for individual subjects?

Generate:

-   Question ID.
-   Question.
-   Related requirement.
-   Reason for asking.
-   Impact if unanswered.
-   Status.

Users must be able to answer questions and trigger a revised analysis.

## MODULE K: Human Review Workflow

This is a critical feature.

Users must be able to:

-   Review extracted requirements.
-   Edit requirement descriptions.
-   Modify priorities.
-   Edit user stories.
-   Edit acceptance criteria.
-   Approve requirements.
-   Reject requirements.
-   Add manual requirements.
-   Answer missing-information questions.
-   Re-run analysis when necessary.

Record: - Original AI-generated content. - User-modified content. - User
action. - Action timestamp. - Previous status. - New status.

Never overwrite the original analysis without maintaining version
history.

## MODULE L: Analysis History

Store:

-   Analysis job ID.
-   Input source.
-   Processing status.
-   Model/provider information.
-   Analysis version.
-   Processing timestamps.
-   Error information.
-   Output references.

Statuses:

-   QUEUED
-   PROCESSING
-   COMPLETED
-   FAILED
-   CANCELLED

Provide retry functionality for eligible failures.

Avoid duplicate processing through idempotency mechanisms.

## MODULE M: Export

Allow users to export:

-   Functional requirements.
-   Non-functional requirements.
-   Business rules.
-   User stories.
-   Acceptance criteria.
-   Missing requirement questions.
-   Validation reports.

Support: - JSON - PDF - Excel

Include project details, analysis version and export timestamp.

# 5. AI AGENT ARCHITECTURE

Use LangGraph to orchestrate specialized AI processing nodes.

Do not create unnecessary autonomous agents for tasks that can be
implemented more reliably as deterministic processing functions.

Proposed workflow:

1.  Input Processing Node.
2.  Requirement Extraction Agent.
3.  Requirement Classification Agent.
4.  User Story Generation Agent.
5.  Acceptance Criteria Generation Agent.
6.  Business Rule Extraction Agent.
7.  Requirement Validation Agent.
8.  Missing Information Detection Agent.
9.  Prioritization Agent.
10. Output Validation Node.

The orchestrator should manage execution order, shared state, error
handling and retries.

Agents should exchange structured Pydantic models rather than arbitrary
paragraphs.

Implement:

-   Strict JSON schema validation.
-   Bounded retries.
-   Timeout handling.
-   LLM rate-limit handling.
-   Provider abstraction.
-   Model configuration.
-   Token usage tracking.
-   Error logging.
-   Prompt versioning.
-   Source traceability.

Treat uploaded documents as untrusted data.

Protect against prompt injection. Instructions embedded in uploaded
documents must never override system instructions or trigger
unauthorized tool execution.

Use deterministic validation wherever possible instead of asking an LLM
to validate everything.

# 6. DATABASE DESIGN

Design a normalized PostgreSQL database.

Initial entities:

1.  users
2.  roles
3.  user_roles
4.  projects
5.  documents
6.  analysis_jobs
7.  requirements
8.  user_stories
9.  acceptance_criteria
10. business_rules
11. requirement_questions
12. requirement_issues
13. requirement_dependencies
14. analysis_versions
15. audit_logs
16. exports

Define:

-   UUID primary keys.
-   Foreign keys.
-   Unique constraints.
-   Check constraints.
-   Indexes.
-   Created timestamps.
-   Updated timestamps.
-   Appropriate deletion behavior.
-   Relationships and cardinality.

Use Flyway for schema migrations.

Avoid storing sensitive information unnecessarily.

Provide an ER diagram using Mermaid.

Explain the relationships and justify important database design
decisions.

# 7. REST API DESIGN

Create versioned REST APIs under /api/v1.

Design endpoints for:

Authentication: - POST /api/v1/auth/register - POST /api/v1/auth/login -
POST /api/v1/auth/refresh - POST /api/v1/auth/logout

Projects: - POST /api/v1/projects - GET /api/v1/projects - GET
/api/v1/projects/{id} - PUT /api/v1/projects/{id} - DELETE
/api/v1/projects/{id}

Documents: - POST /api/v1/projects/{id}/documents - GET
/api/v1/projects/{id}/documents - GET /api/v1/documents/{id} - DELETE
/api/v1/documents/{id}

Analysis: - POST /api/v1/projects/{id}/analyses - GET
/api/v1/analyses/{id} - POST /api/v1/analyses/{id}/retry - POST
/api/v1/analyses/{id}/cancel

Requirements: - GET /api/v1/analyses/{id}/requirements - GET
/api/v1/requirements/{id} - PATCH /api/v1/requirements/{id} - POST
/api/v1/requirements/{id}/approve - POST
/api/v1/requirements/{id}/reject

User stories: - GET /api/v1/analyses/{id}/stories - PATCH
/api/v1/stories/{id}

Acceptance criteria: - GET /api/v1/stories/{id}/acceptance-criteria -
PATCH /api/v1/acceptance-criteria/{id}

Questions: - GET /api/v1/analyses/{id}/questions - POST
/api/v1/questions/{id}/answer

Exports: - POST /api/v1/analyses/{id}/exports - GET /api/v1/exports/{id}

These are initial API proposals. Refine request and response schemas
based on the final domain model.

For each endpoint, document: - HTTP method. - URL. - Request DTO. -
Response DTO. - Validation. - Authentication requirements. -
Authorization rules. - HTTP status codes. - Error scenarios.

Use pagination for collection endpoints.

Use consistent error responses, preferably Spring ProblemDetail.

# 8. FRONTEND DESIGN

Create a professional, modern SaaS dashboard.

Required pages:

1.  Landing page.
2.  Registration page.
3.  Login page.
4.  Dashboard.
5.  Create project page.
6.  Project details page.
7.  Document upload page.
8.  Analysis progress page.
9.  Requirement analysis results page.
10. Requirement details page.
11. User stories page.
12. Acceptance criteria page.
13. Missing information page.
14. Analysis history page.
15. Export page.
16. User profile page.

Dashboard should show:

-   Total projects.
-   Total documents.
-   Completed analyses.
-   Pending reviews.
-   Approved requirements.
-   Recent projects.
-   Recent analysis jobs.

The requirement results page should support:

-   Search.
-   Filtering by requirement type.
-   Filtering by priority.
-   Filtering by status.
-   Sorting.
-   Pagination.
-   Inline editing.
-   Approval and rejection.
-   Viewing original source references.

Create responsive layouts for desktop, tablet and mobile.

Include loading states, empty states, error messages and confirmation
dialogs.

Use reusable React components and properly organized feature modules.

# 9. SECURITY REQUIREMENTS

Implement professional security practices.

-   Secure authentication.
-   Password hashing.
-   JWT validation.
-   Role-based authorization.
-   Project-level access checks.
-   File upload security.
-   MIME type verification.
-   File size limits.
-   Filename sanitization.
-   Rate limiting.
-   Secure internal AI service communication.
-   Environment-based secret management.
-   CORS restrictions.
-   SQL injection protection.
-   Input validation.
-   Audit logging.
-   Sensitive data protection.

Never expose LLM API keys to the frontend.

Do not log passwords, tokens or confidential document contents.

# 10. TESTING STRATEGY

## Java Backend

Use JUnit 5, Mockito and Spring Boot Test.

Write: - Unit tests. - Service tests. - Controller tests. - Repository
tests. - Security tests. - Integration tests.

## Python AI Service

Use Pytest.

Test: - Document parsing. - Invalid document handling. - Requirement
extraction. - Structured output validation. - Duplicate detection. -
Missing information detection. - LLM timeout handling. - Retry
mechanisms. - Prompt injection resistance. - Provider failures.

Use mocked LLM responses for deterministic automated tests.

## Frontend

Use appropriate React testing tools.

Test: - Form validation. - Login workflows. - Project creation. - File
upload. - Analysis status. - Requirement editing. - Approval workflow. -
API error handling.

Include end-to-end tests for the most important user journeys.

# 11. ERROR HANDLING AND LOGGING

Implement centralized exception handling.

Java backend: - GlobalExceptionHandler. - Custom business exceptions. -
Validation exceptions. - ResourceNotFoundException. -
AccessDeniedException. - ExternalServiceException.

Use consistent ProblemDetail responses.

Python: - Structured exceptions. - Centralized exception handlers. -
Correlation IDs. - Structured logging.

Handle: - Invalid files. - Unsupported formats. - Empty documents. - LLM
provider failures. - Invalid LLM JSON. - Database failures. - Network
timeouts. - Duplicate submissions. - Unauthorized requests.

Do not expose internal stack traces to users.

# 12. NON-FUNCTIONAL REQUIREMENTS

The application should provide:

-   Maintainability.
-   Scalability.
-   Reliability.
-   Security.
-   Good performance.
-   Responsive UI.
-   Consistent API contracts.
-   Testability.
-   Observability.
-   Data integrity.

Avoid unnecessary microservices and excessive abstraction.

Use appropriate database indexes and pagination.

Implement safe asynchronous processing.

Make AI provider configuration replaceable without rewriting business
logic.

# 13. PROJECT STRUCTURE

Maintain three separate codebases:

requirement-analyzer-frontend/ requirement-analyzer-backend/
requirement-analyzer-ai-service/

Provide: - Complete folder structure. - Configuration files. -
Environment variable examples. - Dockerfiles. - Docker Compose. -
Database migrations. - README files. - API documentation. - Architecture
documentation.

Do not commit .env files or secrets.

# 14. DEVELOPMENT METHODOLOGY

Follow an iterative development approach.

Do not generate the entire project in one enormous code dump.

Work in milestones.

For every milestone:

1.  Explain the objective.
2.  Explain the architecture and design decisions.
3.  Define the functional requirements.
4.  Identify relevant entities and relationships.
5.  Define API contracts.
6.  Implement the functionality.
7.  Explain important code.
8.  Add validation and exception handling.
9.  Write automated tests.
10. Verify the implementation.
11. Update documentation.
12. Identify remaining tasks.

Do not skip ahead when a critical dependency is incomplete.

Before implementing a feature, inspect the existing codebase and
preserve established conventions.

Do not replace existing code unnecessarily.

Do not provide pseudocode when working implementation is requested.

Do not claim a test passed unless it was actually executed.

# 15. IMPORTANT DEVELOPMENT RULES

I want to understand the project while developing it.

For every major implementation, explain:

-   Why this class exists.
-   Why this package exists.
-   Why this dependency is required.
-   Why this database relationship is necessary.
-   Why this API is designed this way.
-   How data flows between frontend, backend, AI service and database.
-   How errors are handled.
-   How security is maintained.
-   How the code can be tested.

Prefer practical professional engineering over unnecessary complexity.

Use clean code, SOLID principles where appropriate, dependency
injection, DTO separation, service and repository layers, and clear
naming conventions.

Never put business logic inside controllers.

Do not expose JPA entities directly through REST APIs.

Do not make the LLM responsible for deterministic business validation.

Do not trust AI output without validating it.

# 16. EXPECTED FINAL DELIVERABLE

The completed product must allow a registered user to:

1.  Log in securely.
2.  Create a software project.
3.  Enter a project description or upload a document.
4.  Start an AI requirement analysis.
5.  Track the analysis progress.
6.  View extracted functional and non-functional requirements.
7.  View generated business rules.
8.  View generated user stories.
9.  View acceptance criteria.
10. Identify missing or ambiguous information.
11. Edit AI-generated results.
12. Approve or reject requirements.
13. View analysis history.
14. Export the final approved requirement package.

The application must be runnable using documented setup instructions.

It must include a working database, backend, AI service and frontend.

# 17. FIRST ACTION --- DO NOT START CODING IMMEDIATELY

Begin by performing these tasks:

1.  Analyze the entire project specification.
2.  Identify missing or ambiguous decisions.
3.  Prepare the Software Requirements Specification for this
    application.
4.  Prepare a use case list.
5.  Define functional and non-functional requirements.
6.  Identify actors and permissions.
7.  Prepare the system architecture.
8.  Prepare the database ER diagram.
9.  Define the API contract.
10. Prepare the development milestones.

Present the architecture and design decisions for review before
implementation.

After approval, begin Milestone 1 and continue incrementally.

Your objective is to build a real, maintainable and extensible AI
Requirement Analyzer, not merely demonstrate an LLM API integration.
