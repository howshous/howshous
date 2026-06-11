# Howshous System Architecture & Design Diagrams

This directory contains comprehensive architectural and design diagrams for the Howshous smart boarding house platform. All diagrams are created using PlantUML and can be rendered online or integrated into documentation.

## Diagrams Overview

### 1. **System Architecture Diagram** (`01-system-architecture.puml`)
High-level overview of all system components and their interactions:
- Client Layer (Mobile, Web, Landlord Portal)
- API Gateway & Services
- Business Logic Layer
- Data Layer
- External Services integration

### 2. **Use Case Diagram** (`02-use-case-diagram.puml`)
Complete use cases for all user types:
- **Tenant Use Cases**: Search, book, pay, request maintenance, review
- **Landlord Use Cases**: List properties, manage bookings, track payments, view analytics
- **Admin Use Cases**: User management, content moderation, dispute resolution
- **Payment Provider**: Process payments and refunds

### 3. **Class Diagram** (`03-class-diagram.puml`)
Domain model and object-oriented design:
- Core entities: User, Tenant, Landlord, Admin
- Business objects: Property, Room, Booking, Payment, Review
- Supporting classes: MaintenanceRequest, Document, Address
- Relationships and cardinalities

### 4. **Sequence Diagram - Booking Process** (`04-sequence-diagram-booking.puml`)
Step-by-step interaction flow for room booking:
1. Room search and availability check
2. Room selection and booking creation
3. Payment processing
4. Booking confirmation

### 5. **Sequence Diagram - Payment Processing** (`05-sequence-diagram-payment.puml`)
Detailed payment processing workflow:
1. Payment initiation
2. Gateway communication
3. Card validation and processing
4. Payment confirmation/failure handling
5. Notification dispatch

### 6. **Package Diagram** (`06-package-diagram.puml`)
Logical organization of code packages and modules:
- **Presentation**: Mobile and web interfaces
- **API**: REST endpoints and gateways
- **Services**: Core business logic (User, Booking, Payment, etc.)
- **Domain**: Models, entities, and value objects
- **Repository**: Data access layer
- **Infrastructure**: Database, cache, security, storage
- **External**: Third-party integrations
- **Shared**: Utilities, constants, middleware

### 7. **Deployment Diagram** (`07-deployment-diagram.puml`)
Production deployment architecture:
- Client devices (mobile, web)
- CDN and load balancing
- API server cluster
- Microservices (Kubernetes/Docker)
- Cache layer (Redis)
- Database (PostgreSQL Multi-AZ)
- Cloud storage (AWS S3)
- Monitoring (ELK, Prometheus, Grafana)
- External services integration

### 8. **State Diagram - Booking Lifecycle** (`08-state-diagram-booking.puml`)
All states and transitions in a booking's lifecycle:
- Created → Pending → PaymentPending → Confirmed → Active → Completed
- Cancellation paths at each stage
- State descriptions and business rules

### 9. **State Diagram - Payment Lifecycle** (`09-state-diagram-payment.puml`)
Payment processing states:
- Pending → Processing → Completed/Failed
- Refund state
- Retry logic and error handling

### 10. **Actor Scenarios** (`10-actor-scenarios.puml`)
User journey maps for three main actor types:
- **Tenant**: Download → Search → Book → Stay → Review
- **Landlord**: Signup → Verify → Add Properties → Manage → Analyze
- **Admin**: Login → Manage → Moderate → Resolve → Monitor

### 11. **Entity Relationship Diagram** (`11-erd-diagram.puml`)
Complete database schema:
- All entities and their attributes
- Primary and foreign keys
- Relationships and cardinalities
- Data types for each field

**Key Tables**:
- `users`, `tenants`, `landlords`
- `properties`, `rooms`, `amenities`
- `bookings`, `payments`
- `maintenance_requests`, `reviews`
- `documents`, `room_amenities`, `property_rules`

### 12. **Activity Diagram** (`12-activity-diagram.puml`)
Complete booking-to-checkout process flow:
1. Search and selection
2. Booking creation
3. Payment processing
4. Check-in procedures
5. Stay and maintenance
6. Checkout and settlement
7. Review and closure

### 13. **Tenant Actor Diagram** (`13-tenant-actor-diagram.puml`)
Detailed tenant profile and interactions:
- **UC1**: Room search & discovery
- **UC2**: View property details
- **UC3**: Book room & make payment
- **UC4**: Manage booking
- **UC5**: Submit maintenance request
- **UC6**: Rate & review property
- **UC7**: Communicate with landlord
- **UC8**: View history & analytics
- **UC9**: Manage profile & documents

### 14. **Landlord Actor Diagram** (`14-landlord-actor-diagram.puml`)
Detailed landlord profile and interactions:
- **UC1**: Property onboarding
- **UC2**: Room setup & configuration
- **UC3**: Pricing & availability management
- **UC4**: Booking management
- **UC5**: Tenant management
- **UC6**: Maintenance management
- **UC7**: Payment tracking
- **UC8**: Analytics & reports
- **UC9**: Tenant communication

### 15. **Admin Actor Diagram** (`15-admin-actor-diagram.puml`)
Detailed admin profile and interactions:
- **UC1**: User management (verification, suspension, disputes)
- **UC2**: Content moderation (listings, reviews)
- **UC3**: Booking oversight
- **UC4**: Payment monitoring (fraud detection, audits)
- **UC5**: Report generation
- **UC6**: Support ticket management
- **UC7**: System monitoring
- **UC8**: Policy management
- **UC9**: Dispute resolution

---

## How to View Diagrams

### Option 1: Online PlantUML Editor
1. Visit: https://www.plantuml.com/plantuml/uml/
2. Copy and paste the contents of any `.puml` file
3. Click "Render" to view the diagram

### Option 2: VS Code PlantUML Extension
1. Install the PlantUML extension for VS Code
2. Open any `.puml` file
3. Right-click and select "Preview PlantUML Diagram"

### Option 3: Command Line (requires PlantUML installation)
```bash
plantuml -Tpng 01-system-architecture.puml
plantuml -Tsvg 02-use-case-diagram.puml
```

### Option 4: Documentation Integration
These diagrams can be embedded in your manuscript using:
- Markdown with embedded SVG or PNG images
- PDF generation tools that support PlantUML
- Documentation platforms like Confluence, GitBook, etc.

---

## Key Architectural Insights

### Technology Stack
- **Backend**: Kotlin (96%) - primary development language
- **Frontend**: TypeScript (2.3%) - web and mobile interfaces
- **Other**: (1.7%) - configuration, scripts, documentation

### Architecture Patterns
1. **Microservices**: Independent, scalable services
2. **API-First**: RESTful API gateway for all clients
3. **Event-Driven**: Message queue for async operations
4. **Multi-Tier**: Clear separation of concerns
5. **Cloud-Native**: Container orchestration with Kubernetes

### Key Features
- **Multi-user Support**: Tenants, Landlords, Admins
- **Secure Payments**: PCI-DSS compliant payment processing
- **Scalability**: Horizontal scaling with load balancing
- **High Availability**: Multi-AZ database with replication
- **Real-time**: Notifications and live updates via message queue
- **Analytics**: Comprehensive reporting and insights

---

## Document Integration Tips

When including these diagrams in your manuscript:

1. **Add Captions**: Each diagram should have a numbered caption (e.g., "Figure 1: System Architecture")
2. **Reference in Text**: Cite diagrams by number (e.g., "As shown in Figure 1...")
3. **Quality**: Use SVG or high-resolution PNG for better print quality
4. **Consistency**: Maintain uniform styling across all diagrams
5. **Legend**: Include brief legends for complex diagrams
6. **Tables**: Complement diagrams with summary tables of components/relationships

---

## Customization

All diagrams can be customized by modifying:
- Colors (skinparam)
- Font sizes and styles
- Component names and descriptions
- Relationships and flows
- Layout direction (top-to-bottom, left-to-right)

Refer to [PlantUML Documentation](https://plantuml.com/guide) for advanced customization options.

---

**Last Updated**: June 11, 2026
**Repository**: howshous/howshous
**Version**: 1.0
