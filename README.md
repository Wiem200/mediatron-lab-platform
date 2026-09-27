# 🌐 MEDIATRON Laboratory Platform

![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![React](https://img.shields.io/badge/React-18-61DAFB?style=for-the-badge&logo=react&logoColor=black)
![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-3.0-38BDF8?style=for-the-badge&logo=tailwindcss&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Neon_DB-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Containerized-2496ED?style=for-the-badge&logo=docker&logoColor=white)

An automated, data-driven web application developed for the **MEDIATRON Research Laboratory** (LR11TIC05) at **SUP'COM** (University of Carthage). The platform automates scientific output aggregation, member management, and research dissemination through external API integrations, web scraping, human-in-the-loop validation, and interactive analytics.

---

## ✨ Features

- **🔒 Authentication & Access Control**
  - Institutional domain restriction (`@supcom.tn`) with identity cross-referencing.
  - JWT stateless authentication and Role-Based Access Control (RBAC).
  - Single-occupant dynamic promotion for **Lab Head** role.
  - Temporary **Administrative Delegation** with immutable audit logging.

- **🤖 Scientific Output Automation & Management**
  - **Hybrid Ingestion Engine**: Automated fetching via Crossref, Semantic Scholar, and Google Scholar APIs/scraping.
  - **Deduplication Engine**: Two-tier duplicate detection using DOI uniqueness and title/year similarity heuristics.
  - **Author Matching**: Intelligent similarity-based matching (≥ 70%) associating scraped papers to member profiles[cite: 6].
  - **Filterable Repository**: Real-time multi-criteria filtering (author, type, year) with type badges and CSV/PDF export options[cite: 6].

- **🔄 Human-in-the-Loop Validation Workflows**
  - Multi-stage lifecycle (`PENDING` → `APPROVED` / `REJECTED`) for scraped papers, projects, and events[cite: 6].
  - Dedicated validation dashboards for members and the Laboratory Head[cite: 6].

- **📊 Public Portal & Interactive Analytics**
  - Dynamic landing page featuring lab history, research axes, latest papers, and projects[cite: 6].
  - Embedded **Grafana** analytics dashboards for real-time statistical insights[cite: 6].
  - Dedicated directories for members and PhD students[cite: 6].

- **💬 Intelligent Engagement & Chatbot**
  - Floating **AI Chatbot** powered by Google Gemini, grounded directly in database context[cite: 6].
  - Automated subscriber alerts and newsletter broadcasts via Brevo integration[cite: 6].

---

## 🛠 Tech Stack

### **Backend**
* **Framework:** Java 17, Spring Boot 3.4.3[cite: 6]
* **Security:** Spring Security, JWT, BCrypt Password Hashing[cite: 6]
* **Persistence:** Spring Data JPA, Hibernate[cite: 6]
* **Database Management:** Flyway Migrations[cite: 6]

### **Frontend**
* **Library/Tooling:** React.js, Vite[cite: 6]
* **Styling:** Tailwind CSS[cite: 6]
* **Data Visualization:** Grafana Dashboards[cite: 6]

### **Database & DevOps**
* **Database:** Serverless Neon PostgreSQL[cite: 6]
* **Containerization:** Docker[cite: 6]
* **External Services:** Crossref API, Semantic Scholar API, Google Scholar Scraper, Brevo Mailer API, Google Gemini API[cite: 6]

---

## 🏗 System Architecture

The platform follows a layered Model-View-Controller (MVC) architecture ensuring clear separation of concerns[cite: 6]:
