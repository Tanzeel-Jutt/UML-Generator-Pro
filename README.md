# 🔷 UML Generator Pro

**Professional UML, ER, EERD & Relational Mapping Generator**

A powerful diagram generator that supports Java, Python, C++, and SQL — available as both a **Web App** and a **Desktop Application (JavaFX)**.

![UML Generator Pro](https://img.shields.io/badge/Version-1.0-blue) ![License](https://img.shields.io/badge/License-MIT-green) ![Platform](https://img.shields.io/badge/Platform-Web%20%7C%20Desktop-purple)

---

## ✨ Features

### 📊 Diagram Types
- **UML Class Diagrams** — Full support for classes, interfaces, enums, abstract classes
- **ER Diagrams** — Entity-Relationship with Chen notation
- **EERD Diagrams** — Enhanced ER with ISA hierarchies & specialization
- **Relational Mapping** — Automatic ER-to-Relational conversion
- **Normalized SQL** — Auto-generated DDL scripts

### 🛡️ SOLID Analysis
- Real-time SOLID principle violation detection
- Detailed reports with suggestions

### 🔄 Dual Mode
- **Code → UML**: Paste source code, get diagrams instantly
- **UML → Code**: Design UML visually, generate boilerplate code

### 🌐 Supported Languages
| Language | Code → UML | UML → Code |
|----------|:----------:|:----------:|
| Java     | ✅         | ✅         |
| Python   | ✅         | ✅         |
| C++      | ✅         | ✅         |
| SQL      | ✅         | —          |

---

## 🚀 Quick Start

### Web App (Recommended)
Visit the live web app: **[UML Generator Pro](https://uml-generator-pro.netlify.app)**

### Desktop App
1. Download `uml-generator-pro.jar` from the [Releases](https://github.com/Tanzeel-Jutt/UML-Generator-Pro/releases) or from the `web/` folder
2. Run: `java -jar uml-generator-pro.jar`
3. Requires Java 17+

### Build from Source
```bash
# Clone the repository
git clone https://github.com/Tanzeel-Jutt/UML-Generator-Pro.git

# Build with Maven
cd UML-Generator-Pro
mvnw.cmd clean package
```

---

## 🎨 Themes
- **Dracula** — Dark purple theme (default)
- **Matrix Hacker** — Green-on-black terminal aesthetic

## 📦 Project Structure
```
UML-Generator-Pro/
├── src/                    # Java Desktop Application (JavaFX)
│   └── main/java/com/umlgenerator/
│       ├── core/           # Parsers, Generators, Models
│       └── ui/             # JavaFX UI Components
├── web/                    # Web Application (Static HTML/JS/CSS)
│   ├── index.html          # Main entry point
│   ├── app.js              # Application logic
│   ├── parsers.js          # Language parsers
│   ├── renderers.js        # Diagram renderers
│   └── style.css           # Styles & themes
└── pom.xml                 # Maven build config
```

## 📱 PWA Support
The web app can be installed as a Progressive Web App on any device!

---

## 🤝 Contributing
Contributions are welcome! Feel free to open issues and pull requests.

## 📄 License
MIT License — feel free to use and modify.

## 👤 Author
**Tanzeel Ur Rehman** — [@Tanzeel-Jutt](https://github.com/Tanzeel-Jutt)
