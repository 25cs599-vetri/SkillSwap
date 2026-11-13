
# SkillSwap – Student Skill Exchange Platform

A student-to-student learning platform where students can teach skills they know and learn skills they want to develop.

## About the Project

SkillSwap connects students based on their teaching and learning interests. It encourages peer learning, collaboration, and knowledge sharing within a student community.

## Features

- Student registration and login
- Profile creation and editing
- Add skills to teach
- Add skills to learn
- Discover compatible students
- Send learning requests
- Accept or reject requests
- Mark learning requests as completed

## Technology Stack

**Frontend**
- HTML
- CSS
- JavaScript

**Backend**
- Java 17
- Spring Boot
- Spring Data JPA
- Maven

**Database**
- MySQL 8.0

## Project Structure

```text
SkillSwap/
├── frontend/
│   ├── index.html
│   ├── style.css
│   └── app.js
├── backend/
│   ├── src/
│   └── pom.xml
├── .gitignore
└── README.md
```

## How to Run

### 1. Database Setup

Create a MySQL database:

```sql
CREATE DATABASE skillswap;
```

### 2. Configure Database Password

Set the Windows environment variable:

```text
DB_PASSWORD=your_mysql_password
```

### 3. Run Backend

Open a terminal inside the backend folder and execute:

```bash
mvn spring-boot:run
```

Backend runs at:

```text
http://localhost:8080
```

### 4. Run Frontend

Open `frontend/index.html` in your browser.

## Project Objective

To create a collaborative learning environment where students exchange knowledge, develop new skills, and build connections with other learners.

## Future Enhancements

- Student ratings and reviews
- Real-time chat
- Learning session scheduling
- Notifications
- Admin dashboard

---

**SkillSwap – Teach a little. Learn a lot. Grow together.**
