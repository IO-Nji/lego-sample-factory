# 🏭 LEGO Sample Factory Control System

<div align="center">

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?logo=docker&logoColor=white)](docker-compose.yml)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-6DB33F?logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-61DAFB?logo=react&logoColor=black)](https://reactjs.org/)
[![Status](https://img.shields.io/badge/Status-Production%20Ready-success)]()

**LIFE** - LEGO Integrated Factory Execution

*A modern, microservice-based manufacturing control platform that digitizes and automates supply chain operations for the LEGO Sample Factory.*

[Quick Start](#-quick-start) • [Features](#-current-features) • [Architecture](#-system-architecture) • [API Documentation](#-api-design) • [Roadmap](#-roadmap)

</div>

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Current Features](#-current-features)
- [System Architecture](#-system-architecture)
- [Technology Stack](#-technology-stack)
- [Quick Start](#-quick-start)
- [API Design](#-api-design)
- [Configuration](#-configuration)
- [Development](#-development)
- [Roadmap](#-roadmap)
- [Troubleshooting](#-troubleshooting)
- [Contributing](#-contributing)
- [License](#-license)

---

## 🎯 Overview

**LIFE** (LEGO Integrated Factory Execution) is a production-ready prototype that modernizes manufacturing operations through intelligent digital workflows. The platform provides:



- ✅ **Real-time Production Control** – End-to-end order lifecycle management from creation to fulfillment
- 🔐 **Role-Based Access Control** – Nine distinct roles with granular permissions (Admin, Plant Warehouse, Production Control, etc.)
- 📊 **Live Inventory Tracking** – Real-time stock monitoring across all workstations with automatic updates
- 🎯 **Workstation Assignment** – Dynamic task allocation based on user roles and workstation capabilities
- 📱 **Responsive Dashboards** – Role-specific interfaces optimized for operational efficiency
- 🔄 **Automated Workflows** – Intelligent order routing and status transitions based on business rules
- 🌐 **Microservice Architecture** – Independently scalable services with complete data isolation

### Key Benefits

- **Operational Efficiency**: Reduce manual paperwork and streamline production workflows
- **Real-Time Visibility**: Live dashboards provide instant insight into factory operations
- **Scalability**: Microservice design allows independent scaling of critical components
- **Flexibility**: Modular architecture supports rapid feature development and customization
- **Reliability**: Health checks, error handling, and comprehensive logging ensure system stability

---

## ✨ Current Features

### 🔐 Authentication & Authorization

- **JWT-Based Authentication** with secure token management
- **Nine Role Types**: 
  - `ADMIN` – System administration and user management
  - `PLANT_WAREHOUSE` – Customer order management and fulfillment
  - `MODULES_SUPERMARKET` – Module warehouse operations
  - `PRODUCTION_PLANNING` – Factory-wide production scheduling
  - `PRODUCTION_CONTROL` – Manufacturing order execution
  - `ASSEMBLY_CONTROL` – Assembly workstation operations
  - `PARTS_SUPPLY` – Parts warehouse management
  - `MANUFACTURING` – Production line operations
  - `VIEWER` – Read-only system access
- **Workstation-Based Access Control** – Users assigned to specific workstations for targeted operations
- **Protected Routes** with automatic token refresh and expiration handling

### 📦 Product & Inventory Management

- **Master Data Management**:
  - Product variants with configurable attributes
  - Module catalog with bill-of-materials
  - Parts library with specifications
  - Workstation registry (warehouses, manufacturing cells, assembly stations)
- **Real-Time Inventory**:
  - Live stock tracking per workstation
  - Automated inventory updates on order fulfillment
  - Low stock alerts and notifications
  - Multi-location inventory visibility

### 📋 Order Processing & Fulfillment

- **Customer Order Lifecycle**:
  - Order creation with multiple line items
  - Status tracking: `PENDING` → `IN_PROGRESS` → `COMPLETED` → `DELIVERED`
  - Workstation-specific order queues
  - Fulfillment actions with inventory validation
- **Production Control Orders**:
  - Manufacturing order creation and scheduling
  - Work-in-progress tracking
  - Start/pause/complete workflows
  - Notes and annotations for quality control
- **Warehouse Supply Orders**:
  - Cross-warehouse material requests
  - Approval workflows
  - Fulfillment tracking with quantity verification

### 🏭 Workstation Operations

- **Role-Specific Dashboards**:
  - **Admin Dashboard**: System KPIs, user management, configuration
  - **Plant Warehouse**: Customer order intake and fulfillment
  - **Modules Supermarket**: Internal warehouse request handling
  - **Production Planning**: Factory-wide scheduling and resource allocation
  - **Production Control**: Manufacturing task execution
  - **Assembly Control**: Assembly operation management
- **Task Management**: Work queues, priority sorting, deadline tracking
- **Live Updates**: Auto-refresh (5-10s intervals) for real-time data synchronization

### 📊 Production Scheduling (SimAL Integration)

- **Scheduling Engine**: Integration with SimAL for production planning
- **Resource Allocation**: Workstation and material availability checks
- **Production Control Order Generation**: Automated order creation from schedules
- **Assembly Control Order Management**: Sub-assembly tracking and coordination

### 🛡️ Error Handling & Observability

- **Comprehensive Logging**: Structured logs across all microservices
- **Health Checks**: Spring Boot Actuator endpoints for service monitoring
- **Global Exception Handling**: Standardized error responses with meaningful messages
- **API Gateway Monitoring**: Request/response logging and metrics

   cd lego-factory-frontend
   ```
   Access at http://localhost:5173

```powershell
.\mvnw clean package

- [ ] **Load Balancing** – Horizontal scaling of microservices

---
if (isTokenExpired(token)) {
# 🏭 LEGO Sample Factory Control System

Welcome to the LEGO Sample Factory Control System! This document provides a high-level introduction, project goals, and a summary of the system's features.

---

## Table of Contents

1. [Introduction & Overview](README.intro.md)
2. [Architecture & API Design](README.architecture.md)
3. [Development & Operations Guide](README.devops.md)
4. [Roadmap, Contributing & Standards](README.roadmap.md)
5. [License, Acknowledgments & Contact](README.license.md)

---

## Project Overview

The LEGO Sample Factory is a microservice-based manufacturing execution system (MES) for managing LEGO production, inventory, and order processing. It features a modern React frontend, Spring Boot backend, and Dockerized deployment for rapid prototyping and demonstration.

### Key Features
- Role-based dashboards for admin, warehouse, modules, and production
- Real-time inventory and order management
- Production scheduling with SimAL integration
- JWT authentication and secure API gateway
- Responsive UI with live updates and notifications

For detailed architecture, API endpoints, and development instructions, see the referenced topic files above.

---

## Quick Start

See [Development & Operations Guide](README.devops.md) for setup and usage instructions.

---

## License & Acknowledgments

See [License, Acknowledgments & Contact](README.license.md).
SOFTWARE.
```

---

## 🙏 Acknowledgments

- **Spring Boot** team for the excellent framework
- **React** community for the vibrant ecosystem
- **Docker** for simplifying deployment
- LEGO Group for inspiration

---

## 📞 Contact & Support

- **Repository**: [https://github.com/<your-org>/lego-sample-factory](https://github.com/<your-org>/lego-sample-factory)
- **Issues**: [GitHub Issues](https://github.com/<your-org>/lego-sample-factory/issues)
- **Discussions**: [GitHub Discussions](https://github.com/<your-org>/lego-sample-factory/discussions)

---

<div align="center">

**[⬆ Back to Top](#-lego-sample-factory-control-system)**

Made with ❤️ for LEGO Manufacturing Operations

</div>